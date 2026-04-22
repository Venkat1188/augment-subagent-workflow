# Error Handling Code Review Guidelines
### Standards for exception handling, propagation, and graceful degradation

**Description:** Best practices for managing exceptions, ensuring error context preservation, and implementing resilient patterns in distributed systems.
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, `.go`, etc.) across backend and frontend layers.

---

## 🛡️ Exception Handling & Safety

### [avoid-empty-catch-blocks] - Severity: Critical
**Never** use empty catch blocks. Silently swallowing errors makes debugging nearly impossible.
- **Action:** At a minimum, log the error. If an error is truly ignorable, document exactly *why* in a comment.

### [use-specific-exceptions] - Severity: High
Catch specific exception types (e.g., `EntityNotFoundException`) rather than generic `Exception` or `Error`.
- **Reason:** Generic catches hide logic bugs and prevent the system from applying specific recovery logic for known failure modes.

### [preserve-exception-context] - Severity: High
Always preserve the original cause when wrapping or rethrowing exceptions.
- **Java:** Use `new MyBusinessException("message", originalException)`.
- **TypeScript:** Use `new Error("message", { cause: originalError })`.
- **Goal:** Maintain the full stack trace from the source of the failure.

---

## 🚦 Error Propagation & Resilience

### [implement-circuit-breakers-and-retries] - Severity: High
For distributed systems using **Dapr**:
- **Action:** Offload retry logic and circuit breakers to **Dapr Resiliency policies** rather than coding them in Java.
- **Rule:** Only retry **idempotent** operations. Use exponential backoff with jitter to avoid overwhelming recovering services.

### [fail-fast-on-fatal-errors] - Severity: High
For unrecoverable errors (e.g., missing mandatory configuration, database unreachable at boot), fail fast and stop the service. Limping along in a broken state creates harder-to-diagnose downstream issues.

### [use-finally-for-cleanup] - Severity: High
Ensure resource cleanup (closing streams, releasing locks) occurs regardless of success or failure.
- **Java:** Use **try-with-resources** for `AutoCloseable` types.
- **TypeScript:** Use `try...finally` blocks.

---

## 📝 Logging & Diagnostics

### [include-error-context-and-ids] - Severity: High
Logs must include the "Why" and "Who."
- **Context:** Log relevant IDs (e.g., `order_id`) and the operation name.
- **Correlation:** Ensure the **Trace ID** from Dapr is included in the error log to correlate failures across microservices.

### [avoid-logging-sensitive-data] - Severity: Critical
**Never** log passwords, PII (emails, names), or full connection strings in error messages or stack traces. Sanitize inputs before they reach the logger.

---

## 👤 User-Facing Errors

### [dont-expose-internal-details] - Severity: Critical
Never show stack traces, database schema names, or raw SQL errors to the end user.
- **Security:** Internal details are a roadmap for attackers.
- **Action:** Return a generic message (e.g., "Service temporarily unavailable") and a **Correlation ID** that the user can provide to support for internal log lookups.

### [use-structured-error-codes] - Severity: Medium
Use machine-readable error codes (e.g., `INSUFFICIENT_FUNDS`, `VALIDATION_FAILED`).
- **Benefit:** This allows the React frontend to show specific UI components or localized messages based on the code rather than parsing error strings.

---

## 📉 Graceful Degradation

### [implement-graceful-fallbacks] - Severity: Medium
When a non-critical dependency fails (e.g., a recommendation engine), the system should degrade gracefully.
- **Action:** Return a default value or cached data instead of failing the entire request.
- **Visibility:** Log that the system is operating in a degraded state.
