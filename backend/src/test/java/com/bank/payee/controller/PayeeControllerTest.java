package com.bank.payee.controller;

import com.bank.payee.dto.AddPayeeRequest;
import com.bank.payee.dto.InitiateMfaResponse;
import com.bank.payee.dto.VerifyOtpRequest;
import com.bank.payee.dto.VerifyOtpResponse;
import com.bank.payee.model.MfaMethod;
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
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// S125 — section-divider comments (e.g. "// DELETE /api/payees/{id}") are documentation
// headers, not commented-out Java code. Suppressed at class level to avoid per-method noise.
@SuppressWarnings("java:S125")
@ExtendWith(MockitoExtension.class)
class PayeeControllerTest {

    @Mock
    private MfaService mfaService;

    @Mock
    private PayeeService payeeService;

    @InjectMocks
    private PayeeController payeeController;

    /** Returns a mock Authentication that reports the given principal name. */
    private static Authentication mockAuth(String name) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(name);
        return auth;
    }

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
        assertNull(response.getBody().getDemoOtp(), "OTP must NOT be returned in API response");
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
        when(payeeService.addPayee(any(Payee.class), anyString())).thenReturn(Mono.just(payee));

        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "123456");

        // Act
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req, mockAuth("user1")).block();

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
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req, mockAuth("user1")).block();

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
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req, mockAuth("user1")).block();

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
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req, mockAuth("user1")).block();

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
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req, mockAuth("user1")).block();

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
    }

    // -------------------------------------------------------------------------
    // GET /api/payees
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 200 with list of all payees")
    void test_getPayees_returnsAllPayees() {
        // Arrange
        Payee payee1 = new Payee("Alice", "ACC001", "BNKA");
        Payee payee2 = new Payee("Bob", "ACC002", "BNKB");
        List<Payee> payees = Arrays.asList(payee1, payee2);
        when(payeeService.getPayees(anyString())).thenReturn(Mono.just(payees));

        // Act
        ResponseEntity<List<Payee>> response = payeeController.getPayees(mockAuth("user1")).block();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(payeeService, times(1)).getPayees(anyString());
    }

    @Test
    @DisplayName("Should return 200 with empty array when no payees exist")
    void test_getPayees_emptyStore_returns200WithEmptyList() {
        // Arrange
        when(payeeService.getPayees(anyString())).thenReturn(Mono.just(Collections.emptyList()));

        // Act
        ResponseEntity<List<Payee>> response = payeeController.getPayees(mockAuth("user1")).block();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/payees/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return 204 No Content when payee exists and is deleted")
    void test_deletePayee_existingId_returns204() {
        // Arrange
        when(payeeService.deletePayee(eq("payee-id-1"), anyString())).thenReturn(Mono.just(true));

        // Act
        ResponseEntity<Void> response = payeeController.deletePayee("payee-id-1", mockAuth("user1")).block();

        // Assert
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(payeeService, times(1)).deletePayee(eq("payee-id-1"), anyString());
    }

    @Test
    @DisplayName("Should return 404 Not Found when payee does not exist or caller is not the owner")
    void test_deletePayee_nonExistingId_returns404() {
        // Arrange
        when(payeeService.deletePayee(eq("no-such-id"), anyString())).thenReturn(Mono.just(false));

        // Act
        ResponseEntity<Void> response = payeeController.deletePayee("no-such-id", mockAuth("user1")).block();

        // Assert
        assertEquals(404, response.getStatusCode().value());
        verify(payeeService, times(1)).deletePayee(eq("no-such-id"), anyString());
    }

    // -------------------------------------------------------------------------
    // Rule: data-validation [test-validation-thoroughly]
    // Boundary-value and malicious-input tests for AddPayeeRequest / VerifyOtpRequest
    // -------------------------------------------------------------------------

    private static final Validator VALIDATOR;
    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    @DisplayName("[Validation] accountNumber with letters should fail @Pattern")
    void test_addPayeeRequest_accountNumberWithLetters_failsValidation() {
        AddPayeeRequest req = new AddPayeeRequest();
        req.setPayeeName("Alice");
        req.setAccountNumber("ACC001XY");   // letters — violates [0-9]+
        req.setBankCode("BNKA");
        req.setMfaMethod(MfaMethod.SMS);

        Set<ConstraintViolation<AddPayeeRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")),
                "Expected violation on accountNumber");
    }

    @Test
    @DisplayName("[Validation] accountNumber with 21 digits should fail @Size(max=20)")
    void test_addPayeeRequest_accountNumberTooLong_failsValidation() {
        AddPayeeRequest req = new AddPayeeRequest();
        req.setPayeeName("Alice");
        req.setAccountNumber("123456789012345678901");   // 21 digits
        req.setBankCode("BNKA");
        req.setMfaMethod(MfaMethod.SMS);

        Set<ConstraintViolation<AddPayeeRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")),
                "Expected violation on accountNumber length");
    }

    @Test
    @DisplayName("[Validation] accountNumber with 5 digits should fail @Size(min=6)")
    void test_addPayeeRequest_accountNumberTooShort_failsValidation() {
        AddPayeeRequest req = new AddPayeeRequest();
        req.setPayeeName("Alice");
        req.setAccountNumber("12345");   // 5 digits — below min=6
        req.setBankCode("BNKA");
        req.setMfaMethod(MfaMethod.SMS);

        Set<ConstraintViolation<AddPayeeRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("accountNumber")),
                "Expected violation on accountNumber min length");
    }

    @Test
    @DisplayName("[Validation] valid AddPayeeRequest with 6-digit numeric account passes all constraints")
    void test_addPayeeRequest_valid_noViolations() {
        AddPayeeRequest req = new AddPayeeRequest();
        req.setPayeeName("Alice");
        req.setAccountNumber("123456");   // exactly at min boundary
        req.setBankCode("BNKA");
        req.setMfaMethod(MfaMethod.SMS);

        Set<ConstraintViolation<AddPayeeRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.isEmpty(), "Expected no violations for a valid request: " + violations);
    }

    @Test
    @DisplayName("[Validation] 5-digit otpCode should fail @Size(min=6,max=6)")
    void test_verifyOtpRequest_fiveDigitOtp_failsValidation() {
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "12345");   // 5 digits

        Set<ConstraintViolation<VerifyOtpRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("otpCode")),
                "Expected violation on otpCode length");
    }

    @Test
    @DisplayName("[Validation] 7-digit otpCode should fail @Size(min=6,max=6)")
    void test_verifyOtpRequest_sevenDigitOtp_failsValidation() {
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "1234567");   // 7 digits

        Set<ConstraintViolation<VerifyOtpRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("otpCode")),
                "Expected violation on otpCode length");
    }

    @Test
    @DisplayName("[Validation] alpha otpCode should fail @Pattern([0-9]{6})")
    void test_verifyOtpRequest_alphaOtp_failsValidation() {
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "ABCDEF");   // letters

        Set<ConstraintViolation<VerifyOtpRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("otpCode")),
                "Expected violation on otpCode pattern");
    }

    @Test
    @DisplayName("[Validation] exactly 6-digit otpCode passes all constraints")
    void test_verifyOtpRequest_sixDigitOtp_noViolations() {
        VerifyOtpRequest req = new VerifyOtpRequest("sess-001", "123456");

        Set<ConstraintViolation<VerifyOtpRequest>> violations = VALIDATOR.validate(req);
        assertTrue(violations.isEmpty(), "Expected no violations for valid OTP: " + violations);
    }
}
