package com.bank.payee.dto;

import com.bank.payee.model.MfaMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddPayeeRequest {

    @NotBlank(message = "Payee name is required")
    @Size(max = 100, message = "Payee name must not exceed 100 characters")
    private String payeeName;

    // CWE-20: length cap + numeric-only pattern prevent oversized/non-numeric account numbers
    @NotBlank(message = "Account number is required")
    @Size(min = 6, max = 20, message = "Account number must be 6–20 digits")
    @Pattern(regexp = "[0-9]+", message = "Account number must contain digits only")
    private String accountNumber;

    @NotBlank(message = "Bank code is required")
    @Size(min = 2, max = 11, message = "Bank code must be 2–11 characters")
    @Pattern(regexp = "[A-Z0-9]+", message = "Bank code must contain uppercase letters and digits only")
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
