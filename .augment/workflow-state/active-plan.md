# SCRUM-1 – MFA-Protected Payee Management API: Technical Implementation Plan

## Summary
Add a REST controller (`PayeeController`) and a `PayeeService` to expose the already-implemented
MFA service layer via HTTP endpoints. The controller follows a two-step flow:
1. **Initiate MFA** – create a pending payee and generate an OTP session.
2. **Verify OTP** – validate the OTP and, on success, persist the payee.

---

## Current State
| Layer     | Status |
|-----------|--------|
| Models    | ✅ Complete (`Payee`, `MfaSession`, `MfaMethod`) |
| DTOs      | ✅ Complete (`AddPayeeRequest`, `InitiateMfaResponse`, `VerifyOtpRequest`, `VerifyOtpResponse`) |
| Services  | ✅ Complete (`MfaService`, `OtpService`, `VerifyResult`) |
| Controller| ❌ Missing |
| PayeeService | ❌ Missing |
| Tests     | ❌ Missing |

---

## Files to Create

### 1. `PayeeService.java`
**Package**: `com.bank.payee.service`
- In-memory `ConcurrentHashMap<String, Payee>` store.
- `addPayee(Payee payee): Payee` – assigns a UUID id and `addedAt` timestamp, stores it.
- `getPayees(): List<Payee>` – returns all stored payees.

### 2. `PayeeController.java`
**Package**: `com.bank.payee.controller`
**Base path**: `/api/payees`

| Method | Path           | Body               | Response |
|--------|----------------|--------------------|----------|
| POST   | `/initiate-mfa`| `AddPayeeRequest`  | `200 InitiateMfaResponse` |
| POST   | `/verify-otp`  | `VerifyOtpRequest` | `200/400/423 VerifyOtpResponse` |

**`POST /initiate-mfa` logic**:
1. Build a `Payee` from the request.
2. Call `MfaService.initiateMfa(payee, mfaMethod)` → `MfaSession`.
3. Return `InitiateMfaResponse(sessionId, message, mfaMethod, demoOtp)`.

**`POST /verify-otp` logic**:
1. Call `MfaService.verifyOtp(sessionId, otpCode)` → `VerifyResult`.
2. Map `VerifyResult.Status` → HTTP response:
   - `SUCCESS` → store payee via `PayeeService`, return `200 VerifyOtpResponse.success(...)`.
   - `WRONG_OTP` → return `400 VerifyOtpResponse.failure(...)`.
   - `LOCKED` → return `423 VerifyOtpResponse.locked(...)`.
   - `NOT_FOUND` / `EXPIRED` → return `400 VerifyOtpResponse.error(...)`.

---

## Test Plan

### `MfaServiceTest`
- `initiateMfa_SMS_createsSessionAndSendsOtp`
- `initiateMfa_TOTP_createsSessionAndSendsTotpCode`
- `verifyOtp_correctOtp_returnsSuccess`
- `verifyOtp_wrongOtp_decrementsAttempts`
- `verifyOtp_maxFailedAttempts_locksSession`
- `verifyOtp_lockedSession_returnsLocked`
- `verifyOtp_expiredSession_returnsExpired`
- `verifyOtp_unknownSession_returnsNotFound`

### `PayeeControllerTest`
- `initiateMfa_validRequest_returns200WithSessionId`
- `initiateMfa_missingField_returns400`
- `verifyOtp_correctOtp_returns200WithPayee`
- `verifyOtp_wrongOtp_returns400WithRemainingAttempts`
- `verifyOtp_lockedSession_returns423`
- `verifyOtp_expiredSession_returns400`
- `verifyOtp_notFoundSession_returns400`
