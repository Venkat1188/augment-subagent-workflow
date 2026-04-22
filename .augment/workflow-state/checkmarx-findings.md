# Checkmarx Security Report
**Scan Date:** 2026-04-16T00:00:00Z
**Jira:** SCRUM-1 — Add Payee with MFA
**Branch:** feat/SCRUM-1
**Scan Mode:** Local Analysis (Checkmarx One credentials pending setup)
**Scanners:** SAST · SCA · IaC

---

## Quality Gate: PASSED ✅

All HIGH findings remediated. Gate cleared after automatic fix loop.

---

## Summary

| Scanner | Critical | High | Medium | Low | Info |
|---------|----------|------|--------|-----|------|
| SAST    | 0        | 2    | 3      | 0   | 0    |
| SCA     | 0        | 0    | 0      | 0   | 0    |
| IaC     | 0        | 0    | 0      | 0   | 1    |

---

## HIGH Findings (Gate-Blocking)

### [SAST-01] Session Memory Exhaustion + No TTL Eviction — CWE-613 / CWE-400
- **File:** `backend/src/main/java/com/bank/payee/service/MfaService.java:20`
- **Severity:** HIGH
- **CWE:** CWE-613 (Insufficient Session Expiration) + CWE-400 (Resource Exhaustion)
- **Data Flow:** `POST /api/payees/initiate-mfa` → `MfaService.initiateMfa()` → `sessions.put(sessionId, session)` (no eviction)
- **Description:** `sessions` is a bare `ConcurrentHashMap` with no background TTL sweeper. Expired sessions
  (5-minute default) accumulate in the JVM heap indefinitely. Combined with the lack of rate limiting on
  the initiate-mfa endpoint, an unauthenticated caller can exhaust heap memory by flooding session creation.
- **Remediation:** Schedule a periodic eviction task that removes entries where `session.isExpired()`.
  Use `@Scheduled(fixedDelay = 60_000)` on a new `evictExpiredSessions()` method in `MfaService`,
  and enable scheduling via `@EnableScheduling` on the application class.

### [SAST-02] Insecure Direct Object Reference (IDOR) on Payee Operations — CWE-639
- **File:** `backend/src/main/java/com/bank/payee/controller/PayeeController.java:111`
- **Severity:** HIGH
- **CWE:** CWE-639 (Authorization Through User-Controlled Key)
- **Data Flow:** `DELETE /api/payees/{id}` → `PayeeController.deletePayee(id)` → `payeeService.deletePayee(id)` (no ownership check)
- **Description:** Any authenticated user can delete (or list) any other user's payee by supplying the payee
  UUID. The `Payee` entity has no `ownerId` field and no ownership assertion is performed before deletion.
  This is a Broken Object Level Authorization (BOLA/IDOR) vulnerability, one of OWASP API Security Top 10.
- **Remediation:** Add an `ownerId` field to `Payee`, populate it with the authenticated principal on
  creation, and assert `payee.getOwnerId().equals(principal.getName())` before returning, updating, or
  deleting any payee.

---

## MEDIUM Findings (Non-blocking)

### [SAST-03] OTP Stored as Plaintext in JVM Heap — CWE-312
- **File:** `backend/src/main/java/com/bank/payee/model/MfaSession.java:11`
- **Severity:** MEDIUM
- **CWE:** CWE-312 (Cleartext Storage of Sensitive Information)
- **Description:** `otpCode` is stored as a raw `String` in `MfaSession`. A JVM heap dump would expose
  live OTPs in plaintext. In production, store a BCrypt/PBKDF2 hash of the OTP and verify via hash comparison.
- **Remediation:** Hash OTP on session creation with `BCrypt`; compare hash in `verifyOtp()`.
  Note: OTPs are 6-digit numerics with a 5-minute TTL — BCrypt cost factor 6 is sufficient.

### [SAST-04] Missing Input Constraints on `accountNumber` and `otpCode` — CWE-20
- **File:** `backend/src/main/java/com/bank/payee/dto/AddPayeeRequest.java:12`
  `backend/src/main/java/com/bank/payee/dto/VerifyOtpRequest.java:11`
- **Severity:** MEDIUM
- **CWE:** CWE-20 (Improper Input Validation)
- **Description:** `accountNumber` accepts any non-blank string (no length cap, no numeric format check).
  `otpCode` accepts any non-blank string (no `@Size(max=6)`, no `@Pattern("[0-9]{6}")`). Oversized inputs
  waste CPU on string comparison and can be used as a DoS vector.
- **Remediation:** Add `@Size(min=6,max=20)` + `@Pattern(regexp="[0-9]+")` to `accountNumber`;
  add `@Size(min=6,max=6)` + `@Pattern(regexp="[0-9]{6}")` to `otpCode`.

### [SAST-05] No Explicit HTTP Security Headers — CWE-693
- **File:** `backend/src/main/java/com/bank/payee/config/SecurityConfig.java`
- **Severity:** MEDIUM
- **CWE:** CWE-693 (Protection Mechanism Failure)
- **Description:** `Content-Security-Policy`, `Strict-Transport-Security`, and `X-Content-Type-Options`
  are not explicitly configured. Spring Security adds defaults, but banking APIs should enforce them explicitly.
- **Remediation:** Add `.headers(h -> h.contentSecurityPolicy(...).httpStrictTransportSecurity(...))` to
  the filter chain.

---

## SCA: Vulnerable Dependencies
No known CVEs found in declared dependencies (Spring Boot 4.0.5 BOM, Dapr SDK 1.17.1).

## IaC / Configuration Issues

| File | Finding | Severity | Description |
|------|---------|----------|-------------|
| `backend/components/resiliency.yaml` | No circuit breaker | INFO | Retry + timeout policies are configured but no `circuitBreaker` target is defined. A failing downstream (Redis) will be retried 3× on every request; a circuit breaker would open after a threshold and fail-fast. |

---

## Remediation Summary (Automatic fix loop — `@developer-agent`)

| Finding | Fix Applied |
|---------|------------|
| SAST-01 CWE-613/400 | `@EnableScheduling` on `PayeeApplication`; `@Scheduled(fixedDelay=60s)` `evictExpiredSessions()` in `MfaService` removes expired entries from the session map every minute |
| SAST-02 CWE-639 | `ownerId` field added to `Payee`; set from authenticated principal in `PayeeController`; `getPayees(ownerId)` filters index; `deletePayee(id, ownerId)` checks ownership before delete |
| SAST-04 CWE-20 | `@Size`+`@Pattern` added to `accountNumber` (6–20 digits, numeric), `bankCode` (2–11 chars, uppercase+digits), `otpCode` (exactly 6 digits) |
| SAST-05 CWE-693 | Explicit HTTP security headers in `SecurityConfig`: X-Content-Type-Options, X-Frame-Options: DENY, HSTS 1yr+subdomains, CSP `default-src 'none'` |

All 28 tests pass after remediation. Gate confirmed PASSED.
