# Data Privacy Code Review Guidelines
### Comprehensive rules for PII handling, GDPR compliance, and data protection

**Description:** Data privacy best practices covering PII handling, GDPR compliance, data retention, anonymization, and cross-border considerations.  
**Applicable Files:** All source code (`.py`, `.java`, `.ts`, `.js`, etc.) and files containing `*privacy*`, `*user*`, `*personal*`, or `*gdpr*`.

---

## 🔐 PII Handling & Protection

### [identify-pii-correctly] - Severity: Critical
Properly identify all Personally Identifiable Information (PII): names, emails, IPs, location data, and biometrics. Document PII fields in schemas and review data flows for exposure.

### [encrypt-pii-at-rest-and-transit] - Severity: Critical
- **At Rest:** Use **AES-256** or stronger. Database-level encryption is often insufficient; use field-level encryption for highly sensitive data.
- **In Transit:** Use **TLS 1.2+** for all transmissions, including internal service-to-service calls.

### [mask-pii-in-logs] - Severity: High
Never log raw PII. Implement automatic masking or redaction in logging frameworks. Use tokenization or correlation IDs if identifiers are needed for debugging.

### [minimize-pii-collection] - Severity: High
Collect only the data necessary for the specific business purpose. Regularly audit forms and APIs to remove "nice to have" fields.

---

## 🇪🇺 GDPR & Regulatory Compliance

### [implement-consent-management] - Severity: Critical
Consent must be freely given, specific, and informed.
- Store consent records with **timestamps**.
- Never use pre-checked boxes.
- Provide an easy way for users to withdraw consent.

### [support-right-to-erasure] - Severity: Critical
Implement "Right to be Forgotten" logic. Deletion must propagate to **backups, caches, and logs**. Verify completeness of cascading deletions.

### [enable-data-portability-and-sar] - Severity: High
- **Portability:** Provide data exports in machine-readable formats (JSON/CSV).
- **Access Requests (SAR):** Return all held data, processing purposes, and recipients within legal timeframes. Always verify identity before disclosure.

---

## ⏳ Data Retention & Anonymization

### [implement-retention-policies] - Severity: High
Define retention periods for all data types. **Automate deletion** of data past its retention period. Do not keep data indefinitely "just in case."

### [anonymize-data-properly] - Severity: High
When sharing data for analytics:
- Remove direct identifiers.
- Apply **k-anonymity** or **differential privacy**.
- Use **pseudonymization** (replacing IDs with tokens) when complete anonymization is not feasible, keeping the mapping key in a separate, highly secure zone.

---

## 📊 Access Logging & Residency

### [implement-audit-trails] - Severity: High
Log all access to sensitive personal data (Who, What, When, Why). Logs must be **tamper-evident** and integrated with security monitoring to alert on bulk exports or unusual patterns.

### [manage-data-residency] - Severity: High
Adhere to jurisdictional requirements (e.g., data staying within the EU). Configure storage locations and document cross-border flows. Use **Standard Contractual Clauses (SCCs)** for international transfers.

### [conduct-dpia] - Severity: High
Conduct Data Protection Impact Assessments (DPIA) before starting high-risk processing, such as large-scale monitoring or profiling.
