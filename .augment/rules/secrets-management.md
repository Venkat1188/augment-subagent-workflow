# Secrets Management Security Code Review Guidelines
### Standards for secure handling of secrets and sensitive configuration

**Description:** Guidelines for secrets management covering storage, git security, rotation, logging, and configuration.  
**Applicable Files:** All source code (`.py`, `.java`, `.ts`, etc.), `.yaml`, `.json`, `.env*`, `.properties`, and infra files (`terraform`, `docker`, `ansible`).

---

## 🔐 Storage & Transmission

### [no-hardcoded-secrets] - Severity: Critical
**Never** hardcode secrets, API keys, passwords, or tokens in source code or connection strings.
- **Action:** Use environment variables for local dev and **Secret Managers** (Vault, AWS Secrets Manager) for production.
- **Dapr Integration:** Leverage [Dapr Secret Store building blocks](https://dapr.io) to retrieve secrets at runtime.

### [secure-secret-injection] - Severity: High
Inject secrets at **runtime**, not build time.
- **❌ BAD:** Baking secrets into Docker images or `application.properties` inside a JAR.
- **✅ GOOD:** Use K8s Secret CSI drivers or Dapr sidecar injectors to provide secrets to the application process only when it starts.

### [encrypt-secrets-at-rest] - Severity: High
Always encrypt secrets stored in databases or config systems. Use **envelope encryption** (data key encrypted by a master key) and ensure keys are stored separately from the data.

---

## 🛑 Git & Version Control Security

### [configure-gitignore] - Severity: High
Ensure `.gitignore` is audited to exclude:
- `.env`, `*.pem`, `*.key`, `credentials.json`, and `secrets.yaml`.
- **Pre-commit Hooks:** Use tools like **truffleHog** or **detect-secrets** to stop secrets from ever being committed to the local history.

### [clean-git-history] - Severity: Critical
If a secret is committed, **it is compromised** even if deleted in a later commit.
- **Action:** Immediately rotate the secret. Then, use tools like `BFG Repo-Cleaner` to scrub it from the entire Git history.

---

## 🔄 Lifecycle & Rotation

### [implement-key-rotation] - Severity: High
Automate the rotation of secrets (typically every 30–90 days).
- **Design:** Ensure your Spring Boot 4 application can handle secret updates (e.g., via Spring Cloud Config or Dapr's dynamic secret fetching) without requiring a restart.

### [implement-revocation-procedures] - Severity: High
Maintain an inventory of all active secrets. On employee departure or a suspected breach, follow a practiced revocation procedure to immediately invalidate credentials.

---

## 📝 Logging & Error Safety

### [redact-secrets-in-logs] - Severity: High
Implement automatic redaction in your logging framework (e.g., Logback or Winston).
- **Action:** Use filters to mask strings matching typical token patterns or specific sensitive keys like `password` or `access_token`.

### [avoid-sensitive-data-in-errors] - Severity: High
Error messages must never reveal connection strings or internal paths.
- **Action:** Disable debug output in production. Ensure Spring Boot `server.error.include-message` is set to `never` for external API consumers.
