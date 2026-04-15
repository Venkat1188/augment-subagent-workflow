package com.bank.payee.service;

import com.bank.payee.model.Payee;
import com.bank.payee.model.PayeeAddedEvent;
import io.dapr.client.DaprClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
     * {@code payee-index} list so that {@link #getPayees()} can return it,
     * and publishes a {@link PayeeAddedEvent} to the {@value #TOPIC} topic.
     *
     * @param payee the payee to store (id and addedAt will be populated)
     * @return {@code Mono<Payee>} — the stored payee
     */
    public Mono<Payee> addPayee(Payee payee) {
        payee.setId(UUID.randomUUID().toString());
        payee.setAddedAt(LocalDateTime.now());

        PayeeAddedEvent event = new PayeeAddedEvent(
                payee.getId(),
                payee.getName(),
                payee.getAccountNumber(),
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
     * Returns all confirmed payees from the {@code payee-index} key in the Dapr state store.
     *
     * @return {@code Mono<List<Payee>>} — empty list if no payees stored yet
     */
    @SuppressWarnings("unchecked")
    public Mono<List<Payee>> getPayees() {
        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .map(state -> state.getValue() != null
                        ? (List<Payee>) state.getValue()
                        : List.of());
    }

    /**
     * Deletes a payee by its UUID from the Dapr state store and removes it
     * from the {@code payee-index}.
     *
     * @param id the UUID of the payee to delete
     * @return {@code Mono<Boolean>} — {@code true} if deleted, {@code false} if not found
     */
    public Mono<Boolean> deletePayee(String id) {
        return daprClient.getState(STORE, id, Payee.class)
                .flatMap(state -> {
                    if (state.getValue() == null) {
                        return Mono.just(false);
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
     * Reads the current payee-index, appends {@code payee}, and saves it back.
     * This is a best-effort read-modify-write; for strict consistency use Dapr
     * ETag-based concurrency or a transactional state API.
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> appendToIndex(Payee payee) {
        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .flatMap(state -> {
                    List<Payee> list = state.getValue() != null
                            ? new ArrayList<>((List<Payee>) state.getValue())
                            : new ArrayList<>();
                    list.add(payee);
                    return daprClient.saveState(STORE, INDEX_KEY, list);
                });
    }

    /**
     * Reads the current payee-index, removes the entry with the given {@code id},
     * and saves the updated list back.
     */
    @SuppressWarnings("unchecked")
    private Mono<Void> removeFromIndex(String id) {
        return daprClient.getState(STORE, INDEX_KEY, List.class)
                .flatMap(state -> {
                    if (state.getValue() == null) return Mono.empty();
                    List<Payee> list = new ArrayList<>((List<Payee>) state.getValue());
                    list.removeIf(p -> id.equals(p.getId()));
                    return daprClient.saveState(STORE, INDEX_KEY, list);
                });
    }
}
