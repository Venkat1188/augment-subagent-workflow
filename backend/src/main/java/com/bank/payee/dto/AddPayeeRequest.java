package com.bank.payee.dto;

import com.bank.payee.model.MfaMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddPayeeRequest {

    @NotBlank(message = "Payee name is required")
    private String payeeName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotNull(message = "MFA method is required (SMS or TOTP)")
    private MfaMethod mfaMethod;

    public AddPayeeRequest() {}

    public AddPayeeRequest(String payeeName, String accountNumber, String bankCode, MfaMethod mfaMethod) {
        this.payeeName = payeeName;
        this.accountNumber = accountNumber;
        this.bankCode = bankCode;
        this.mfaMethod = mfaMethod;
    }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public MfaMethod getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(MfaMethod mfaMethod) { this.mfaMethod = mfaMethod; }
}
