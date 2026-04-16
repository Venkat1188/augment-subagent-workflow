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

    public void sendSmsOtp(String otp) {
        // S2068/S5145 — OTP is a sensitive credential; never log its value.
        // The OTP is delivered out-of-band to the customer's registered mobile number.
        log.info("📱 [MOCK SMS] OTP dispatched to customer's registered mobile number");
    }

    public void sendTotpCode(String otp) {
        // S2068/S5145 — OTP is a sensitive credential; never log its value.
        log.info("🔐 [MOCK TOTP] Authenticator app code generated and sent to registered device");
    }
}
