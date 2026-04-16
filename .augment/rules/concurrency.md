# Concurrency Code Review Guidelines
### Standards for thread safety, deadlocks, race conditions, and async patterns

**Description:** Best practices for managing concurrent execution, ensuring thread safety, preventing deadlocks, and optimizing asynchronous patterns.
**Applicable Files:** All multi-threaded or async source code (`.java`, `.ts`, `.go`, `.py`, `.rs`, `.cs`, etc.).

---

## 🧵 Thread Safety & State Protection

### [protect-shared-mutable-state] - Severity: Critical
All shared mutable state **must** be protected by synchronization primitives.
- **Action:** Use Mutexes, Locks, or `synchronized` blocks.
- **Java 26 Note:** When using **Virtual Threads**, prefer `ReentrantLock` over `synchronized` to avoid "Carrier Thread Pinning" during I/O.
- **Verification:** Run tests with race detectors (e.g., `go -race` or ThreadSanitizer).

### [use-atomic-operations] - Severity: Medium
Use atomic variables (e.g., `AtomicInteger`, `atomic.Int64`) for simple counters or flags. They are more performant than locks but remember they do **not** provide atomicity for compound operations (e.g., check-then-act).

### [minimize-lock-scope] - Severity: High
Keep critical sections as small as possible.
- **Rule:** Never perform I/O, network calls, or long computations while holding a lock. This reduces contention and prevents system-wide bottlenecks.

---

## 🛑 Deadlock Prevention

### [acquire-locks-in-consistent-order] - Severity: Critical
Always acquire multiple locks in a **consistent, documented order**.
- **Action:** Define a global hierarchy for lock acquisition to prevent circular wait conditions.
- **Rule:** If you need Lock A and Lock B, every thread in the system must acquire them in the order (A -> B).

### [use-lock-timeouts] - Severity: High
Use `tryLock` with a timeout instead of indefinite blocking. This allows the system to recover from potential deadlocks and log the event for debugging rather than freezing permanently.

---

## 🏎️ Race Condition Prevention

### [avoid-check-then-act-races] - Severity: High
Patterns like `if (condition) { act(); }` are inherently racy. The condition may change between the check and the act.
- **Solution:** Wrap the entire sequence in a lock or use atomic "Compare-And-Swap" (CAS) operations.

### [use-thread-safe-collections] - Severity: High
Standard collections (HashMap, ArrayList) are not thread-safe.
- **Action:** Use `ConcurrentHashMap`, `CopyOnWriteArrayList`, or `sync.Map`.
- **Note:** Iterating over these collections might still require external synchronization if you need a consistent "snapshot" of the data.

---

## ⏳ Async & Event-Loop Patterns

### [understand-event-loop-blocking] - Severity: Critical
In single-threaded runtimes (**Node.js**, **Python asyncio**), **never** block the event loop with heavy CPU tasks.
- **Result:** Blocking the loop freezes all concurrent users.
- **Solution:** Offload CPU-heavy work to Worker Threads or separate processes.

### [handle-async-cancellation] - Severity: Medium
Always implement cancellation logic for async tasks.
- **Tools:** Use `AbortController` (JS/TS), `CancellationToken` (.NET), or `Context` (Go).
- **Action:** Check the cancellation state before starting expensive operations and clean up resources (sockets, file handles) immediately on abort.

### [implement-backpressure] - Severity: High
Prevent fast producers from overwhelming slow consumers.
- **Action:** Use bounded queues or reactive streams. If the consumer is full, signal the producer to slow down or reject new tasks to avoid memory exhaustion.

### [avoid-async-void] - Severity: High
Avoid `async void` (except in top-level event handlers).
- **Reason:** Exceptions in `async void` methods cannot be caught by callers and can crash the entire process.
- **Action:** Always return a `Task`, `Promise`, or `Future`.
