# Integration Testing Code Review Guidelines
### Standards for test isolation, external dependencies, and environment stability

**Description:** Guidelines for reviewing integration tests (IT) and E2E tests to ensure reliable verification of cross-service and database interactions.  
**Applicable Files:** `**/*integration*`, `**/*IT.java`, `**/*.test.{ts,js}`, `**/*_test.go`, `**/test_*.py`, etc.

---

## 🏗️ Test Isolation & Environment

### [isolate-test-environments] - Severity: Critical
Integration tests must **never** run against production or shared staging systems.
- **Action:** Use dedicated test databases and queues. Verify that environment variables (e.g., `DATABASE_URL`) are explicitly pointing to test-only resources.

### [use-test-containers] - Severity: Medium
For Java 26 and Spring Boot 4, utilize **Testcontainers**.
- **Requirement:** Spin up fresh, containerized instances of Redis, Postgres, or Kafka for each test suite run. This ensures local and CI environments are identical.

### [ensure-database-cleanup] - Severity: High
Tests must not leave "leftover" data that could pollute subsequent tests.
- **Strategies:** Use `@Transactional` rollbacks (fastest), table truncation, or unique UUIDs for every test record to ensure parallel safety.

---

## 🛰️ External Dependencies & Dapr

### [use-service-mocking-strategically] - Severity: Medium
When testing components that use **Dapr Service Invocation**:
- Use **WireMock** or **MockServer** to simulate the Dapr sidecar responses.
- Use real services only when testing actual contract compliance or complex distributed workflows.

### [handle-external-service-failures] - Severity: High
Integration tests are the primary place to verify **resiliency patterns**.
- **Action:** Explicitly test how the application behaves when a dependency returns a `500`, `403`, or times out. Ensure retries and fallbacks (via Dapr Resiliency) work as intended.

### [manage-secrets-securely] - Severity: Critical
Never hardcode API keys or DB passwords in test code.
- **Action:** Inject secrets via CI/CD environment variables or use a `.env.test` file (added to `.gitignore`).

---

## 📊 Data Management

### [use-test-data-factories] - Severity: Medium
Avoid manual object creation.
- **Action:** Use **ObjectMother** or **Data Builder** patterns to centralize default values. This makes tests more readable and easier to update when the schema changes.

### [avoid-production-data] - Severity: Critical
Using real production data in tests is a legal and security violation (GDPR/PII).
- **Requirement:** Always use synthetic data or anonymized datasets that match production patterns but contain no real user info.

---

## ⚡ Performance & CI/CD

### [optimize-test-suite-speed] - Severity: Medium
Integration tests are inherently slower than unit tests.
- **Optimization:** Reuse containers across the test suite (Singleton Pattern) where safe.
- **Selective Testing:** Tag tests (e.g., `@Tag("slow")`) so they can be excluded from standard PR checks and run only on merges or nightly builds.

### [ensure-parallel-safety] - Severity: High
If tests are running in parallel, ensure they use **unique identifiers** (UUIDs) for database entries and queue topics to avoid resource contention and flaky results.
