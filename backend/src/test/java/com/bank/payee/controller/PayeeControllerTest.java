package com.bank.payee.controller;

import com.bank.payee.dto.AddPayeeRequest;
import com.bank.payee.dto.InitiateMfaResponse;
import com.bank.payee.dto.VerifyOtpRequest;
import com.bank.payee.dto.VerifyOtpResponse;
import com.bank.payee.model.MfaMethod;
import com.bank.payee.model.MfaSession;
import com.bank.payee.model.Payee;
import com.bank.payee.service.MfaService;
import com.bank.payee.service.PayeeService;
import com.bank.payee.service.VerifyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayeeControllerTest {

    @Mock
    private MfaService mfaService;

    @Mock
    private PayeeService payeeService;

    @InjectMocks
    private PayeeController payeeController;

    // -------------------------------------------------------------------------
    // POST /api/payees/initiate-mfa
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 200 with sessionId when initiate-mfa request is valid")
    void test_initiateMfa_validRequest_returns200WithSessionId() {
        // Arrange
        AddPayeeRequest req = new AddPayeeRequest("Alice", "ACC001", "BNKA", MfaMethod.SMS);
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        MfaSession mockSession = new MfaSession(
                "sess-001", payee, MfaMethod.SMS, "123456",
                LocalDateTime.now().plusMinutes(10));
        when(mfaService.initiateMfa(any(Payee.class), eq(MfaMethod.SMS))).thenReturn(mockSession);

        // Act
        ResponseEntity<InitiateMfaResponse> response = payeeController.initiateMfa(req);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("sess-001", response.getBody().getSessionId());
        assertEquals("123456", response.getBody().getDemoOtp());
        assertEquals(MfaMethod.SMS, response.getBody().getMfaMethod());
    }

    // -------------------------------------------------------------------------
    // POST /api/payees/verify-otp – SUCCESS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 200 with saved payee when OTP is correct")
    void test_verifyOtp_correctOtp_returns200WithPayee() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        when(mfaService.verifyOtp("sess-001", "123456")).thenReturn(VerifyResult.success(payee));
        when(payeeService.addPayee(any(Payee.class))).thenReturn(payee);

        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "123456");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(payee, response.getBody().getPayee());
    }

    // -------------------------------------------------------------------------
    // POST /api/payees/verify-otp – WRONG_OTP
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 400 with remaining attempts when OTP is wrong")
    void test_verifyOtp_wrongOtp_returns400WithRemainingAttempts() {
        // Arrange
        when(mfaService.verifyOtp("sess-001", "000000")).thenReturn(VerifyResult.wrongOtp(2));
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "000000");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(2, response.getBody().getRemainingAttempts());
    }

    // -------------------------------------------------------------------------
    // POST /api/payees/verify-otp – LOCKED
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 423 Locked when session is locked")
    void test_verifyOtp_lockedSession_returns423() {
        // Arrange
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(5);
        when(mfaService.verifyOtp("sess-001", "000000"))
                .thenReturn(VerifyResult.locked(lockUntil));
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "000000");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);

        // Assert
        assertEquals(423, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(lockUntil, response.getBody().getLockedUntil());
    }

    // -------------------------------------------------------------------------
    // POST /api/payees/verify-otp – NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 400 when session is not found")
    void test_verifyOtp_notFoundSession_returns400() {
        // Arrange
        when(mfaService.verifyOtp("bad-session", "000000")).thenReturn(VerifyResult.notFound());
        VerifyOtpRequest req = new VerifyOtpRequest("bad-session", "000000");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
    }

    // -------------------------------------------------------------------------
    // POST /api/payees/verify-otp – EXPIRED
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 400 when session is expired")
    void test_verifyOtp_expiredSession_returns400() {
        // Arrange
        when(mfaService.verifyOtp("old-session", "111111")).thenReturn(VerifyResult.expired());
        VerifyOtpRequest req = new VerifyOtpRequest("old-session", "111111");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
    }
}
