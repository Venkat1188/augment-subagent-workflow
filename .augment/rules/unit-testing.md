# Unit Testing Code Review Guidelines
### Standards for test quality, mocking, coverage, and best practices

**Description:** Guidelines for reviewing unit tests across all project languages to ensure high-quality, reliable, and descriptive test suites.  
**Applicable Files:** `**/*test*`, `**/*spec*`, `**/*.test.{ts,js}`, `**/*.spec.{ts,js}`, `**/*Test.java`, `**/*Spec.scala`, etc.

---

## 💎 Test Quality & Determinism

### [ensure-test-determinism] - Severity: High
Tests must produce the same result every time.
- **❌ Avoid:** Dependencies on the current system time, random values, or external network services.
- **✅ Action:** Use fixed clock dates, seeded random generators, and mocks for external I/O. Flaky tests must be fixed or removed immediately.

### [ensure-test-independence] - Severity: High
Tests must not rely on execution order or shared mutable state.
- **Action:** Each test must set up its own preconditions. Use `beforeEach` for common setup, but never allow one test to rely on the side effects of another.

### [test-boundary-and-error-conditions] - Severity: High
Don't just test the "happy path."
- **Boundaries:** Test empty inputs, `null` values, and max/min numeric ranges.
- **Errors:** Verify that correct exceptions are thrown and error messages are meaningful.

---

## 🎭 Mocking & Test Doubles

### [avoid-over-mocking] - Severity: Medium
Do not mock everything. Over-mocking tests implementation details rather than behavior, making refactoring difficult.
- **Action:** Mock only external dependencies (APIs, DBs), I/O, and non-deterministic behavior. Use real objects for internal logic where practical.

### [reset-mocks-between-tests] - Severity: High
Always reset mock state between tests to prevent "state leakage." Accumulated call counts or stubbed responses from previous tests can cause false positives.

### [avoid-mocking-types-you-dont-own] - Severity: Low
Avoid mocking third-party libraries directly. Create a thin **Adapter** or **Wrapper** and mock your own interface. This protects your tests if the third-party API changes.

---

## 📈 Meaningful Coverage

### [prioritize-meaningful-coverage] - Severity: High
Coverage percentage is a vanity metric if assertions are weak.
- **Focus:** Prioritize branch coverage (ensuring `if/else` and `switch` paths are hit) over simple line coverage.
- **Critical Paths:** Ensure business-critical logic and security-sensitive code have near 100% coverage.

---

## 🏷️ Naming & Structure

### [use-descriptive-test-names] - Severity: Medium
Test names should read like a specification of the expected behavior.
- **❌ BAD:** `testMethod()`, `test1()`
- **✅ GOOD:** `should_return_empty_list_when_no_items_match_filter()`

### [follow-arrange-act-assert] - Severity: Medium
Structure every test into three clear sections separated by whitespace:
1.  **Arrange:** Set up preconditions and mocks.
2.  **Act:** Execute the specific method under test.
3.  **Assert:** Verify the outcome.

---

## 🛠️ Best Practices

### [keep-tests-fast] - Severity: Medium
Unit tests should run in milliseconds. If a test is slow, it is likely an integration test masquerading as a unit test. Mock the slow dependencies (Network/File System) to keep the feedback loop tight.

### [test-public-interface] - Severity: Medium
Test the **public interface**, not private implementation details. If you feel the need to test a private method, it is often a signal that the logic should be extracted into a separate, injectable class.

### [avoid-test-logic] - Severity: Medium
Keep tests linear. Avoid `if` statements or `for` loops inside your test code. Logic in tests introduces the risk of "bugs in the tests" and makes them harder to read.
