# Docker Code Review Guidelines
### Standards for container security, performance, and resource management

**Description:** Guidelines for reviewing Dockerfiles and Docker Compose configurations to ensure secure, lean, and maintainable container environments.  
**Applicable Files:** `**/Dockerfile`, `**/Dockerfile.*`, `**/*.dockerfile`, `**/docker-compose.yaml`, `**/docker-compose.yml`.

---

## 🛡️ Security

### [use-non-root-user] - Severity: High
Always run containers as a non-root user using the `USER` directive.
- Create a dedicated user with minimal permissions during the build.
- Running as root increases the attack surface significantly if the container is compromised.

### [no-secrets-in-images] - Severity: High
**Never** embed API keys, passwords, or certificates in a `Dockerfile` or image layers.
- Secrets persist in the image history even if deleted in a later layer.
- **Solution:** Use Docker Secrets, environment variables at runtime, or a vault (e.g., HashiCorp Vault).

### [avoid-privileged-mode] - Severity: High
Never use `--privileged` unless absolutely necessary. It gives the container full access to the host.
- **Solution:** Use `--cap-add` to grant only the specific Linux capabilities required.

### [use-minimal-base-images] - Severity: Medium
Prefer minimal base images like **Alpine**, **Distroless**, or **Scratch**.
- Smaller images have a reduced attack surface and result in faster pull times.

---

## 🚀 Build Best Practices

### [use-multi-stage-builds] - Severity: Medium
Use multi-stage builds to separate the build environment from the runtime environment.
- Copy only the final artifacts (e.g., the compiled `.jar` or `dist/` folder) to the final stage.
- This excludes build-time tools (compilers, git, etc.) from the production image.

### [pin-base-image-versions] - Severity: High
**Never use the `:latest` tag.** Pin images to a specific version or a SHA256 digest.
- **Example:** `FROM eclipse-temurin:26-jre-alpine` instead of `FROM openjdk:latest`.
- This ensures reproducible builds and prevents unexpected breaking changes.

### [optimize-layer-caching] - Severity: Low
Order instructions from **least to most frequently changing**.
1. Install OS packages.
2. Copy dependency files (e.g., `pom.xml` or `package.json`).
3. Install application dependencies.
4. Copy source code.

---

## 🌐 Networking & Resources

### [network-isolation] - Severity: Medium
Use Docker networks to isolate tiers (e.g., `frontend-net`, `backend-net`).
- Avoid `--network=host` as it removes isolation and gives the container full host network access.
- Only expose ports that are strictly necessary using `EXPOSE`.

### [set-resource-limits] - Severity: Medium
Always define memory and CPU limits in `docker-compose.yml` or via CLI flags.
- Unbounded containers can monopolize host resources and cause system instability.
- **Example (Compose):**
  ```yaml
  deploy:
    resources:
      limits:
        cpus: '0.5'
        memory: 512M
