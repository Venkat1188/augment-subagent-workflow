package com.bank.payee.model;

/**
 * Dapr pub/sub event published to the {@code payee-events} topic
 * whenever a payee is successfully added via MFA verification.
 *
 * <p>Wrapped in a {@code CloudEvent<PayeeAddedEvent>} envelope by the Dapr SDK.
 *
 * <p><b>Privacy (S6096):</b> Account numbers are PII. This record stores only
 * a <em>masked</em> representation (last 4 digits, e.g. {@code ****1234}) so that
 * downstream subscribers cannot read full account numbers from the message broker.
 */
public record PayeeAddedEvent(
        String payeeId,
        String name,
        String maskedAccountNumber,   // e.g. "****1234" — never the full account number
        String bankCode,
        String addedAt
) {

    /**
     * Masks an account number, keeping only the last 4 digits visible.
     * Returns {@code "****"} if the account number is null or shorter than 4 characters.
     *
     * @param accountNumber the raw account number (never stored or published unmasked)
     * @return masked representation, e.g. {@code "****5678"}
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
