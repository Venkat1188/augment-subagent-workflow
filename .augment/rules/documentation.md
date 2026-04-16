# Documentation Code Review Guidelines
### Standards for comments, API docs, READMEs, and architecture

**Description:** Guidelines for maintaining high-quality documentation at the code, API, and architectural levels.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, etc.), `.md`, `.rst`, `README*`, and `CHANGELOG*`.

---

## 📝 Code Comments & Inline Docs

### [explain-why-not-what] - Severity: Medium
Comments should explain the **reasoning** behind a block of code, not restate what the code does.
- **❌ Avoid:** `// Increment counter`
- **✅ Good:** `// Increment to handle off-by-one error in legacy Dapr state store response`

### [use-docstrings-consistently] - Severity: Medium
All public APIs, classes, and methods must use language-standard docstrings.
- **Java:** Javadoc (`/** ... */`)
- **TypeScript:** JSDoc (`/** ... */`)
- **Benefit:** Enables IDE tooltips and automated documentation generation.

### [remove-outdated-comments] - Severity: Medium
Outdated comments are more dangerous than no comments. When code is modified, the associated comments **must** be updated or removed to prevent misleading future maintainers.

---

## 🔌 API Documentation

### [document-parameter-and-returns] - Severity: Medium
Every public method must document:
- **Parameters:** Type, purpose, constraints, and whether they are `@Nullable`.
- **Return Values:** Expected format, nullability, and potential side effects.
- **Exceptions:** List all checked and common runtime exceptions thrown (e.g., `@throws EntityNotFoundException`).

### [provide-code-examples] - Severity: Medium
For complex utility methods or API clients, include a small usage example within the docstring. Examples are often more effective than descriptions for preventing developer misuse.

---

## 📖 README & Project Entry

### [include-setup-and-env-requirements] - Severity: High
The root `README.md` must allow a new developer to set up the project from scratch.
- **Requirements:** Specify exact versions (e.g., Java 26, Node 20) and system dependencies (e.g., Dapr CLI, Docker).
- **Prerequisites:** Include any required environment variables or local secrets setup.

### [document-architecture-overview] - Severity: Medium
Provide a high-level overview of the system architecture. Explain how the Spring Boot services interact with Dapr sidecars and the React frontend. Use **Mermaid.js** for version-controlled diagrams.

---

## 🏛️ Architectural Documentation

### [maintain-decision-records] - Severity: Medium
Use **Architecture Decision Records (ADRs)** for significant design choices (e.g., "Why we chose Redis for caching over Memcached"). Document the context, the decision, and the long-term consequences.

### [document-data-flow] - Severity: Medium
Map out how data moves through the system, especially across trust boundaries. Include details on data transformation, storage locations, and retention policies to aid in compliance (GDPR) and debugging.

### [maintain-runbooks] - Severity: High
Document operational procedures for deployment, rollback, and incident response. A clear runbook is critical for maintaining the "Production-Ready" status of Spring Boot microservices.
