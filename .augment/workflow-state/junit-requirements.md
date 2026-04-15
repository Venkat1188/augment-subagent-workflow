# JUnit 5 Test Specifications — SCRUM-1

## Overview

| Test Class | New/Modified | # Tests |
|---|---|---|
| `MfaServiceTest` | ✅ Already exists & passes | — (no changes) |
| `PayeeControllerTest` | **Modify** — add 3 new tests | +3 |
| `PayeeServiceTest` | **Create new** | 4 |

---

## A. `PayeeServiceTest` ← NEW CLASS

**File:** `backend/src/test/java/com/bank/payee/service/PayeeServiceTest.java`

```java
package com.bank.payee.service;

import com.bank.payee.model.Payee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayeeServiceTest {

    // PayeeService has no dependencies to mock — instantiate directly
    private PayeeService payeeService;

    @BeforeEach
    void setUp() {
        payeeService = new PayeeService();
    }

    // ------------------------------------------------------------------
    // addPayee
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should assign UUID id and addedAt timestamp when payee is added")
    void test_addPayee_populatesIdAndTimestamp() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");

        // Act
        Payee saved = payeeService.addPayee(payee);

        // Assert
        assertNotNull(saved.getId(),      "id must be populated");
        assertNotNull(saved.getAddedAt(), "addedAt must be populated");
        assertEquals("Alice",  saved.getName());
        assertEquals("ACC001", saved.getAccountNumber());
        assertEquals("BNKA",   saved.getBankCode());
    }

    // ------------------------------------------------------------------
    // getPayees
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return empty list when no payees have been added")
    void test_getPayees_emptyStore_returnsEmptyList() {
        // Act
        List<Payee> result = payeeService.getPayees();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return all stored payees after multiple adds")
    void test_getPayees_afterMultipleAdds_returnsAllPayees() {
        // Arrange
        payeeService.addPayee(new Payee("Alice", "ACC001", "BNKA"));
        payeeService.addPayee(new Payee("Bob",   "ACC002", "BNKB"));

        // Act
        List<Payee> result = payeeService.getPayees();

        // Assert
        assertEquals(2, result.size());
    }

    // ------------------------------------------------------------------
    // deletePayee
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return true and remove payee when id exists")
    void test_deletePayee_existingId_returnsTrueAndRemovesPayee() {
        // Arrange
        Payee saved = payeeService.addPayee(new Payee("Alice", "ACC001", "BNKA"));
        String id = saved.getId();

        // Act
        boolean result = payeeService.deletePayee(id);

        // Assert
        assertTrue(result, "deletePayee should return true for a known id");
        assertTrue(payeeService.getPayees().isEmpty(), "store must be empty after deletion");
    }

    @Test
    @DisplayName("Should return false when id does not exist")
    void test_deletePayee_nonExistingId_returnsFalse() {
        // Act
        boolean result = payeeService.deletePayee("non-existent-uuid");

        // Assert
        assertFalse(result, "deletePayee should return false for an unknown id");
    }
}
```

---

## B. `PayeeControllerTest` — ADD 3 NEW TEST METHODS

**File:** `backend/src/test/java/com/bank/payee/controller/PayeeControllerTest.java`

Add the following imports (if not already present):
```java
import java.util.List;
import static org.mockito.Mockito.when;
```

Append these three test methods inside the existing `PayeeControllerTest` class:

```java
    // -----------------------------------------------------------------------
    // GET /api/payees
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return 200 with list of payees when payees exist")
    void test_getPayees_returnsAllPayees() {
        // Arrange
        Payee alice = new Payee("Alice", "ACC001", "BNKA");
        Payee bob   = new Payee("Bob",   "ACC002", "BNKB");
        when(payeeService.getPayees()).thenReturn(List.of(alice, bob));

        // Act
        ResponseEntity<List<Payee>> response = payeeController.getPayees();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(payeeService, times(1)).getPayees();
    }

    // -----------------------------------------------------------------------
    // DELETE /api/payees/{id} — found
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return 204 No Content when payee is successfully deleted")
    void test_deletePayee_existingId_returns204() {
        // Arrange
        when(payeeService.deletePayee("payee-uuid-123")).thenReturn(true);

        // Act
        ResponseEntity<Void> response = payeeController.deletePayee("payee-uuid-123");

        // Assert
        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(payeeService, times(1)).deletePayee("payee-uuid-123");
    }

    // -----------------------------------------------------------------------
    // DELETE /api/payees/{id} — not found
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return 404 Not Found when payee id does not exist")
    void test_deletePayee_nonExistingId_returns404() {
        // Arrange
        when(payeeService.deletePayee("unknown-id")).thenReturn(false);

        // Act
        ResponseEntity<Void> response = payeeController.deletePayee("unknown-id");

        // Assert
        assertEquals(404, response.getStatusCode().value());
        verify(payeeService, times(1)).deletePayee("unknown-id");
    }
```

---

## C. `MfaServiceTest` — No Changes Required

The existing `MfaServiceTest` is complete and covers all 6 scenarios:

| Test Method | Scenario |
|---|---|
| `test_initiateMfa_sms_createsSessionAndSendsSmsOtp` | SMS OTP dispatched |
| `test_initiateMfa_totp_createsSessionAndSendsTotpCode` | TOTP code dispatched |
| `test_verifyOtp_correctOtp_returnsSuccess` | OTP matches → SUCCESS |
| `test_verifyOtp_wrongOtp_decrementsRemainingAttempts` | Wrong OTP → WRONG_OTP |
| `test_verifyOtp_maxFailedAttempts_locksSession` | 3 failures → LOCKED |
| `test_verifyOtp_alreadyLockedSession_returnsLocked` | Already locked → LOCKED |
| `test_verifyOtp_unknownSession_returnsNotFound` | No session → NOT_FOUND |

---

## D. Mock & Assertion Reference

### `PayeeServiceTest` — no mocks needed
`PayeeService` uses an in-memory `ConcurrentHashMap` with no collaborators.
Use a plain `new PayeeService()` instance, reset in `@BeforeEach`.

### `PayeeControllerTest` — existing mocks sufficient

| Mock | Used in new tests |
|---|---|
| `@Mock PayeeService payeeService` | `getPayees()`, `deletePayee(id)` |
| `@Mock MfaService mfaService` | Not used in new tests |

### Key assertions for new tests

| Scenario | Assert |
|---|---|
| GET payees (list) | `assertEquals(200, ...)`, `assertEquals(2, body.size())` |
| DELETE found | `assertEquals(204, ...)`, `assertNull(body)` |
| DELETE not found | `assertEquals(404, ...)` |
| addPayee | `assertNotNull(id)`, `assertNotNull(addedAt)` |
| deletePayee found | `assertTrue(result)`, `assertTrue(getPayees().isEmpty())` |
| deletePayee not found | `assertFalse(result)` |
