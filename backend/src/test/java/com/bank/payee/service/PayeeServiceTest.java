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
                    assertNotNull(saved.getId(), "id must be populated");
                    assertNotNull(saved.getAddedAt(), "addedAt must be populated");
                    assertEquals("Alice", saved.getName());
                    assertEquals("ACC001", saved.getAccountNumber());
                })
                .verifyComplete();

        verify(daprClient, times(1)).saveState(eq(PayeeService.STORE), anyString(), any(Payee.class));
        verify(daprClient, times(1)).publishEvent(eq(PayeeService.PUB_SUB), eq(PayeeService.TOPIC), any());
    }

    // -------------------------------------------------------------------------
    // getPayees — reads "payee-index" from Dapr state store
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return empty list when state store has no payee-index")
    @SuppressWarnings("unchecked")
    void test_getPayees_emptyStore_returnsEmptyList() {
        // Arrange
        when(daprClient.getState(eq(PayeeService.STORE), eq("payee-index"), eq(List.class)))
                .thenReturn(Mono.just(new State<>("payee-index", null, null, null, null)));

        // Act & Assert
        StepVerifier.create(payeeService.getPayees())
                .assertNext(list -> assertTrue(list.isEmpty()))
                .verifyComplete();
    }

    // -------------------------------------------------------------------------
    // deletePayee — checks state then deletes if present
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true and delete state when payee id exists")
    void test_deletePayee_existingId_returnsTrueAndDeletesState() {
        // Arrange
        Payee existing = new Payee("Alice", "ACC001", "BNKA");
        when(daprClient.getState(eq(PayeeService.STORE), eq("abc-123"), eq(Payee.class)))
                .thenReturn(Mono.just(new State<>("abc-123", existing, null, null, null)));
        when(daprClient.deleteState(eq(PayeeService.STORE), eq("abc-123")))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(payeeService.deletePayee("abc-123"))
                .assertNext(result -> assertTrue(result, "Should return true for existing payee"))
                .verifyComplete();

        verify(daprClient, times(1)).deleteState(PayeeService.STORE, "abc-123");
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
