package com.bank.payee.model;

import java.time.LocalDateTime;

public class MfaSession {

    private final String sessionId;
    private final Payee pendingPayee;
    private final MfaMethod mfaMethod;
    private final String otpCode;
    private final LocalDateTime expiresAt;
    private int failedAttempts;
    private LocalDateTime lockedUntil;

    public MfaSession(String sessionId, Payee pendingPayee, MfaMethod mfaMethod,
                      String otpCode, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.pendingPayee = pendingPayee;
        this.mfaMethod = mfaMethod;
        this.otpCode = otpCode;
        this.expiresAt = expiresAt;
        this.failedAttempts = 0;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public String getSessionId() { return sessionId; }
    public Payee getPendingPayee() { return pendingPayee; }
    public MfaMethod getMfaMethod() { return mfaMethod; }
    public String getOtpCode() { return otpCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}
