# Terraform Code Review Guidelines
### Standards for security, state management, and infrastructure-as-code quality

**Description:** Guidelines for reviewing Terraform configurations to ensure secure state handling, reproducible builds, and drift prevention.
**Applicable Files:** `**/*.tf`, `**/*.tfvars`, `**/*.tfvars.json`, `**/terraform.tfstate`, `.terraform.lock.hcl`.

---

## 🛡️ Security

### [mark-sensitive-variables] - Severity: High
Mark variables containing secrets (API keys, passwords, tokens) with `sensitive = true`.
- This prevents values from appearing in CLI output, logs, and `terraform plan`.
- **Note:** Sensitive values are still stored in the state file; ensure the backend is encrypted.

### [no-hardcoded-credentials] - Severity: High
**Never** hardcode credentials in `.tf` files.
- Use environment variables (e.g., `TF_VAR_...`), secret management systems (Vault, AWS Secrets Manager), or IAM instance profiles.
- Ensure `.tfvars` files containing secrets are added to `.gitignore`.

### [encrypt-and-restrict-state] - Severity: High
Remote state files contain sensitive resource attributes.
- **Encryption:** Enable server-side encryption (SSE-S3, KMS, GCS encryption).
- **Access:** Implement strict IAM policies for the state bucket. Enable **versioning** and **MFA delete** protection.

---

## 🏗️ State Management & Collaboration

### [use-remote-backend-with-locking] - Severity: High
Always use a remote backend (S3, GCS, Terraform Cloud) for team environments.
- **Locking:** Mandatory to prevent concurrent modifications (e.g., use a DynamoDB table for S3).
- **Never** disable locking in production environments.

### [implement-state-backup] - Severity: Medium
Enable versioning on your state bucket for point-in-time recovery. Regularly test state recovery procedures to prepare for accidental state corruption.

---

## 💎 Code Quality & Versioning

### [pin-versions] - Severity: High
- **Providers:** Pin provider versions in `required_providers` (e.g., `~> 5.0`) to ensure reproducible applies.
- **Modules:** Never reference the `main` branch directly. Use semantic versioning tags or commit SHAs.

### [validate-variable-inputs] - Severity: Medium
Use `validation` blocks within variables to fail fast. Check for correct string formats, numeric ranges, and allowed values before the plan phase.
```hcl
variable "instance_size" {
  type = string
  validation {
    condition     = contains(["t3.micro", "t3.small"], var.instance_size)
    error_message = "Valid values: t3.micro, t3.small."
  }
}
