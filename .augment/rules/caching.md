# Caching Code Review Guidelines
### Standards for caching strategies, invalidation, and distributed consistency

**Description:** Best practices for application-level, distributed (Redis/Dapr), and HTTP caching.  
**Applicable Files:** All source code (`.java`, `.ts`, `.py`, etc.) and files containing `*cache*`, `*redis*`, or `*memcache*`.

---

## 🏗️ Caching Strategies

### [use-cache-aside-pattern] - Severity: Medium
For read-heavy workloads, use the **Cache-Aside** (Lazy Loading) pattern.
- **Workflow:** Read from cache -> on miss, load from DB -> update cache.
- **Requirement:** Always set a **TTL** (Time-to-Live) to ensure data eventually refreshes even if invalidation logic fails.

### [use-write-through-consistency] - Severity: Medium
Use **Write-Through** (synchronous update to both cache and DB) for data that requires high consistency immediately after a write.
- **Note:** This increases write latency but guarantees that the next read is fresh.

### [prevent-cache-stampede] - Severity: High
In high-traffic systems, prevent multiple threads from hitting the DB simultaneously when a key expires.
- **Mitigation:** Implement **locking/mutexes** during a refresh, or use the **Stale-While-Revalidate** pattern to serve old data while one background task updates the cache.

---

## 🧹 Cache Invalidation & TTL

### [set-appropriate-ttl] - Severity: High
**Never** cache data without a TTL unless you have a robust, event-driven invalidation mechanism.
- Choose TTLs based on volatility: Short (seconds) for stock prices, Long (hours/days) for configuration or static content.

### [implement-event-driven-invalidation] - Severity: Medium
For immediate consistency, trigger cache purges based on domain events.
- **Dapr Integration:** Use Dapr Pub/Sub to broadcast invalidation events across multiple microservice instances to clear local or distributed caches.

---

## 🔑 Key Design & Namespacing

### [design-cache-keys-carefully] - Severity: Medium
Keys must be unique, predictable, and namespaced.
- **Pattern:** `service:feature:entity:version:id` (e.g., `orders:v1:user:123`).
- **Versioning:** Include a version in the key (e.g., `:v2:`) when the data schema changes to prevent deserialization errors after a deployment.

---

## 🌐 Distributed Caching (Redis/Dapr)

### [handle-distributed-consistency] - Severity: High
In a distributed setup, be aware of race conditions.
- **Action:** Use **CAS (Check-and-Set)** operations or Dapr's optimistic concurrency (ETags) for conditional updates to the cache.

### [implement-cache-failover] - Severity: High
The application must **never** crash if the cache is down.
- **Action:** Implement a **Circuit Breaker**. If Redis is unreachable, the application should fall back to the database gracefully (Fail-Open).

---

## 🛡️ Security & HTTP Caching

### [avoid-caching-user-data-publicly] - Severity: Critical
**Never** cache user-specific or sensitive data in shared/public caches (like a CDN).
- **HTTP Header:** Use `Cache-Control: private, no-store` for authenticated responses.
- **Safety:** Audit your Redis contents to ensure PII is not stored in plaintext without encryption.

### [implement-etag-validation] - Severity: Medium
Use **ETags** and `If-None-Match` headers for HTTP resources. This allows the server to return a `304 Not Modified`, saving bandwidth and reducing load on the application layer.

---

## 🚩 Common Pitfalls

### [prevent-memory-pressure] - Severity: High
Monitor your cache eviction rates.
- High eviction rates indicate your cache is undersized for the working set.
- Ensure an eviction policy is set (e.g., **LRU** - Least Recently Used) so the cache doesn't crash the node when memory is full.
