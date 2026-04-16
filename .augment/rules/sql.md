# SQL Code Review Guidelines
### Standards for security, performance, data integrity, and maintainability

**Description:** General SQL best practices for developers and DBAs.  
**Applicable Files:** `**/*.sql`, `**/*repository*`, `**/*query*`, `**/*dao*`, `**/*mapper*`, and database logic within `.py`, `.java`, `.ts`, `.js`, `.go`, `.cs`, `.rb`.

---

## 🛡️ Security Rules

### [prevent-sql-injection] - Severity: Critical
Never concatenate user input directly into SQL queries.
- Use **parameterized queries**, prepared statements, or ORM query builders.
- String interpolation in SQL is a critical vulnerability.

### [use-parameterized-queries] - Severity: Critical
Always use placeholders (e.g., `WHERE id = ?` or `WHERE id = $1`) instead of string formatting. This prevents injection and improves query plan caching by the database engine.

### [apply-least-privilege] - Severity: High
Application connections must use roles with the minimum required permissions. Avoid using `root` or `admin` accounts.

### [sanitize-input-for-identifiers] - Severity: Critical
If dynamic table or column names are required, whitelist allowed values using enums or maps. Never allow arbitrary user input to define SQL identifiers.

### [mask-sensitive-data-in-logs] - Severity: High
Ensure queries containing PII, passwords, or tokens are not logged in plain text. Use placeholders in logs and mask values before outputting.

---

## ⚡ Performance Rules

### [use-appropriate-indexes] - Severity: High
Create indexes on columns used in `WHERE`, `JOIN`, `ORDER BY`, and `GROUP BY` clauses. Use composite indexes for multi-column filters and monitor execution plans.

### [avoid-select-star] - Severity: Medium
Explicitly list required columns. This reduces data transfer, prevents breaking changes when the schema evolves, and improves query stability.

### [prevent-n-plus-one-queries] - Severity: High
Avoid executing a query inside a loop for each item. Use **JOINs**, batch queries, or eager loading to fetch related data in a single call.

### [limit-result-sets] - Severity: High
Always use `LIMIT`/`TOP` for potential large result sets. Implement pagination (keyset or offset) to prevent memory exhaustion and application failure.

### [avoid-functions-on-indexed-columns] - Severity: Medium
Avoid functions in `WHERE` clauses on indexed columns (e.g., `WHERE YEAR(date_col) = 2024`). This prevents index usage (SARGability). Compare against the column directly.

---

## 💎 Data Integrity Rules

### [use-foreign-keys] - Severity: High
Define foreign key constraints to maintain referential integrity. This prevents orphaned records and serves as documentation for relationships.

### [define-appropriate-constraints] - Severity: Medium
Use `CHECK`, `NOT NULL`, `UNIQUE`, and `DEFAULT` constraints. The database should be the final line of defense for data validity.

### [use-transactions-appropriately] - Severity: High
Wrap related operations in transactions to ensure atomicity. Keep transactions short to minimize lock contention and choose appropriate isolation levels.

### [handle-null-values-explicitly] - Severity: Medium
Use `COALESCE`, `NULLIF`, or `IS NULL`. Remember that `NULL` comparisons with `=` or `<>` do not behave like standard values.

---

## 🛠️ Best Practices

### [version-schema-migrations] - Severity: High
Use version-controlled migration files (e.g., Flyway, Liquibase). Never apply manual schema changes to production. Ensure migrations are **backwards compatible** to support zero-downtime deployments.

### [normalize-appropriately] - Severity: Medium
Apply 3rd Normal Form (3NF) to reduce redundancy. Denormalize only when specific performance requirements justify the integrity trade-offs.

### [use-appropriate-data-types] - Severity: Medium
- Use `DECIMAL` for money (never `FLOAT`).
- Use `DATE`/`TIMESTAMP` for time (never strings).
- Choose `INTEGER` vs `BIGINT` based on growth projections.

### [use-connection-pooling] - Severity: High
Always use a connection pool (e.g., HikariCP) to manage connections efficiently. Creating new connections is expensive and impacts latency.

### [implement-soft-deletes] - Severity: Low
Consider `is_deleted` flags or `deleted_at` timestamps for data recovery or audit trails. Ensure global filters exclude these records by default.
