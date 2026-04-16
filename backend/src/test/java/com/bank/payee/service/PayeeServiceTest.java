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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayeeServiceTest {

    @Mock
    private DaprClient daprClient;

    @InjectMocks
    private PayeeService payeeService;

    // -------------------------------------------------------------------------
    // addPayee — saves state in Dapr AND publishes a CloudEvent
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should save state, update index, and publish event when payee is added")
    @SuppressWarnings("unchecked")
    void test_addPayee_savesStateAndPublishesEvent() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        // 3-arg saveState: used for individual payee by UUID key
        when(daprClient.saveState(eq(PayeeService.STORE), anyString(), any()))
                .thenReturn(Mono.empty());
        // 5-arg saveState (ETag-aware): used for payee-index write inside appendToIndex
        when(daprClient.saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any()))
                .thenReturn(Mono.empty());
        // getState for payee-index (appendToIndex reads it first before ETag-aware write)
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, null, null, null, null)));
        when(daprClient.publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(payeeService.addPayee(payee, "user1"))
                .assertNext(saved -> {
                    assertNotNull(saved.getId(), "id must be populated");
                    assertNotNull(saved.getAddedAt(), "addedAt must be populated");
                    assertEquals("Alice", saved.getName());
                    assertEquals("ACC001", saved.getAccountNumber());
                    assertEquals("user1", saved.getOwnerId(), "ownerId must be set from caller");
                })
                .verifyComplete();

        // Verify: 1 call per payee (3-arg), 1 ETag-aware index write (5-arg), 1 publish
        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), anyString(), any());
        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any());
        verify(daprClient, times(1)).publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any());
    }

    @Test
    @DisplayName("Should propagate error when saveState fails")
    void test_addPayee_whenSaveStateFails_propagatesError() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        when(daprClient.saveState(eq(PayeeService.STORE), anyString(), any()))
                .thenReturn(Mono.error(new RuntimeException("Dapr state store unavailable")));

        // Act & Assert
        StepVerifier.create(payeeService.addPayee(payee, "user1"))
                .expectErrorMessage("Dapr state store unavailable")
                .verify();

        verify(daprClient, never()).publishEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should propagate error when publishEvent fails after successful save")
    @SuppressWarnings("unchecked")
    void test_addPayee_whenPublishEventFails_propagatesError() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");
        // 3-arg saveState for individual payee by UUID
        when(daprClient.saveState(eq(PayeeService.STORE), anyString(), any()))
                .thenReturn(Mono.empty());
        // 5-arg ETag-aware saveState for payee-index (appendToIndex)
        when(daprClient.saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any()))
                .thenReturn(Mono.empty());
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, null, null, null, null)));
        when(daprClient.publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any()))
                .thenReturn(Mono.error(new RuntimeException("Pub/sub unavailable")));

        // Act & Assert
        StepVerifier.create(payeeService.addPayee(payee, "user1"))
                .expectErrorMessage("Pub/sub unavailable")
                .verify();
    }

    // -------------------------------------------------------------------------
    // getPayees — reads "payee-index" from Dapr state store
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return empty list when state store has no payee-index")
    @SuppressWarnings("unchecked")
    void test_getPayees_emptyStore_returnsEmptyList() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, null, null, null, null)));

        // Act & Assert — ownerId "user1" matches no entries (index is null/empty)
        StepVerifier.create(payeeService.getPayees("user1"))
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return all payees when payee-index is populated")
    @SuppressWarnings("unchecked")
    void test_getPayees_withPayees_returnsPopulatedList() {
        // Arrange
        // CWE-639: set ownerId so the getPayees filter returns them for "user1"
        List<Payee> stored = new ArrayList<>();
        Payee p1 = new Payee("Alice", "ACC001", "BNKA"); p1.setOwnerId("user1");
        Payee p2 = new Payee("Bob",   "ACC002", "BNKB"); p2.setOwnerId("user1");
        stored.add(p1);
        stored.add(p2);
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, stored, null, null, null)));

        // Act & Assert — "user1" should see their 2 payees (filter by ownerId)
        StepVerifier.create(payeeService.getPayees("user1"))
                .assertNext(list -> assertEquals(2, list.size()))
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // deletePayee — checks state then deletes if present
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true, delete state, and remove from index when payee id exists")
    @SuppressWarnings("unchecked")
    void test_deletePayee_existingId_returnsTrueAndDeletesState() {
        // Arrange
        Payee existing = new Payee("Alice", "ACC001", "BNKA");
        existing.setId("abc-123");
        existing.setOwnerId("user1");  // CWE-639: ownership check requires matching ownerId
        List<Payee> index = new ArrayList<>(List.of(existing));
        when(daprClient.getState(eq(PayeeService.STORE), eq("abc-123"), eq(Payee.class)))
                .thenReturn(Mono.just(new State<>("abc-123", existing, null, null, null)));
        when(daprClient.deleteState(eq(PayeeService.STORE), eq("abc-123")))
                .thenReturn(Mono.empty());
        // removeFromIndex: reads index then saves updated list via ETag-aware 5-arg saveState
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, index, null, null, null)));
        when(daprClient.saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("abc-123", "user1"))
                .assertNext(result -> assertTrue(result, "Should return true for existing payee owned by caller"))
                .verifyComplete();

        verify(daprClient, times(1)).deleteState(PayeeService.STORE, "abc-123");
        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any());
    }

    @Test
    @DisplayName("Should return false when payee not found (CWE-639: also returns false when not owner)")
    void test_deletePayee_nonExistingId_returnsFalse() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq("xyz-000"), eq(Payee.class)))
                .thenReturn(Mono.just(new State<>("xyz-000", null, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("xyz-000", "user1"))
                .assertNext(result -> assertFalse(result, "Should return false for non-existent payee"))
                .verifyComplete();

        verify(daprClient, never()).deleteState(any(), any());
    }
}
