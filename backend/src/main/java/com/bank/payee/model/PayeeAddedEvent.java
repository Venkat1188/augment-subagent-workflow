package com.bank.payee.model;

/**
 * Dapr pub/sub event published to the {@code payee-events} topic
 * whenever a payee is successfully added via MFA verification.
 *
 * <p>Wrapped in a {@code CloudEvent<PayeeAddedEvent>} envelope by the Dapr SDK.
 */
public record PayeeAddedEvent(
        String payeeId,
        String name,
        String accountNumber,
        String bankCode,
        String addedAt
) {}
