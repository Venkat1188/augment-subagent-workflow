# Input Validation Security Code Review Guidelines
### Standards for preventing XSS, Injection, and Malicious Data Entry

**Description:** Guidelines for input validation and sanitization to prevent XSS, SQLi, Path Traversal, and other injection-based attacks.
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, etc.), controllers, handlers, and forms.

---

## 🛡️ XSS Prevention (Cross-Site Scripting)

### [encode-output-for-context] - Severity: Critical
Always encode output based on where it is displayed.
- **HTML Body:** Use entity encoding.
- **Attributes:** Use attribute encoding.
- **JavaScript:** Use JSON serialization or JS-specific encoding for script contexts.
- **Frameworks:** Rely on React/Thymeleaf auto-escaping, but verify contexts where it is disabled (e.g., `dangerouslySetInnerHTML`).

### [sanitize-html-input] - Severity: Critical
If your application must accept rich text (HTML):
- Use a battle-tested library like **DOMPurify** (TS) or **OWASP Java HTML Sanitizer**.
- **Rule:** Never use regex to sanitize HTML. Whitelist tags/attributes and re-sanitize on every output.

### [implement-csp] - Severity: High
Implement a strict **Content-Security-Policy (CSP)** header.
- Start with `default-src 'self';`.
- Avoid `'unsafe-inline'` and `'unsafe-eval'`. Use nonces for authorized inline scripts.

---

## 💉 Injection Prevention

### [prevent-sql-injection] - Severity: Critical
**Never** concatenate user input into SQL queries.
- **Action:** Use **Parameterized Queries** or JPA/Hibernate repositories.
- For dynamic table/column names, use a strict whitelist; never pass raw strings from the client.

### [prevent-command-and-template-injection] - Severity: Critical
- **Command:** Avoid shell execution. Use subprocess APIs with list arguments (e.g., `ProcessBuilder` in Java).
- **Template:** Never pass user input to template engines (Thymeleaf, Jinja) as executable code. Sandbox your engines.

### [prevent-xml-injection-xxe] - Severity: High
Disable DTDs and external entity processing in XML parsers to prevent **XXE attacks**.
- **Java:** `factory.setFeature("http://apache.org", true);`

---

## 📁 File Upload Security

### [validate-file-types-and-size] - Severity: High
- **Content Inspection:** Validate types by **magic bytes** (file signature), not just extension or MIME type.
- **Size Limits:** Enforce limits at the Proxy (Nginx), App (Spring `max-file-size`), and Storage levels.

### [secure-file-storage] - Severity: High
- **Location:** Store uploads outside the web root to prevent direct execution.
- **Naming:** Generate a new, random filename (UUID). Never use the user-provided filename on the disk.
- **Sanitization:** Remove path components (`../`) to prevent directory traversal.

---

## 📏 Data Validation Best Practices

### [use-whitelist-validation] - Severity: High
Define what **is** allowed rather than what **isn't**.
- Use strict Regex for formats (Email, Zip codes).
- Reject by default: If the input doesn't match the whitelist, discard it.

### [implement-boundary-checking] - Severity: High
Validate numeric inputs for **Overflow** and **Underflow**.
- Ensure array indices and collection sizes are within expected limits to prevent resource exhaustion (DoS).

### [validate-json-depth] - Severity: Medium
Limit JSON parsing depth (e.g., max 32 levels) to prevent "Billion Laughs" style attacks or stack overflow during deserialization.
