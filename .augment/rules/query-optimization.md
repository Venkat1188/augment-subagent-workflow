# Query Optimization Code Review Guidelines
### Standards for query analysis, N+1 prevention, pagination, and connection health

**Description:** Best practices for database query performance, efficient data fetching, and robust connection management.
**Applicable Files:** `**/*.sql`, `**/*repository*`, `**/*dao*`, `**/*query*`, and data logic in `.java`, `.ts`, `.py`, `.go`, etc.

---

## 🔍 Query Analysis & Indexing

### [analyze-query-execution-plans] - Severity: High
Always use `EXPLAIN ANALYZE` or your database's equivalent to inspect execution plans.
- **Look for:** Sequential scans on large tables, temporary disk sorts, and "High Cost" operations.
- **Action:** Verify that the database is actually using the indexes you created. If not, refactor the query or use index hints.

### [avoid-index-defeating-patterns] - Severity: High
Avoid patterns that force the database to ignore indexes (SARGability issues):
- **❌ BAD:** `WHERE UPPER(email) = 'USER@EXAMPLE.COM'` or `WHERE date_col + interval '1 day' > now()`.
- **✅ GOOD:** Use direct column comparisons: `WHERE email = 'user@example.com'` (with a case-insensitive collation) or `WHERE date_col > now() - interval '1 day'`.

---

## 🛑 N+1 Query Prevention

### [detect-n-plus-one-queries] - Severity: Critical
N+1 queries occur when you fetch a list and then execute a separate query for each item in that list to get related data.
- **Action:** Look for database calls inside loops. Use your ORM’s logging (e.g., Hibernate SQL logging) in dev to spot repetitive queries.
- **Solution:** Use **Eager Loading** (e.g., `JOIN FETCH` in JPA, `includes` in TypeORM) or the **DataLoader** pattern to batch requests.

### [consolidate-queries] - Severity: Medium
Where possible, use `IN` clauses or CTEs (Common Table Expressions) to fetch related data in a single round-trip rather than multiple individual lookups.

---

## 📄 Pagination Strategies

### [use-cursor-pagination-for-large-datasets] - Severity: High
Avoid `OFFSET` pagination for large tables. `OFFSET 10000` requires the database to scan and discard 10,000 rows.
- **Solution:** Use **Cursor/Keyset Pagination**. Use a unique, indexed column (like `id` or `created_at`) to "seek" the next set of rows: `WHERE id > :last_seen_id LIMIT 20`.

### [limit-deep-pagination] - Severity: Medium
If using offset pagination, cap the maximum page depth. Deep pagination is a common cause of database timeouts and resource exhaustion.

---

## 🤝 Join & Connection Management

### [use-connection-pooling] - Severity: High
Creating database connections is an expensive operation.
- **Requirement:** Always use a pool like **HikariCP**.
- **Action:** Release connections immediately after use. Never hold a connection open while waiting for an external API response or user input.

### [configure-timeouts] - Severity: High
- **Connection Timeout:** Prevents the app from hanging if the DB is unreachable.
- **Query Timeout:** Kills "runaway" queries that take too long, protecting the database from resource starvation.

### [avoid-select-star] - Severity: Medium
Explicitly name the columns you need.
- **Performance:** Allows the use of **Covering Indexes** where the database answers the query entirely from the index without touching the table heap.
- **Stability:** Prevents application crashes when new columns are added to the schema.

---

## 🛠️ Performance Patterns (Java 26 / Dapr)

### [use-prepared-statements] - Severity: High
Always use prepared statements. They are pre-parsed and cached by the database engine, significantly reducing overhead for repeated queries while preventing SQL injection.

### [batch-loading-in-dapr] - Severity: Medium
When using Dapr State Stores, leverage **Bulk Get** operations instead of individual `getState` calls in a loop to minimize sidecar-to-app communication overhead.
