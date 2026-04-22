# SCRUM-1 — Technical Implementation Plan (v2 — Dapr-Native)
## Skill Applied: `java-spring-boot-dapr` | Java 26 | Spring Boot 4.0.5 | Dapr 1.17.1

---

## 1. Gap Analysis — Current vs Skill Standard

| Area | Current State | Required (Skill Standard) |
|------|--------------|--------------------------|
| Java version | 22 | **26** (`-XX:+UseCompactObjectHeaders`) |
| Spring Boot | 3.3.0 | **4.0.5** (Jakarta EE 11 / Spring Framework 7) |
| Dependency style | Monolithic `spring-boot-starter-web` | **Granular**: `spring-boot-starter-web-minimal` |
| Dapr | ❌ Not used | **dapr-sdk-springboot 1.17.1** |
| State store | `ConcurrentHashMap` (in-memory) | **Dapr State Store** with ETag concurrency |
| Pub/Sub | ❌ None | **Dapr Pub/Sub** — `CloudEvent<PayeeAddedEvent>` on `payee-events` |
| Threading | Default platform threads | **Virtual Threads** (`spring.threads.virtual.enabled: true`) |
| Observability | ❌ None | **micrometer-tracing** (W3C trace propagation) |
| Resiliency | ❌ None | **Dapr resiliency.yaml** (no hardcoded retries in Java) |
| Secrets | N/A | `{{secretScope.secretKey}}` in component YAMLs |
| Service return types | Blocking `Payee` / `boolean` | **`Mono<Payee>`** / **`Mono<Boolean>`** (Reactive) |

---

## 2. pom.xml Changes

### Remove
- `spring-boot-starter-web` → replace with `spring-boot-starter-web-minimal`
- Parent version `3.3.0` → `4.0.5`
- Java version `22` → `26`

### Add
```xml
<dependency>
  <groupId>io.dapr</groupId>
  <artifactId>dapr-sdk-springboot</artifactId>
  <version>1.17.1</version>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing</artifactId>
</dependency>
```

---

## 3. application.yml (full content — NEW FILE)
```yaml
spring:
  application:
    name: payee-mfa
  threads:
    virtual:
      enabled: true
dapr:
  client:
    grpc:
      endpoint: "localhost:50001"
```

---

## 4. Dapr Component YAMLs (NEW FILES under `components/`)

### components/statestore.yaml
```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: payee-statestore
spec:
  type: state.redis
  version: v1
  metadata:
    - name: redisHost
      secretKeyRef: { name: redis-secret, key: redisHost }
    - name: redisPassword
      secretKeyRef: { name: redis-secret, key: redisPassword }
auth:
  secretStore: local-secret-store
```

### components/pubsub.yaml
```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: payee-pubsub
spec:
  type: pubsub.redis
  version: v1
  metadata:
    - name: redisHost
      secretKeyRef: { name: redis-secret, key: redisHost }
auth:
  secretStore: local-secret-store
```

### components/resiliency.yaml
```yaml
apiVersion: dapr.io/v1alpha1
kind: Resiliency
metadata:
  name: payee-resiliency
spec:
  policies:
    retries:
      payee-retry:
        policy: exponential
        maxRetries: 3
        maxInterval: 10s
    timeouts:
      payee-timeout: 5s
  targets:
    components:
      payee-statestore:
        outbound:
          retry: payee-retry
          timeout: payee-timeout
```

---

## 5. Classes to Create / Modify

### NEW: `model/PayeeAddedEvent.java`
```java
public record PayeeAddedEvent(
    String payeeId, String name, String accountNumber,
    String bankCode, String addedAt) {}
```

### MODIFY: `service/PayeeService.java` — replace ConcurrentHashMap with Dapr
```java
@Service
public class PayeeService {
    static final String STORE   = "payee-statestore";
    static final String PUB_SUB = "payee-pubsub";
    static final String TOPIC   = "payee-events";
    private final DaprClient daprClient;

    // addPayee → saveState + publishEvent → returns Mono<Payee>
    public Mono<Payee> addPayee(Payee payee) { ... }

    // getPayees → query statestore by key prefix → returns Mono<List<Payee>>
    public Mono<List<Payee>> getPayees() { ... }

    // deletePayee → getState → deleteState if exists → returns Mono<Boolean>
    public Mono<Boolean> deletePayee(String id) { ... }
}
```

### MODIFY: `controller/PayeeController.java` — reactive return types
```java
@GetMapping
public Mono<ResponseEntity<List<Payee>>> getPayees() {
    return payeeService.getPayees().map(ResponseEntity::ok);
}

@DeleteMapping("/{id}")
public Mono<ResponseEntity<Void>> deletePayee(@PathVariable String id) {
    return payeeService.deletePayee(id)
        .map(removed -> removed
            ? ResponseEntity.<Void>noContent().build()
            : ResponseEntity.<Void>notFound().build());
}
```

### KEEP unchanged
- `config/SecurityConfig.java`
- `service/MfaService.java`
- All DTOs, models, `OtpService`, `VerifyResult`

---

## 2. Classes to Modify

### 2a. `PayeeService` — add `deletePayee`

**File:** `backend/src/main/java/com/bank/payee/service/PayeeService.java`

Add the following method (no other changes needed):

```java
/**
 * Removes a confirmed payee by ID.
 * @param id the payee's UUID
 * @return true if the payee existed and was removed; false if not found
 */
public boolean deletePayee(String id) {
    return payeeStore.remove(id) != null;
}
```

---

### 2b. `PayeeController` — add `getPayees` and `deletePayee` endpoints

**File:** `backend/src/main/java/com/bank/payee/controller/PayeeController.java`

Add new import: `java.util.List`

#### New method 1 — List Payees

```java
@GetMapping
public ResponseEntity<List<Payee>> getPayees() {
    return ResponseEntity.ok(payeeService.getPayees());
}
```

#### New method 2 — Delete Payee

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletePayee(@PathVariable String id) {
    boolean removed = payeeService.deletePayee(id);
    return removed
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
}
```

---

## 3. HTTP Endpoint Specification

### GET /api/payees

| Property | Value |
|---|---|
| Method | GET |
| Path | `/api/payees` |
| Request body | None |
| Success (with data) | `200 OK` — JSON array of `Payee` objects |
| Success (empty) | `200 OK` — `[]` |

**Sample response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Alice Smith",
    "accountNumber": "ACC001",
    "bankCode": "BNKA",
    "addedAt": "2026-04-15T10:30:00"
  }
]
```

### DELETE /api/payees/{id}

| Property | Value |
|---|---|
| Method | DELETE |
| Path | `/api/payees/{id}` |
| Path variable | `id` — UUID string |
| Request body | None |
| Success | `204 No Content` |
| Not found | `404 Not Found` |

---

## 4. End-to-End Flow Diagrams

```
GET /api/payees
───────────────
Client          PayeeController         PayeeService
  │── GET /api/payees ──────────>│
  │                              │── getPayees() ──────>│
  │                              │<── List<Payee> ───────│
  │<── 200 OK [payees array] ────│

DELETE /api/payees/{id}  ── found
──────────────────────────────────
Client          PayeeController         PayeeService
  │── DELETE /api/payees/abc ─-->│
  │                              │── deletePayee("abc")─>│── remove("abc") → Payee (non-null)
  │<── 204 No Content ───────────│<── true ──────────────│

DELETE /api/payees/{id}  ── NOT found
──────────────────────────────────────
Client          PayeeController         PayeeService
  │── DELETE /api/payees/xyz ─-->│
  │                              │── deletePayee("xyz")─>│── remove("xyz") → null
  │<── 404 Not Found ────────────│<── false ─────────────│
```

---

## 5. Files Changed Summary

| File | Change Type | Description |
|---|---|---|
| `service/PayeeService.java` | **Modify** | Add `deletePayee(String id): boolean` |
| `controller/PayeeController.java` | **Modify** | Add `getPayees()` (GET) and `deletePayee(id)` (DELETE) |
| `controller/PayeeControllerTest.java` | **Modify** | Add 3 new test methods for the 2 new endpoints |
| `service/PayeeServiceTest.java` | **Create** | New unit test class for `PayeeService` (4 tests) |

---

## 6. No New DTOs Required

- `GET /api/payees` returns the existing `Payee` model directly as a JSON array.
- `DELETE /api/payees/{id}` returns an empty body (`ResponseEntity<Void>`).

---

## 7. Risks & Notes

- **In-memory store:** `ConcurrentHashMap` — all data lost on restart. Acceptable for demo scope.
- **MFA on delete (assumption):** Delete does NOT require MFA re-verification. Confirm with PO.
- **Thread safety:** `ConcurrentHashMap.remove()` is atomic — the implementation is thread-safe.
- **No pagination:** `getPayees()` returns all records. Add pagination if store grows large.
