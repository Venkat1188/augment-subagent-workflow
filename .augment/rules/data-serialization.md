# Data Serialization Code Review Guidelines
### Standards for safe serialization, binary formats, and schema evolution

**Description:** Best practices for JSON safety, binary formats (Protobuf/Avro), security, and versioning across all supported languages.  
**Applicable Files:** `.py`, `.java`, `.ts`, `.js`, `.go`, `.rb`, `.rs`, `.cs`, `.proto`, and files containing `*serializ*`, `*protobuf*`, or `*avro*`.

---

## 🛡️ Security & Safety

### [prevent-deserialization-attacks] - Severity: Critical
Deserialization of untrusted data can lead to Remote Code Execution (RCE).
- **Never** use unsafe deserializers (e.g., Python `pickle`, Java `ObjectInputStream` without filters) on untrusted data.
- Prefer data-only formats like **JSON** or **Protobuf**.
- **Sign Serialized Data:** Use HMAC or digital signatures to verify integrity before deserializing sensitive payloads (e.g., cookies, tokens).

### [parse-json-safely] - Severity: High
- **Validate Schema:** Use **Zod**, **JSON Schema**, or Jackson/Gson annotations to validate structure immediately after parsing.
- **Set Limits:** Define maximum payload size and nesting depth to prevent Denial of Service (DoS) via recursion or memory exhaustion.
- **Precision:** Be aware of JavaScript's `number` precision limits (> 2^53). Use strings for large `BigInt` or `Decimal` values.

---

## 🏎️ Performance & Binary Formats

### [use-protobuf-for-performance] - Severity: Medium
For high-performance internal service-to-service communication (especially in Dapr/gRPC environments):
- Use **Protocol Buffers (Protobuf)**. It is smaller and faster to parse than JSON.
- Maintain clear `.proto` contracts as the source of truth.

### [use-streaming-for-large-data] - Severity: High
Do not load massive documents into memory.
- Use **streaming parsers** (e.g., Jackson `JsonGenerator`, Python `ijson`).
- Implement **lazy parsing** if only a small subset of a large document is required.

### [use-compression-appropriately] - Severity: Medium
Use **LZ4** or **Snappy** for high-speed internal transfers, or **Gzip** for storage/public APIs. Balance CPU cost against transport savings.

---

## 🔄 Versioning & Compatibility

### [design-for-backward-compatibility] - Severity: High
Ensure "Read-Forward, Read-Backward" compatibility:
- **New Readers:** Must handle old data (provide defaults for new fields).
- **Old Readers:** Must handle new data gracefully (ignore unknown fields).
- **Deprecation:** Mark fields as `@Deprecated` instead of removing them immediately.

### [version-serialization-formats] - Severity: Medium
Include a version indicator in your data structures. Use a **format version** (e.g., `v1`, `v2`) rather than the application version to trigger specific migration logic or logic branches.

### [handle-missing-fields-gracefully] - Severity: Medium
Provide sensible defaults for new optional fields. Document the difference between "Missing" and "Null" semantics to avoid ambiguity during deserialization.

---

## 🛠️ Consistency Rules

### [serialize-dates-consistently] - Severity: Medium
Standardize on **ISO 8601** (e.g., `2026-04-15T19:54:00Z`). Always include timezone offsets and document the expected precision (milliseconds vs. microseconds) in your API specifications.

### [handle-special-values] - Severity: Medium
Be explicit about `NaN`, `Infinity`, and `undefined`, as they are not valid in standard JSON. Decide on a standard (e.g., `NaN` becomes `null`) and enforce it globally.
