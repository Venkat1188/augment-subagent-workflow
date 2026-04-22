# Data Validation Code Review Guidelines
### Comprehensive rules for input validation, sanitization, and boundary checks

**Description:** Best practices for input validation, business rules, sanitization, and boundary checks across all application layers.  
**Applicable Files:** All source code (`.py`, `.java`, `.ts`, `.js`, etc.) and files containing `*validator*`, `*validation*`, or `*schema*`.

---

## 🛡️ Input Validation & Security

### [validate-all-inputs] - Severity: Critical
Never trust data from external sources (API requests, file uploads, headers, etc.).
- **Server-Side is Final:** Client-side validation is for UX only; server-side validation is the true security boundary.
- **Whitelist over Blacklist:** Define what *is* allowed (e.g., specific regex) rather than trying to block known bad patterns. Reject by default.

### [use-schema-validation] - Severity: High
Leverage declarative validation libraries (e.g., **Zod, Joi, Bean Validation, Pydantic**) for structured data. Define expected types and constraints once to ensure maintainability and consistency.

### [validate-file-uploads] - Severity: High
- **Content over Extension:** Verify file types using **magic bytes**, not just the file extension.
- **Sanitize Paths:** Rename files and use access controls to prevent path traversal.
- **Resource Limits:** Enforce strict file size limits and scan for malware.

---

## 🧠 Business Rules & State Validation

### [implement-domain-validation] - Severity: High
Validate data against business domain logic, not just format.
- **Cross-Field Constraints:** Check relationships between fields (e.g., `startDate` must be before `endDate`).
- **State Awareness:** Verify the resource is in a valid state for the requested operation and handle potential race conditions.

### [normalize-input-data] - Severity: Medium
Normalize data *before* validation.
- **Unicode:** Use Unicode normalization to prevent homoglyph attacks.
- **Formatting:** Trim whitespace and standardize formats (e.g., phone numbers, addresses) for consistent comparison and storage.

---

## 🧹 Sanitization & Error Handling

### [sanitize-output-appropriately] - Severity: Critical
Sanitization must happen at **output time** based on the context:
- **Web:** HTML-encode content to prevent XSS.
- **Database:** Use parameterized SQL to prevent injection.
- **Shell:** Escape parameters for shell commands.

### [avoid-sensitive-data-in-errors] - Severity: High
Never expose system internals (stack traces, database errors, internal paths) in error messages.
- **User-Friendly:** Provide clear, actionable messages that help the user fix the input.
- **Secure:** Log detailed errors server-side only. Return generic `400 Bad Request` or `500 Internal Server Error` to the client.

---

## 📏 Boundary & Resource Validation

### [validate-numeric-ranges-and-strings] - Severity: High
- **Numbers:** Enforce strict `min`/`max` bounds and check for overflow/underflow.
- **Strings:** Enforce minimum and maximum lengths. Reject empty strings where they are logically invalid.

### [validate-array-and-collection-sizes] - Severity: High
Limit the size of arrays and the depth of nested structures. Unbounded collections can lead to resource exhaustion and Denial of Service (DoS).

### [test-validation-thoroughly] - Severity: High
Validation logic is critical code.
- Test with **boundary values** (exactly at the limit), **edge cases** (null, empty, special characters), and **malicious input** (script tags, SQL fragments).
- Include validation checks in your automated unit test suite.
