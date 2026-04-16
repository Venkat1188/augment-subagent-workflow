package com.bank.payee.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Rule: data-privacy [identify-pii-correctly] — PII field inventory:
 * <ul>
 *   <li>{@code name}          — Direct identifier (full name). PII Class: HIGH.</li>
 *   <li>{@code accountNumber} — Financial identifier. PII Class: HIGH. Never log raw value.</li>
 *   <li>{@code ownerId}       — Maps to the authenticated username. PII Class: MEDIUM.</li>
 *   <li>{@code id}, {@code bankCode}, {@code addedAt} — Non-PII operational fields.</li>
 * </ul>
 * All PII fields are stored in the Dapr state store. At-rest encryption is enforced
 * at the infrastructure layer (Redis AUTH + TLS — see components/statestore.yaml).
 * Field-level encryption should be added if the storage backend changes to a shared cluster.
 */
public class Payee {

    private String id;
    /** PII: full name — treat as HIGH sensitivity; never include in logs. */
    private String name;
    /** PII: bank account number — HIGH sensitivity; always mask to last 4 digits before logging or publishing. */
    private String accountNumber;
    private String bankCode;
    private LocalDateTime addedAt;
    /** PII: mapped to authenticated principal — MEDIUM sensitivity; used for CWE-639 ownership checks. */
    private String ownerId;

    public Payee() {}

    public Payee(String name, String accountNumber, String bankCode) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.bankCode = bankCode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    /**
     * S2160 — equals based on {@code id} (unique per persisted payee).
     * Before {@code id} is set, two Payee instances are only equal if they are the same object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payee other)) return false;
        if (id == null || other.id == null) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
