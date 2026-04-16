# Contributing to the Project

Welcome! To maintain our production standards across the **Java 26 / Spring Boot 4 / Dapr** backend and the **TypeScript / React** frontend, all contributors must follow the guidelines outlined below.

## 📁 Engineering Guidelines

We use specific "Skill Files" and Guideline documents to ensure consistency and safety. Please review the relevant section before starting your task:

### 1. Backend Standards
*   **[Java & Dapr Production Skill](./java-spring-boot-dapr-production.md)**: Standards for Java 26, Spring Boot 4 modularity, and Dapr sidecar integration (Virtual Threads, gRPC, and State Management).
*   **[Java Code Review Guidelines](./java.md)**: Security (SQLi, XXE), performance (StringBuilder), and concurrency rules (Virtual Thread pinning).

### 2. Frontend Standards
*   **[TypeScript Review Guidelines](./typescript.md)**: Type safety (avoiding `any`), security (XSS, Prototype Pollution), and bundle optimization.
*   **[React Design Guidelines](./react.md)**: Performance (memoization, virtualization), state management, and Hook safety.

---

## 🚀 Getting Started

### Prerequisites
- **JDK 26** and Maven 3.9+
- **Node.js 20+** and pnpm/npm
- **Dapr CLI** initialized (`dapr init`)

### Development Workflow
1.  **Environment Setup**:
    ```bash
    # Start the backend with Dapr sidecar
    dapr run --app-id backend --app-port 8080 --resources-path ./components -- mvn spring-boot:run
    ```
2.  **Linting & Validation**:
    Before submitting a PR, run the automated validators:
    - **Backend**: `python scripts/validate_dapr_config.py ./components`
    - **Frontend**: `npm run lint`

## 🛠 Pull Request Process

1.  **Self-Review**: Compare your changes against the relevant `.md` guidelines linked above.
2.  **Security First**: Any new API endpoint or external data handling must pass the [Security Rules](./java-guidelines.md#security-rules).
3.  **Performance**: Ensure no new "infinite loops" in React or "thread pinning" in Java are introduced.
4.  **Tests**: Include unit tests. For Dapr services, use the `DaprClient` mocks provided in the test suite.

## 📜 Code of Conduct
We are committed to a professional and inclusive community. Please be respectful in all PR comments and code reviews.
