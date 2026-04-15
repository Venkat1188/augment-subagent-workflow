package com.bank.payee.service;

import com.bank.payee.model.Payee;
import java.time.LocalDateTime;

/**
 * Internal result object returned by {@link MfaService#verifyOtp}.
 */
public class VerifyResult {

    public enum Status { SUCCESS, WRONG_OTP, LOCKED, NOT_FOUND, EXPIRED }

    private final Status status;
    private final Payee payee;
    private final int remainingAttempts;
    private final LocalDateTime lockedUntil;
    private final String message;

    private VerifyResult(Status status, Payee payee, int remainingAttempts,
                         LocalDateTime lockedUntil, String message) {
        this.status = status;
        this.payee = payee;
        this.remainingAttempts = remainingAttempts;
        this.lockedUntil = lockedUntil;
        this.message = message;
    }

    public static VerifyResult success(Payee payee) {
        return new VerifyResult(Status.SUCCESS, payee, 0, null, "OTP verified successfully");
    }

    public static VerifyResult wrongOtp(int remaining) {
        return new VerifyResult(Status.WRONG_OTP, null, remaining, null,
                "Invalid OTP. " + remaining + " attempt(s) remaining.");
    }

    public static VerifyResult locked(LocalDateTime lockedUntil) {
        return new VerifyResult(Status.LOCKED, null, 0, lockedUntil,
                "Too many failed attempts. Action blocked for 5 minutes.");
    }

    public static VerifyResult notFound() {
        return new VerifyResult(Status.NOT_FOUND, null, 0, null,
                "Session not found. Please restart the process.");
    }

    public static VerifyResult expired() {
        return new VerifyResult(Status.EXPIRED, null, 0, null,
                "Session expired. Please restart the process.");
    }

    public Status getStatus() { return status; }
    public Payee getPayee() { return payee; }
    public int getRemainingAttempts() { return remainingAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public String getMessage() { return message; }
}
