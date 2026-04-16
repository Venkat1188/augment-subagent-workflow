# Sonar Findings — SCRUM-1 — 2026-04-16

## Quality Gate: FAILED ✗

## Summary
| Severity    | Count |
|---|---|
| 🔴 Blocker  | 0 |
| 🔴 Critical | 4 |
| 🟠 Major    | 2 |
| 🔵 Minor    | 2 |

---

## Findings

### [CRITICAL] — Rule `java:S2068` — OtpService.java:20
**File:** `backend/src/main/java/com/bank/payee/service/OtpService.java`
**Rule:** `java:S2068` — Credentials/sensitive values logged in plaintext
**Severity:** CRITICAL
**Offending code:**
```java
log.info("📱 [MOCK SMS] OTP {} dispatched to customer's registered mobile number", otp);
```
**Required fix:** Remove the OTP value from the log argument. Log only the event, not the credential.

---

### [CRITICAL] — Rule `java:S5145` — OtpService.java:24
**File:** `backend/src/main/java/com/bank/payee/service/OtpService.java`
**Rule:** `java:S5145` — Logger prints sensitive data (OTP)
**Severity:** CRITICAL
**Offending code:**
```java
log.info("🔐 [MOCK TOTP] Current Authenticator App code: {}", otp);
```
**Required fix:** Remove OTP from log statement. Log only that the code was generated/sent.

---

### [CRITICAL] — Rule `java:S6096` — PayeeAddedEvent.java:9-15
**File:** `backend/src/main/java/com/bank/payee/model/PayeeAddedEvent.java`
**Rule:** `java:S6096` — PII / account number exposed unmasked in pub/sub event
**Severity:** CRITICAL
**Offending code:**
```java
public record PayeeAddedEvent(
        String payeeId, String name,
        String accountNumber,  // ← full unmasked PII published to broker
        String bankCode, String addedAt) {}
```
**Required fix:** Mask the account number before publishing (show only last 4 digits, e.g. `****1234`). Add a static `mask(String acct)` helper and apply it in `PayeeService.addPayee`.

---

### [CRITICAL] — Rule `java:S4502` — SecurityConfig.java:22
**File:** `backend/src/main/java/com/bank/payee/config/SecurityConfig.java`
**Rule:** `java:S4502` — CSRF protection disabled without documented justification
**Severity:** CRITICAL
**Offending code:**
```java
.csrf(csrf -> csrf.disable())
```
**Required fix:** Add inline comment explaining the architectural decision (stateless API, no browser sessions, HTTP Basic over TLS only).

---

### [MAJOR] — Rule `java:S2160` — Payee.java:5-35
**File:** `backend/src/main/java/com/bank/payee/model/Payee.java`
**Rule:** `java:S2160` — Class with fields overrides neither `equals()` nor `hashCode()`
**Severity:** MAJOR
**Offending code:** `Payee` class — no `equals()` or `hashCode()` method.
**Required fix:** Implement `equals()` and `hashCode()` based on `id` field (unique per payee).

---

### [MAJOR] — Rule `java:S3008` — MfaSession.java:12-13 + MfaService.java:65
**File:** `backend/src/main/java/com/bank/payee/service/MfaService.java`,
         `backend/src/main/java/com/bank/payee/model/MfaSession.java`
**Rule:** `java:S3008` — Mutable session fields not thread-safe under concurrent MFA attempts
**Severity:** MAJOR
**Offending code:**
```java
// MfaSession.java — non-atomic mutable fields
private int failedAttempts;
private LocalDateTime lockedUntil;

// MfaService.java:65 — race condition on increment
session.incrementFailedAttempts();
```
**Required fix:** Use `AtomicInteger` for `failedAttempts` in `MfaSession` and mark `lockedUntil` as `volatile`. Update `incrementFailedAttempts()` to use `AtomicInteger.incrementAndGet()`.

---

## Remediation Priority

| # | Severity | Rule | File | Fix |
|---|---|---|---|---|
| 1 | CRITICAL | S2068 | OtpService.java:20 | Remove OTP from log |
| 2 | CRITICAL | S5145 | OtpService.java:24 | Remove OTP from log |
| 3 | CRITICAL | S6096 | PayeeAddedEvent.java | Mask accountNumber in event |
| 4 | CRITICAL | S4502 | SecurityConfig.java:22 | Add CSRF justification comment |
| 5 | MAJOR | S2160 | Payee.java | Add equals()/hashCode() |
| 6 | MAJOR | S3008 | MfaSession.java | AtomicInteger for failedAttempts; volatile lockedUntil |
