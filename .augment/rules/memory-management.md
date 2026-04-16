# Memory Management Code Review Guidelines
### Standards for preventing leaks, object pooling, and garbage collection efficiency

**Description:** Best practices for memory management covering leaks, object pooling, GC optimization, and profiling across backend and frontend stacks.  
**Applicable Files:** All source code (`.java`, `.ts`, `.js`, `.py`, `.go`, etc.).

---

## 💧 Memory Leak Prevention

### [cleanup-event-listeners-and-timers] - Severity: High
Always remove event listeners and clear timers (`setInterval`, `setTimeout`) when components unmount or objects are disposed.
- **React/TS:** Use the cleanup function in `useEffect`. Store listener references or use `AbortController` to cancel multiple listeners at once.
- **Java:** In long-lived applications, ensure that listeners attached to static or singleton objects are explicitly removed to allow the GC to reclaim memory.

### [avoid-closure-leaks] - Severity: High
Closures capture their enclosing scope, which can prevent garbage collection of large objects.
- **Action:** Be cautious with closures in long-lived callbacks. Explicitly null out references to large objects inside the closure once they are no longer needed. Use `WeakRef` or `WeakMap` for optional metadata.

### [handle-subscription-cleanup] - Severity: High
Unsubscribe from Observables (RxJS), event emitters, and pub/sub channels.
- **Action:** Use automatic unsubscription patterns like `takeUntil` or `take(1)` in RxJS. Store subscription references and clear them in lifecycle destroy hooks.

---

## 🏊 Object Pooling & GC Patterns

### [avoid-allocation-in-hot-loops] - Severity: High
Avoid creating new objects inside frequently executed loops.
- **Java 26:** While Java 26 has high-performance GCs (like ZGC), excessive allocation in hot loops still causes latency spikes. Pre-allocate buffers or reuse objects where possible.
- **Rule:** Use `StringBuilder` instead of string concatenation inside loops to prevent creating thousands of intermediate string objects.

### [use-object-pooling-judiciously] - Severity: Medium
Use object pooling for frequently created/destroyed heavy objects (e.g., DB connections, thread pools).
- **Requirement:** Ensure pooled objects are **reset to a clean state** (clearing fields/buffers) before returning to the pool to prevent data leakage between uses.

### [prefer-value-types] - Severity: Medium
In languages that support them (Go, Rust, C#), use value types/structs instead of reference types to reduce heap allocation.
- **Java:** Be mindful of boxing/unboxing overhead (e.g., using `Integer` instead of `int`).

---

## 📦 Large Object Handling

### [stream-large-data] - Severity: High
Never load massive files or database result sets entirely into memory.
- **Action:** Use streaming APIs for File I/O and HTTP responses. Process data in chunks/batches to allow the GC to reclaim memory between segments.

### [implement-lazy-loading] - Severity: Medium
Use lazy loading to defer loading expensive data until it is actually accessed.
- **Caution:** Be aware of the "N+1 query" risk in ORM contexts when using lazy loading.

---

## 🔍 Memory Profiling & Monitoring

### [implement-memory-monitoring] - Severity: High
Monitor memory usage in production.
- **Metrics:** Track heap size, GC frequency, and GC pause times.
- **Java 26:** Monitor **ZGC** metrics to ensure the concurrent collector is keeping up with the allocation rate.
- **Action:** Set up alerts for memory anomalies and "Gradual Growth" patterns which indicate slow leaks.

### [take-heap-snapshots] - Severity: High
Use heap snapshots during development to identify leaks.
- **Tools:** Use Chrome DevTools (Frontend), VisualVM, or JProfiler (Java). Compare snapshots over time to see which objects are being retained unexpectedly.

### [establish-memory-budgets] - Severity: Medium
Set memory budgets for major features and services.
- **Action:** Monitor against these budgets in CI/CD. Fail builds if a new feature causes a significant, unjustified jump in baseline memory usage.
