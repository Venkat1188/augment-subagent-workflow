# API Security Code Review Guidelines
### Standards for CORS, rate limiting, request validation, and versioning

**Description:** Guidelines for securing API endpoints against common vulnerabilities, abuse, and information disclosure.  
**Applicable Files:** `**/*.java`, `**/*.ts`, `**/*controller*`, `**/*handler*`, `**/*route*`, `**/*api*`, `**/openapi*`, `**/swagger*`.

---

## 🌐 CORS & Request Security

### [validate-cors-origins] - Severity: High
**Never** use `Access-Control-Allow-Origin: *` for APIs handling sensitive data or credentials.
- **Action:** Whitelist specific, trusted origins (e.g., your production frontend domain).
- **Credentials:** Only set `Allow-Credentials: true` when using a specific origin. For cross-origin Dapr service calls, prefer token-based auth (JWT) over cookies.

### [enforce-request-size-limits] - Severity: High
Set strict body size limits at the API Gateway and Application level (Spring `MultipartProperties` or `server.max-http-header-size`).
- **Goal:** Prevent Denial of Service (DoS) attacks via massive JSON payloads or large file uploads.

### [validate-content-type] - Severity: Medium
Strictly validate the `Content-Type` header (e.g., `application/json`). Reject requests with unexpected types to prevent content-type confusion and CSRF-style attacks.

---

## 🚦 Rate Limiting & Availability

### [implement-rate-limiting] - Severity: High
Implement rate limiting on **all** endpoints.
- **Dapr Integration:** Use the [Dapr Rate Limit middleware](https://dapr.io) for infrastructure-level enforcement.
- **Auth Endpoints:** Apply much stricter limits to login and password reset routes to prevent brute-force attacks.
- **Response:** Return `429 Too Many Requests` with a `Retry-After` header.

### [handle-burst-traffic] - Severity: Medium
Use algorithms like **Token Bucket** to allow for brief, legitimate traffic bursts while maintaining a steady-state ceiling to protect backend resources.

---

## 🔍 Validation & Sanitization

### [implement-schema-validation] - Severity: High
Validate all incoming request bodies against a strict schema (e.g., **JSON Schema, Zod, or Bean Validation**).
- **Reject Unknown Fields:** Configure your parsers (like Jackson) to fail on unknown properties to prevent **Mass Assignment** vulnerabilities.
- **Sanitize:** Trim strings and validate formats (Email, UUID) before the data reaches your service layer.

---

## 🛡️ Response Security & Disclosure

### [implement-security-headers] - Severity: High
Every API response must include standard security headers:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security` (HSTS) for all HTTPS traffic.

### [handle-errors-securely] - Severity: High
**Never** return stack traces, database schema details, or software versions in API responses.
- **Action:** Use a global error handler to catch exceptions and return a generic JSON error object with a unique correlation ID for server-side log lookup.

### [control-response-data] - Severity: Medium
Minimize data exposure. Do not return full User objects if only the `userId` and `displayName` are required. Use **Data Transfer Objects (DTOs)** to explicitly define what data leaves the system.

---

## 🔄 API Versioning

### [handle-deprecation-securely] - Severity: Medium
Old API versions often lack modern security headers or patches.
- **Action:** Set clear sunset dates for old versions. Use the `Deprecation` header to inform clients. Monitor usage and force-upgrade users from versions with known vulnerabilities.
