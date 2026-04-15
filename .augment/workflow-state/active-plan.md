# SCRUM-1 — Technical Implementation Plan
## Inferred Story: View & Delete Payees

> **Jira access was blocked by authentication.**
> Requirements inferred from codebase analysis of `backend/src/main/java/com/bank/payee/`.

---

## 1. Current State (Post-Analysis)

The application already implements the full MFA-protected **Add Payee** flow:

| Layer | Status |
|---|---|
| Models (`Payee`, `MfaSession`, `MfaMethod`) | ✅ Complete |
| DTOs (all 4 DTO classes) | ✅ Complete |
| `MfaService` + `OtpService` + `VerifyResult` | ✅ Complete |
| `PayeeService.addPayee()` + `getPayees()` | ✅ Complete |
| `POST /api/payees/initiate-mfa` | ✅ Complete |
| `POST /api/payees/verify-otp` | ✅ Complete |
| `MfaServiceTest` | ✅ Complete (8 tests) |
| `PayeeControllerTest` | ✅ Complete (5 tests) |

**Gaps identified — what SCRUM-1 still requires:**

| Feature | Status |
|---|---|
| `GET /api/payees` endpoint | ⚠️ Service method exists; **controller endpoint MISSING** |
| `DELETE /api/payees/{id}` endpoint | ❌ No service method AND no controller endpoint |
| `PayeeServiceTest` | ❌ No unit tests for `PayeeService` at all |

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
