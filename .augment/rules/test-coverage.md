# Test Coverage Code Review Guidelines
### Standards for coverage metrics, critical paths, and quality assurance

**Description:** Guidelines for reviewing test coverage to ensure high-quality protection, focusing on critical paths rather than just line counts.  
**Applicable Files:** `**/*test*`, `**/*spec*`, `**/*.test.{ts,js}`, `**/*Test.java`, `**/*coverage*`, `**/codecov*`, `**/*.lcov`.

---

## 📊 Coverage Metrics & Thresholds

### [understand-line-vs-branch-coverage] - Severity: Medium
Line coverage alone is insufficient.
- **Priority:** Focus on **Branch Coverage** (if/else outcomes). 100% line coverage can still miss unexecuted decision paths.
- **Goal:** Set a project-wide minimum (e.g., 80%), but require higher thresholds (90%+) for critical business modules.

### [track-coverage-trends] - Severity: Medium
Coverage should be a stable or upward trend.
- **Regression:** Fail CI builds if a Pull Request reduces the overall coverage percentage.
- **Granularity:** Monitor coverage by module; don't let high coverage in trivial "utility" code mask a lack of tests in complex domains.

---

## 🛡️ Critical Path Protection

### [ensure-security-sensitive-coverage] - Severity: Critical
There must be **zero coverage gaps** in security-sensitive code.
- **Areas:** Authentication, Authorization, Input Validation, and Cryptography.
- **Requirement:** Review security tests specifically during code review to ensure they verify "deny by default" behaviors.

### [prioritize-error-handling-coverage] - Severity: High
Bugs often hide in the code that handles failures.
- **Requirement:** Explicitly test `catch` blocks, error callbacks, and exception paths.
- **Edge Cases:** Ensure boundary values (empty collections, `null` inputs) are covered even if the "happy path" already executes those lines.

### [test-integration-points] - Severity: High
Code interacting with external systems (Dapr sidecars, Databases, APIs) must have comprehensive coverage for both **Success** and **Failure** (timeout, 500 errors) scenarios.

---

## 💎 Quality Over Quantity

### [prevent-coverage-gaming] - Severity: High
Watch for "smoke tests" that execute code without meaningful assertions.
- **Assertion Quality:** Every test must verify a specific outcome. Tests that only "touch" code to inflate numbers provide false confidence.
- **Mutation Testing:** Consider using tools like **PITest** (Java) or **Stryker** (TS) to verify that your tests actually catch bugs when code is intentionally mutated.

### [handle-hard-to-test-code] - Severity: Medium
If code is too complex to cover, it is a signal for **refactoring**.
- **Action:** Extract dependencies for mocking or simplify nested logic. Hard-to-test code is technical debt that will eventually break.

---

## 📝 Reporting & CI Integration

### [configure-pr-coverage-gates] - Severity: High
- **Visibility:** Use tools (e.g., Codecov, SonarQube) to post a coverage summary directly in the Pull Request.
- **Diff Coverage:** Focus review on the **Coverage Diff**—ensure 100% of the *new* code is covered before it is merged.

### [generate-actionable-reports] - Severity: Medium
Integrate coverage visualization into the IDE so developers can see untested lines/branches in real-time while writing code.
