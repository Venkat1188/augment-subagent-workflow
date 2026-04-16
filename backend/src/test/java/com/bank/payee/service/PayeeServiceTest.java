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
        StepVerifier.create(payeeService.addPayee(payee))
                .assertNext(saved -> {
                    assertNotNull(saved.getId(), "id must be populated");
                    assertNotNull(saved.getAddedAt(), "addedAt must be populated");
                    assertEquals("Alice", saved.getName());
                    assertEquals("ACC001", saved.getAccountNumber());
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
        StepVerifier.create(payeeService.addPayee(payee))
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
        StepVerifier.create(payeeService.addPayee(payee))
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

        // Act & Assert
        StepVerifier.create(payeeService.getPayees())
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return all payees when payee-index is populated")
    @SuppressWarnings("unchecked")
    void test_getPayees_withPayees_returnsPopulatedList() {
        // Arrange
        List<Payee> stored = new ArrayList<>();
        stored.add(new Payee("Alice", "ACC001", "BNKA"));
        stored.add(new Payee("Bob",   "ACC002", "BNKB"));
        when(daprClient.getState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), eq(List.class)))
                .thenReturn(Mono.just(new State<>(PayeeService.INDEX_KEY, stored, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.getPayees())
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
        StepVerifier.create(payeeService.deletePayee("abc-123"))
                .assertNext(result -> assertTrue(result, "Should return true for existing payee"))
                .verifyComplete();

        verify(daprClient, times(1)).deleteState(PayeeService.STORE, "abc-123");
        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), eq(PayeeService.INDEX_KEY), any(), any(), any());
    }

    @Test
    @DisplayName("Should return false and never call deleteState when payee id not found")
    void test_deletePayee_nonExistingId_returnsFalse() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq("xyz-000"), eq(Payee.class)))
                .thenReturn(Mono.just(new State<>("xyz-000", null, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("xyz-000"))
                .assertNext(result -> assertFalse(result, "Should return false for non-existent payee"))
                .verifyComplete();

        verify(daprClient, never()).deleteState(any(), any());
    }
}
