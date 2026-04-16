package com.bank.payee.dto;

import com.bank.payee.model.Payee;
import java.time.LocalDateTime;

public class VerifyOtpResponse {

    private boolean success;
    private String message;
    private Payee payee;
    private Integer remainingAttempts;
    private LocalDateTime lockedUntil;

    // S1186 — empty no-arg constructor required by Jackson for JSON deserialization.
    // Fields are populated via static factory methods (success, failure, locked, error).
    public VerifyOtpResponse() {
        // Jackson deserialization constructor — intentionally empty
    }

    public static VerifyOtpResponse success(String message, Payee payee) {
        VerifyOtpResponse r = new VerifyOtpResponse();
        r.success = true;
        r.message = message;
        r.payee = payee;
        return r;
    }

    public static VerifyOtpResponse failure(String message, int remainingAttempts) {
        VerifyOtpResponse r = new VerifyOtpResponse();
        r.success = false;
        r.message = message;
        r.remainingAttempts = remainingAttempts;
        return r;
    }

    public static VerifyOtpResponse locked(String message, LocalDateTime lockedUntil) {
        VerifyOtpResponse r = new VerifyOtpResponse();
        r.success = false;
        r.message = message;
        r.lockedUntil = lockedUntil;
        return r;
    }

    public static VerifyOtpResponse error(String message) {
        VerifyOtpResponse r = new VerifyOtpResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Payee getPayee() { return payee; }
    public Integer getRemainingAttempts() { return remainingAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
}
