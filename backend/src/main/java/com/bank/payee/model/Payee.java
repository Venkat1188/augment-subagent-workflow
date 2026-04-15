package com.bank.payee.model;

import java.time.LocalDateTime;

public class Payee {

    private String id;
    private String name;
    private String accountNumber;
    private String bankCode;
    private LocalDateTime addedAt;

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
}
