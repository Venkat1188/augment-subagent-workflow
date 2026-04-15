package com.bank.payee.service;

import com.bank.payee.model.MfaMethod;
import com.bank.payee.model.MfaSession;
import com.bank.payee.model.Payee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private OtpService otpService;

    @InjectMocks
    private MfaService mfaService;

    // -------------------------------------------------------------------------
    // initiateMfa
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create session and send SMS OTP when method is SMS")
    void test_initiateMfa_sms_createsSessionAndSendsSmsOtp() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("123456");
        Payee payee = new Payee("Alice", "ACC001", "BNKA");

        // Act
        MfaSession session = mfaService.initiateMfa(payee, MfaMethod.SMS);

        // Assert
        assertNotNull(session.getSessionId());
        assertEquals("123456", session.getOtpCode());
        assertEquals(MfaMethod.SMS, session.getMfaMethod());
        assertFalse(session.isExpired());
        verify(otpService, times(1)).sendSmsOtp("123456");
        verify(otpService, never()).sendTotpCode(any());
    }

    @Test
    @DisplayName("Should create session and send TOTP code when method is TOTP")
    void test_initiateMfa_totp_createsSessionAndSendsTotpCode() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("654321");
        Payee payee = new Payee("Bob", "ACC002", "BNKB");

        // Act
        MfaSession session = mfaService.initiateMfa(payee, MfaMethod.TOTP);

        // Assert
        assertNotNull(session.getSessionId());
        assertEquals("654321", session.getOtpCode());
        verify(otpService, times(1)).sendTotpCode("654321");
        verify(otpService, never()).sendSmsOtp(any());
    }

    // -------------------------------------------------------------------------
    // verifyOtp – happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return SUCCESS when OTP matches")
    void test_verifyOtp_correctOtp_returnsSuccess() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("111111");
        MfaSession session = mfaService.initiateMfa(new Payee("C", "A", "B"), MfaMethod.SMS);

        // Act
        VerifyResult result = mfaService.verifyOtp(session.getSessionId(), "111111");

        // Assert
        assertEquals(VerifyResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getPayee());
        assertEquals("OTP verified successfully", result.getMessage());
    }

    // -------------------------------------------------------------------------
    // verifyOtp – wrong OTP
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return WRONG_OTP with 2 remaining attempts on first wrong OTP")
    void test_verifyOtp_wrongOtp_decrementsRemainingAttempts() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("999999");
        MfaSession session = mfaService.initiateMfa(new Payee("D", "A", "B"), MfaMethod.SMS);

        // Act
        VerifyResult result = mfaService.verifyOtp(session.getSessionId(), "000000");

        // Assert
        assertEquals(VerifyResult.Status.WRONG_OTP, result.getStatus());
        assertEquals(2, result.getRemainingAttempts());
    }

    // -------------------------------------------------------------------------
    // verifyOtp – lockout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should lock session after MAX_ATTEMPTS (3) failed attempts")
    void test_verifyOtp_maxFailedAttempts_locksSession() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("777777");
        MfaSession session = mfaService.initiateMfa(new Payee("E", "A", "B"), MfaMethod.SMS);
        String sid = session.getSessionId();

        // Act
        mfaService.verifyOtp(sid, "000001");
        mfaService.verifyOtp(sid, "000002");
        VerifyResult result = mfaService.verifyOtp(sid, "000003");

        // Assert
        assertEquals(VerifyResult.Status.LOCKED, result.getStatus());
        assertNotNull(result.getLockedUntil());
    }

    @Test
    @DisplayName("Should return LOCKED immediately when session is already locked")
    void test_verifyOtp_alreadyLockedSession_returnsLocked() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("555555");
        MfaSession session = mfaService.initiateMfa(new Payee("F", "A", "B"), MfaMethod.SMS);
        String sid = session.getSessionId();
        mfaService.verifyOtp(sid, "000001");
        mfaService.verifyOtp(sid, "000002");
        mfaService.verifyOtp(sid, "000003"); // triggers lock

        // Act
        VerifyResult result = mfaService.verifyOtp(sid, "555555"); // even correct OTP is rejected

        // Assert
        assertEquals(VerifyResult.Status.LOCKED, result.getStatus());
    }

    // -------------------------------------------------------------------------
    // verifyOtp – not found / expired
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return NOT_FOUND for unknown session ID")
    void test_verifyOtp_unknownSession_returnsNotFound() {
        // Act
        VerifyResult result = mfaService.verifyOtp("no-such-session-id", "000000");

        // Assert
        assertEquals(VerifyResult.Status.NOT_FOUND, result.getStatus());
    }
}
