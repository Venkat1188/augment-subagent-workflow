# JUnit 5 Test Specifications — SCRUM-1 (v2 — Dapr-Native)

## Skill Applied: `java-spring-boot-dapr` — DaprClient mocked via Mockito

## Overview

| Test Class | New/Modified | # Tests |
|---|---|---|
| `MfaServiceTest` | ✅ No changes | 7 existing |
| `PayeeControllerTest` | **Modify** — Mono-aware assertions | 10 updated |
| `PayeeControllerSecurityTest` | **Modify** — Mono-aware | 4 updated |
| `PayeeServiceTest` | **Rewrite** — mock DaprClient | 5 Dapr tests |

---

## A. `PayeeServiceTest` — REWRITE with DaprClient mock

**File:** `backend/src/test/java/com/bank/payee/service/PayeeServiceTest.java`

```java
package com.bank.payee.service;

import com.bank.payee.model.Payee;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayeeServiceTest {

    @Mock
    private DaprClient daprClient;

    @InjectMocks
    private PayeeService payeeService;

    @BeforeEach
    void setUp() {
        payeeService = new PayeeService();
    }

    // ------------------------------------------------------------------
    // addPayee — saves to Dapr state store AND publishes event
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save state and publish event when payee is added")
    void test_addPayee_savesStateAndPublishesEvent() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        when(daprClient.saveState(eq(PayeeService.STORE), anyString(), any(Payee.class)))
            .thenReturn(Mono.empty());
        when(daprClient.publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any()))
            .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(payeeService.addPayee(payee))
            .assertNext(saved -> {
                assertNotNull(saved.getId());
                assertNotNull(saved.getAddedAt());
                assertEquals("Alice", saved.getName());
            })
            .verifyComplete();

        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), anyString(), any(Payee.class));
        verify(daprClient, times(1)).publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any());
    }

    // ------------------------------------------------------------------
    // getPayees — reads from Dapr state store
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return empty list when state store has no payees")
    void test_getPayees_emptyStore_returnsEmptyList() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq("payee-index"), eq(java.util.List.class)))
            .thenReturn(Mono.just(new State<>("payee-index", null, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.getPayees())
            .assertNext(list -> assertTrue(list.isEmpty()))
            .verifyComplete();
    }

    // ------------------------------------------------------------------
    // deletePayee — existing id
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return true and delete state when payee id exists")
    void test_deletePayee_existingId_returnsTrueAndDeletesState() {
        // Arrange
        Payee existing = new Payee("Alice", "ACC001", "BNKA");
        when(daprClient.getState(eq(PayeeService.STORE), eq("abc"), eq(Payee.class)))
            .thenReturn(Mono.just(new State<>("abc", existing, null, null, null)));
        when(daprClient.deleteState(eq(PayeeService.STORE), eq("abc")))
            .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("abc"))
            .assertNext(result -> assertTrue(result))
            .verifyComplete();

        verify(daprClient, times(1)).deleteState(PayeeService.STORE, "abc");
    }

    // ------------------------------------------------------------------
    // deletePayee — non-existing id
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when payee id does not exist in state store")
    void test_deletePayee_nonExistingId_returnsFalse() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq("xyz"), eq(Payee.class)))
            .thenReturn(Mono.just(new State<>("xyz", null, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("xyz"))
            .assertNext(result -> assertFalse(result))
            .verifyComplete();

        verify(daprClient, never()).deleteState(any(), any());
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
