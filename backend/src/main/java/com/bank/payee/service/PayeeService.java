package com.bank.payee.service;

import com.bank.payee.model.Payee;
import com.bank.payee.model.PayeeAddedEvent;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.StateOptions;
import io.dapr.exceptions.DaprException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dapr-native payee service.
 *
 * <p>Replaces in-memory {@code ConcurrentHashMap} with Dapr state store operations.
 * All methods return {@link Mono} for non-blocking composition.
 *
 * <p>Skill: java-spring-boot-dapr — state store + pub/sub, resiliency via resiliency.yaml.
 */
@Service
public class PayeeService {

    /** Dapr state store component name (matches components/statestore.yaml). */
    public static final String STORE   = "payee-statestore";

    /** Dapr pub/sub component name (matches components/pubsub.yaml). */
    public static final String PUB_SUB = "payee-pubsub";

    /** Topic on which {@link PayeeAddedEvent} events are published. */
    public static final String TOPIC   = "payee-events";

    private final DaprClient daprClient;

    public PayeeService(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    /** State store key for the payee list index. */
    static final String INDEX_KEY = "payee-index";

    /**
     * Persists a confirmed payee in the Dapr state store, updates the
     * {@code payee-index} list so that {@link #getPayees(String)} can return it,
     * and publishes a {@link PayeeAddedEvent} to the {@value #TOPIC} topic.
     *
     * @param payee   the payee to store (id and addedAt will be populated)
     * @param ownerId the authenticated principal name — stored for BOLA prevention (CWE-639)
     * @return {@code Mono<Payee>} — the stored payee
     */
    public Mono<Payee> addPayee(Payee payee, String ownerId) {
        payee.setId(UUID.randomUUID().toString());
        payee.setAddedAt(LocalDateTime.now());
        payee.setOwnerId(ownerId);   // CWE-639 — bind this record to its owner at creation time
        // Rule: data-validation [normalize-input-data] — trim whitespace before persistence
        // so that "Alice " and "Alice" are stored identically and comparisons are predictable.
        if (payee.getName() != null) {
            payee.setName(payee.getName().strip());
        }

        // S6096 — never publish raw account numbers to the message broker.
        // Use the built-in mask helper so only the last 4 digits are visible to subscribers.
        PayeeAddedEvent event = new PayeeAddedEvent(
                payee.getId(),
                payee.getName(),
                PayeeAddedEvent.maskAccountNumber(payee.getAccountNumber()),
                payee.getBankCode(),
                payee.getAddedAt().toString()
        );

        // Mono.defer() ensures appendToIndex and publishEvent are only called
        // if saveState succeeds — prevents eager evaluation of inner Monos.
        return daprClient.saveState(STORE, payee.getId(), payee)
                .then(Mono.defer(() -> appendToIndex(payee)))
                .then(Mono.defer(() -> daprClient.publishEvent(PUB_SUB, TOPIC, event)))
                .thenReturn(payee);
    }

    /**
     * Returns confirmed payees that belong to {@code ownerId} from the Dapr state store.
     * CWE-639 — filters the shared index to only expose the caller's own records.
     *
     * @param ownerId the authenticated principal name
     * @return {@code Mono<List<Payee>>} — empty list if no payees stored yet
     */
    @SuppressWarnings("unchecked")
    public Mono<List<Payee>> getPayees(String ownerId) {
        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .map(state -> state.getValue() != null
                        ? ((List<Payee>) state.getValue()).stream()
                                .filter(p -> ownerId.equals(p.getOwnerId()))
                                .toList()
                        : List.<Payee>of());
    }

    /**
     * Deletes a payee by its UUID from the Dapr state store and removes it
     * from the {@code payee-index}, but only if the caller is the owner.
     * CWE-639 — returns {@code false} (not 403) when the payee exists but the
     * caller does not own it, to avoid leaking whether the ID is valid.
     *
     * @param id      the UUID of the payee to delete
     * @param ownerId the authenticated principal name
     * @return {@code Mono<Boolean>} — {@code true} if deleted, {@code false} if not found or not owned
     */
    public Mono<Boolean> deletePayee(String id, String ownerId) {
        return daprClient.getState(STORE, id, Payee.class)
                .flatMap(state -> {
                    Payee payee = state.getValue();
                    if (payee == null || !ownerId.equals(payee.getOwnerId())) {
                        return Mono.just(false);   // not found OR not the owner — same response (CWE-639)
                    }
                    return daprClient.deleteState(STORE, id)
                            .then(Mono.defer(() -> removeFromIndex(id)))
                            .thenReturn(true);
                });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads the current payee-index, appends {@code payee}, and saves it back
     * using <b>ETag-based optimistic concurrency</b> ({@code FIRST_WRITE} wins).
     *
     * <p>If a concurrent writer updates the index between our read and write,
     * Dapr returns an {@code ABORTED} status. We catch that via
     * {@link DaprException} and retry up to 3 times, re-reading the fresh
     * state on each attempt — preventing lost updates in a banking context.
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> appendToIndex(Payee payee) {
        StateOptions opts = new StateOptions(
                StateOptions.Consistency.STRONG,
                StateOptions.Concurrency.FIRST_WRITE);

        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .flatMap(state -> {
                    List<Payee> list = state.getValue() != null
                            ? new ArrayList<>((List<Payee>) state.getValue())
                            : new ArrayList<>();
                    list.add(payee);
                    // Pass the ETag so Dapr rejects the write if another writer
                    // has already updated this key since we read it.
                    return daprClient.saveState(STORE, INDEX_KEY, state.getEtag(), list, opts);
                })
                .retryWhen(Retry.max(3).filter(e -> e instanceof DaprException));
    }

    /**
     * Reads the current payee-index, removes the entry with the given {@code id},
     * and saves the updated list back using <b>ETag-based optimistic concurrency</b>.
     *
     * <p>Retries up to 3 times on ETag conflict ({@link DaprException}) to ensure
     * no concurrent add/remove operation is silently overwritten.
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> removeFromIndex(String id) {
        StateOptions opts = new StateOptions(
                StateOptions.Consistency.STRONG,
                StateOptions.Concurrency.FIRST_WRITE);

        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .flatMap(state -> {
                    if (state.getValue() == null) return Mono.empty();
                    List<Payee> list = new ArrayList<>((List<Payee>) state.getValue());
                    list.removeIf(p -> id.equals(p.getId()));
                    return daprClient.saveState(STORE, INDEX_KEY, state.getEtag(), list, opts);
                })
                .retryWhen(Retry.max(3).filter(e -> e instanceof DaprException));
    }
}
