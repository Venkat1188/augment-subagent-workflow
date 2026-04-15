package com.bank.payee.service;

import com.bank.payee.model.Payee;
import com.bank.payee.model.PayeeAddedEvent;
import io.dapr.client.DaprClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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

    /**
     * Persists a confirmed payee in the Dapr state store and publishes a
     * {@link PayeeAddedEvent} to the {@value #TOPIC} topic.
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

        return daprClient.saveState(STORE, payee.getId(), payee)
                .then(daprClient.publishEvent(PUB_SUB, TOPIC, event))
                .thenReturn(payee);
    }

    /**
     * Returns all confirmed payees from the Dapr state store index.
     *
     * @return {@code Mono<List<Payee>>} — empty list if no payees stored yet
     */
    @SuppressWarnings("unchecked")
    public Mono<List<Payee>> getPayees() {
        return daprClient.getState(STORE, "payee-index", List.class)
                .map(state -> state.getValue() != null
                        ? (List<Payee>) state.getValue()
                        : List.of());
    }

    /**
     * Deletes a payee by its UUID from the Dapr state store.
     *
     * @param id the UUID of the payee to delete
     * @return {@code Mono<Boolean>} — {@code true} if deleted, {@code false} if not found
     */
    public Mono<Boolean> deletePayee(String id) {
        return daprClient.getState(STORE, id, Payee.class)
                .flatMap(state -> state.getValue() == null
                        ? Mono.just(false)
                        : daprClient.deleteState(STORE, id).thenReturn(true));
    }
}
