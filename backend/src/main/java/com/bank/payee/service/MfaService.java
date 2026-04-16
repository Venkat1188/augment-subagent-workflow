package com.bank.payee.service;

import com.bank.payee.model.MfaMethod;
import com.bank.payee.model.MfaSession;
import com.bank.payee.model.Payee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);

    static final int MAX_ATTEMPTS = 3;
    static final int LOCKOUT_MINUTES = 5;
    private static final int SESSION_EXPIRY_MINUTES = 10;

    private final Map<String, MfaSession> sessions = new ConcurrentHashMap<>();
    private final OtpService otpService;

    public MfaService(OtpService otpService) {
        this.otpService = otpService;
    }

    public MfaSession initiateMfa(Payee pendingPayee, MfaMethod method) {
        String sessionId = UUID.randomUUID().toString();
        String otp = otpService.generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES);

        MfaSession session = new MfaSession(sessionId, pendingPayee, method, otp, expiresAt);
        sessions.put(sessionId, session);

        if (method == MfaMethod.SMS) {
            otpService.sendSmsOtp(otp);
        } else {
            otpService.sendTotpCode(otp);
        }

        return session;
    }

    public VerifyResult verifyOtp(String sessionId, String inputOtp) {
        MfaSession session = sessions.get(sessionId);

        if (session == null) {
            return VerifyResult.notFound();
        }

        if (session.isExpired()) {
            sessions.remove(sessionId);
            return VerifyResult.expired();
        }

        if (session.isLocked()) {
            return VerifyResult.locked(session.getLockedUntil());
        }

        if (session.getOtpCode().equals(inputOtp)) {
            sessions.remove(sessionId);
            return VerifyResult.success(session.getPendingPayee());
        }

        // S3008 — incrementAndGet() is atomic; the returned value is the definitive count.
        int attempts = session.incrementFailedAttempts();
        if (attempts >= MAX_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            session.setLockedUntil(lockUntil);
            return VerifyResult.locked(lockUntil);
        }

        return VerifyResult.wrongOtp(MAX_ATTEMPTS - attempts);
    }

    /**
     * CWE-613 / CWE-400 — Evict expired MFA sessions every 60 s to prevent unbounded heap growth.
     * Sessions expire after SESSION_EXPIRY_MINUTES; this sweeper ensures they are removed from the
     * ConcurrentHashMap even if the client never calls verifyOtp (abandoned flows).
     */
    @Scheduled(fixedDelay = 60_000)
    void evictExpiredSessions() {
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
        int evicted = before - sessions.size();
        if (evicted > 0) {
            log.info("MFA session eviction: removed {} expired session(s); {} active", evicted, sessions.size());
        }
    }
}
