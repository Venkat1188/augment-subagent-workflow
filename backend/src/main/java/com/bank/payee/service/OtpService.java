package com.bank.payee.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    // S1172 — 'otp' is intentionally not logged (S2068/S5145: OTP is a sensitive credential).
    // The parameter is kept in the signature so callers (MfaService) and tests can verify
    // the correct value is being dispatched; a real implementation would pass it to an SMS gateway.
    @SuppressWarnings("java:S1172")
    public void sendSmsOtp(String otp) {
        log.info("📱 [MOCK SMS] OTP dispatched to customer's registered mobile number");
    }

    // S1172 — same rationale as sendSmsOtp above.
    @SuppressWarnings("java:S1172")
    public void sendTotpCode(String otp) {
        log.info("🔐 [MOCK TOTP] Authenticator app code generated and sent to registered device");
    }
}
