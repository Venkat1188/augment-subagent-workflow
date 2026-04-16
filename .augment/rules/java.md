# Java Code Review Guidelines
### Comprehensive rules for security, performance, best practices, and concurrency

**Description:** Code review guidelines for Java 26 / Spring Boot 4 applications  
**Applicable Files:** `**/*.java`

---

## 🛡️ Security Rules

### [java-sql-injection] - Severity: High
Prevent SQL injection by using parameterized queries. Avoid string concatenation in SQL queries. Use `PreparedStatement` with parameterized queries instead of `Statement` with concatenated strings.
- **❌ BAD:** `String query = "SELECT * FROM users WHERE id = " + userId; stmt.executeQuery(query);`
- **✅ GOOD:** `PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?"); pstmt.setString(1, userId);`

### [java-xxe-attack] - Severity: High
Prevent XXE (XML External Entity) attacks in XML parsing. Disable external entity processing when parsing XML. Configure `DocumentBuilderFactory` or `SAXParserFactory` to prevent XXE attacks.
- **❌ BAD:** `DocumentBuilderFactory.newInstance().newDocumentBuilder()` without security features.
- **✅ GOOD:** Set features to disallow-doctype-decl, disable external-general-entities and external-parameter-entities.

### [java-deserialization-vulnerability] - Severity: High
Avoid unsafe deserialization of untrusted data. Never deserialize untrusted data using `ObjectInputStream`. Use safe alternatives like JSON with explicit type handling or implement `ObjectInputFilter` for whitelisting allowed classes.
- **❌ BAD:** `ObjectInputStream ois = new ObjectInputStream(untrustedInput); ois.readObject();`
- **✅ GOOD:** Use `ObjectInputFilter.Config.createFilter` to whitelist allowed classes.

---

## ⚡ Performance Rules

### [java-stringbuilder-concatenation] - Severity: Medium
Use `StringBuilder` for string concatenation in loops. Regular string concatenation creates new `String` objects on each iteration.
- **❌ BAD:** `result += item + ", "` in a loop.
- **✅ GOOD:** `StringBuilder sb = new StringBuilder(); sb.append(item).append(", ");`

### [java-connection-pool-usage] - Severity: Medium
Use connection pools for database and HTTP connections. Always use pools (e.g., HikariCP) instead of creating individual connections. Configure pool sizes appropriately and always return connections to the pool.
- **❌ BAD:** `DriverManager.getConnection(url, user, password);`
- **✅ GOOD:** Use `HikariDataSource` with try-with-resources for connections.

---

## 🛠️ Best Practices Rules

### [java-optional-usage] - Severity: Medium
Use `Optional` correctly—avoid `.get()` without `.isPresent()`. Use `.orElse()`, `.orElseGet()`, or `.orElseThrow()`. Do not use `Optional` for fields or method parameters; it is designed for return types.
- **❌ BAD:** `user.get().getName()` without checking.
- **✅ GOOD:** `user.map(User::getName).orElse("Unknown")` or `user.orElseThrow()`.

### [java-null-safety] - Severity: Medium
Use null-safe practices. Use `@Nullable` and `@NonNull` annotations. Return empty collections instead of `null`. Use `Objects.requireNonNull()` for validation.
- **❌ BAD:** `return null;` for empty collections.
- **✅ GOOD:** `return Collections.emptyList();`

### [java-try-with-resources] - Severity: Medium
Use try-with-resources for all `AutoCloseable` resources (streams, connections, readers). This ensures proper cleanup even when exceptions occur.
- **❌ BAD:** Manual try-finally with `is.close()`.
- **✅ GOOD:** `try (InputStream is = new FileInputStream(file)) { ... }`

---

## 🧵 Concurrency & Java 26 Virtual Threads

### [java-virtual-thread-pinning] - Severity: High
Avoid "pinning" Virtual Threads to the carrier thread. In Java 26, performing I/O or long-running operations inside a `synchronized` block or calling native methods can pin the thread, negating the benefits of Project Loom.
- **❌ BAD:** `synchronized(lock) { performNetworkCall(); }`
- **✅ GOOD:** Use `ReentrantLock` instead of `synchronized` for blocks containing I/O.

### [java-thread-pools-avoidance] - Severity: Medium
Do not pool Virtual Threads. Virtual Threads are cheap and short-lived. Avoid using `FixedThreadPool` for virtual thread tasks.
- **❌ BAD:** `ExecutorService executor = Executors.newFixedThreadPool(100);` (for virtual tasks)
- **✅ GOOD:** `ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();`

### [java-thread-local-leakage] - Severity: High
Be cautious with `ThreadLocal` when using millions of Virtual Threads. This can lead to massive memory consumption.
- **✅ GOOD:** Prefer **Scoped Values** (introduced in Java 21+, refined in 26) for sharing data within a bounded scope.

### [java-volatile-usage] - Severity: High
Use `volatile` correctly for **visibility**, not atomicity. `volatile` ensures visibility across threads but does not provide thread-safety for compound actions (like `i++`).
- **✅ GOOD:** Use `AtomicInteger`, `LongAdder`, or `VarHandle` for atomic updates.

### [java-synchronized-blocks] - Severity: Medium
Use `private final` lock objects. Avoid synchronizing on `this` or class literals. Keep blocks small to minimize contention.
- **❌ BAD:** `public synchronized void update()`
- **✅ GOOD:** `private final Object lock = new Object(); synchronized (lock) { ... }`

---

## 🛰️ Dapr Distributed Patterns (v1.17.1)

### [dapr-distributed-lock] - Severity: High
Do not use local `synchronized` blocks for resource coordination across multiple service instances. Use the **Dapr Lock API** to ensure mutual exclusion across the cluster.
- **❌ BAD:** Using `ReentrantLock` to protect a shared database record accessed by multiple pods.
- **✅ GOOD:** `daprClient.tryLock(storeName, resourceId, lockOwner, expiry).block();`

### [dapr-idempotent-pubsub] - Severity: High
Assume "At Least Once" delivery for all Dapr Pub/Sub messages. Handlers must be idempotent to prevent duplicate processing.
- **✅ GOOD:** Use a **Dapr State Store** to track processed `cloudEvent.getId()` and exit early if the ID already exists.

### [dapr-service-invocation-grpc] - Severity: Medium
For production environments, prioritize gRPC for service-to-service calls to reduce overhead.
- **❌ BAD:** Hardcoding `http://localhost:3500/v1.0/invoke/...`
- **✅ GOOD:** Use `daprClient.invokeMethod` with the App-ID, which defaults to gRPC in the Java SDK.

### [dapr-state-optimistic-concurrency] - Severity: High
Prevent "Lost Updates" in distributed state. Always use **ETags** when performing read-modify-write operations.
- **❌ BAD:** `saveState(key, newValue)` without checking the previous version.
- **✅ GOOD:** Fetch the state with `getState()`, modify it, and send the `etag` back in the `saveState` request. If the etag mismatches, retry the operation.

### [dapr-secret-management] - Severity: Critical
Never hardcode sensitive data in `components/*.yaml`. Use Dapr **Secret Store** references.
- **❌ BAD:** `value: "my-db-password"`
- **✅ GOOD:** `secretKeyRef: { name: "db-password", key: "password" }`

### [dapr-sidecar-readiness] - Severity: Medium
Ensure the application waits for the Dapr sidecar to be ready before performing any startup logic (like database migrations).
- **✅ GOOD:** In the entrypoint script or `dapr run` command, use the `--wait-for-sidecar` flag.
