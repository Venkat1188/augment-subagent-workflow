# Logging Code Review Guidelines
### Standards for log levels, structured logging, security, and observability

**Description:** Guidelines for logging covering structured formats, log levels, security compliance, and distributed observability.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, `.go`, etc.) and files containing `*log*` or `*logger*`.

---

## 📊 Log Level Management

### [use-log-levels-appropriately] - Severity: High
- **ERROR:** Use for failures requiring immediate action (exceptions, data corruption). These must be alert-worthy.
- **WARN:** Use for potential issues (approaching limits, deprecated usage).
- **INFO:** Use for significant lifecycle events (service started, request handled).
- **DEBUG:** Use for diagnostic data. **Must** be disabled in production by default.
- **❌ Avoid:** "Log Spam." Do not log every function entry/exit; it hides actual signals in noise.

---

## 🧱 Structured Logging (JSON)

### [use-json-format-and-consistent-fields] - Severity: Medium
Always use **JSON format** for logs in production. Structured logs are mandatory for ELK/Splunk/CloudWatch parsing.
- **Consistent Fields:** Use a standard schema across all services: `timestamp`, `level`, `message`, `service_name`, `request_id`, and `user_id`.
- **Standard Timestamps:** Use **ISO 8601** format in **UTC** (`yyyy-MM-ddTHH:mm:ssZ`).

### [include-context-fields] - Severity: High
Include contextual metadata in every log entry. Avoid embedding IDs in message strings; use separate fields (e.g., `user_id: 123` instead of `"User 123 logged in"`).

---

## 🔐 Security & Sensitive Data

### [redact-pii-and-mask-secrets] - Severity: Critical
**Never** log raw PII (emails, SSNs) or secrets (passwords, API keys).
- **Compliance:** GDPR requires data minimization. Logs are subject to "Right to be Forgotten" requests.
- **Action:** Implement automatic masking in your logging framework (e.g., Logback filters or Winston transports).

---

## ⚡ Performance & Resource Safety

### [use-async-logging] - Severity: Medium
Use **asynchronous logging** (e.g., Logback `AsyncAppender`) to prevent log I/O from blocking application threads. This is critical for high-concurrency Java 26 Virtual Thread environments.

### [configure-log-rotation] - Severity: High
Configure rotation by size (e.g., 100MB) or time. This prevents "Disk Full" scenarios which can crash nodes in a Kubernetes environment.

---

## 🕵️ Observability & Tracing

### [include-correlation-ids] - Severity: High
Every log must include a **Correlation/Trace ID**.
- **Dapr Integration:** Dapr automatically propagates W3C Trace Context. Ensure your logger is configured to extract and include `trace_id` and `span_id` from the MDC (Mapped Diagnostic Context).
- **Goal:** Enable a single request to be traced through multiple microservices by searching one ID in the log aggregator.

### [create-meaningful-alerts] - Severity: Medium
Logs should feed into alerting. Create alerts for **Error Spikes** or specific patterns (e.g., `OutOfMemoryError`), but ensure they are actionable to prevent alert fatigue.
