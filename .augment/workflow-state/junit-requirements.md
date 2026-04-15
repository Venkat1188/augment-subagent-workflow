# JUnit 5 Test Specifications – SCRUM-1

## `MfaServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock private OtpService otpService;
    @InjectMocks private MfaService mfaService;

    @Test @DisplayName("Should create session and send SMS OTP when method is SMS")
    void test_initiateMfa_sms_createsSessionAndSendsSmsOtp() {
        // Arrange
        when(otpService.generateOtp()).thenReturn("123456");
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        // Act
        MfaSession session = mfaService.initiateMfa(payee, MfaMethod.SMS);
        // Assert
        assertNotNull(session.getSessionId());
        assertEquals("123456", session.getOtpCode());
        verify(otpService).sendSmsOtp("123456");
        verify(otpService, never()).sendTotpCode(any());
    }

    @Test @DisplayName("Should create session and send TOTP code when method is TOTP")
    void test_initiateMfa_totp_createsSessionAndSendsTotpCode() {
        when(otpService.generateOtp()).thenReturn("654321");
        Payee payee = new Payee("Bob", "ACC002", "BNKB");
        MfaSession session = mfaService.initiateMfa(payee, MfaMethod.TOTP);
        assertNotNull(session.getSessionId());
        verify(otpService).sendTotpCode("654321");
        verify(otpService, never()).sendSmsOtp(any());
    }

    @Test @DisplayName("Should return SUCCESS when OTP matches")
    void test_verifyOtp_correctOtp_returnsSuccess() {
        when(otpService.generateOtp()).thenReturn("111111");
        MfaSession session = mfaService.initiateMfa(new Payee("C","A","B"), MfaMethod.SMS);
        VerifyResult result = mfaService.verifyOtp(session.getSessionId(), "111111");
        assertEquals(VerifyResult.Status.SUCCESS, result.getStatus());
        assertNotNull(result.getPayee());
    }

    @Test @DisplayName("Should return WRONG_OTP with decremented attempts on bad OTP")
    void test_verifyOtp_wrongOtp_decrementsRemainingAttempts() {
        when(otpService.generateOtp()).thenReturn("999999");
        MfaSession session = mfaService.initiateMfa(new Payee("D","A","B"), MfaMethod.SMS);
        VerifyResult result = mfaService.verifyOtp(session.getSessionId(), "000000");
        assertEquals(VerifyResult.Status.WRONG_OTP, result.getStatus());
        assertEquals(2, result.getRemainingAttempts());
    }

    @Test @DisplayName("Should lock session after MAX_ATTEMPTS failed attempts")
    void test_verifyOtp_maxFailedAttempts_locksSession() {
        when(otpService.generateOtp()).thenReturn("777777");
        MfaSession session = mfaService.initiateMfa(new Payee("E","A","B"), MfaMethod.SMS);
        String sid = session.getSessionId();
        mfaService.verifyOtp(sid, "000001");
        mfaService.verifyOtp(sid, "000002");
        VerifyResult result = mfaService.verifyOtp(sid, "000003");
        assertEquals(VerifyResult.Status.LOCKED, result.getStatus());
        assertNotNull(result.getLockedUntil());
    }

    @Test @DisplayName("Should return NOT_FOUND for unknown session")
    void test_verifyOtp_unknownSession_returnsNotFound() {
        VerifyResult result = mfaService.verifyOtp("no-such-id", "000000");
        assertEquals(VerifyResult.Status.NOT_FOUND, result.getStatus());
    }
}
```

## `PayeeControllerTest`

```java
@ExtendWith(MockitoExtension.class)
class PayeeControllerTest {

    @Mock private MfaService mfaService;
    @Mock private PayeeService payeeService;
    @InjectMocks private PayeeController payeeController;

    @Test @DisplayName("Should return 200 with sessionId when initiate-mfa request is valid")
    void test_initiateMfa_validRequest_returns200() {
        // Arrange
        AddPayeeRequest req = new AddPayeeRequest("Alice","ACC001","BNKA", MfaMethod.SMS);
        MfaSession mockSession = new MfaSession("sess-1", new Payee("Alice","ACC001","BNKA"),
            MfaMethod.SMS, "123456", LocalDateTime.now().plusMinutes(10));
        when(mfaService.initiateMfa(any(Payee.class), eq(MfaMethod.SMS))).thenReturn(mockSession);
        // Act
        ResponseEntity<InitiateMfaResponse> response = payeeController.initiateMfa(req);
        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("sess-1", response.getBody().getSessionId());
    }

    @Test @DisplayName("Should return 200 with payee when OTP is correct")
    void test_verifyOtp_correctOtp_returns200WithPayee() {
        Payee payee = new Payee("Alice","ACC001","BNKA");
        when(mfaService.verifyOtp("sess-1","123456")).thenReturn(VerifyResult.success(payee));
        when(payeeService.addPayee(any())).thenReturn(payee);
        VerifyOtpRequest req = new VerifyOtpRequest("sess-1","123456");
        ResponseEntity<VerifyOtpResponse> response = payeeController.verifyOtp(req);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }

    @Test @DisplayName("Should return 400 when OTP is wrong")
    void test_verifyOtp_wrongOtp_returns400() {
        when(mfaService.verifyOtp("sess-1","000000")).thenReturn(VerifyResult.wrongOtp(2));
        ResponseEntity<VerifyOtpResponse> response =
            payeeController.verifyOtp(new VerifyOtpRequest("sess-1","000000"));
        assertEquals(400, response.getStatusCode().value());
        assertFalse(response.getBody().isSuccess());
        assertEquals(2, response.getBody().getRemainingAttempts());
    }

    @Test @DisplayName("Should return 423 when session is locked")
    void test_verifyOtp_lockedSession_returns423() {
        when(mfaService.verifyOtp("sess-1","000000"))
            .thenReturn(VerifyResult.locked(LocalDateTime.now().plusMinutes(5)));
        ResponseEntity<VerifyOtpResponse> response =
            payeeController.verifyOtp(new VerifyOtpRequest("sess-1","000000"));
        assertEquals(423, response.getStatusCode().value());
    }

    @Test @DisplayName("Should return 400 when session is not found")
    void test_verifyOtp_notFound_returns400() {
        when(mfaService.verifyOtp("bad-id","000000")).thenReturn(VerifyResult.notFound());
        ResponseEntity<VerifyOtpResponse> response =
            payeeController.verifyOtp(new VerifyOtpRequest("bad-id","000000"));
        assertEquals(400, response.getStatusCode().value());
    }
}
```
