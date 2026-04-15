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
        log.info("📱 [MOCK SMS] OTP {} dispatched to customer's registered mobile number", otp);
    }

    public void sendTotpCode(String otp) {
        log.info("🔐 [MOCK TOTP] Current Authenticator App code: {}", otp);
    }
}
