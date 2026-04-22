package com.bank.payee.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class MfaSession {

    private final String sessionId;
    private final Payee pendingPayee;
    private final MfaMethod mfaMethod;
    private final String otpCode;
    private final LocalDateTime expiresAt;

    // S3008 — failedAttempts uses AtomicInteger so concurrent verifyOtp() calls
    // cannot lose an increment under race conditions (banking security control).
    private final AtomicInteger failedAttempts = new AtomicInteger(0);

    // S3008 — volatile ensures cross-thread visibility of the lockout timestamp
    // without requiring a full synchronized block for a single reference write.
    private volatile LocalDateTime lockedUntil;

    public MfaSession(String sessionId, Payee pendingPayee, MfaMethod mfaMethod,
                      String otpCode, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.pendingPayee = pendingPayee;
        this.mfaMethod = mfaMethod;
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /** Thread-safe increment; returns the updated count. */
    public int incrementFailedAttempts() {
        return failedAttempts.incrementAndGet();
    }

    public String getSessionId() { return sessionId; }
    public Payee getPendingPayee() { return pendingPayee; }
    public MfaMethod getMfaMethod() { return mfaMethod; }
    public String getOtpCode() { return otpCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getFailedAttempts() { return failedAttempts.get(); }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}
