package com.bank.payee.service;

import com.bank.payee.model.Payee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayeeServiceTest {

    private PayeeService payeeService;

    @BeforeEach
    void setUp() {
        payeeService = new PayeeService();
    }

    // -------------------------------------------------------------------------
    // addPayee
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should assign a non-null id and addedAt timestamp when payee is added")
    void test_addPayee_populatesIdAndTimestamp() {
        // Arrange
        Payee payee = new Payee("Alice", "ACC001", "BNKA");

        // Act
        Payee result = payeeService.addPayee(payee);

        // Assert
        assertNotNull(result.getId());
        assertNotNull(result.getAddedAt());
        assertEquals("Alice", result.getName());
    }

    // -------------------------------------------------------------------------
    // getPayees
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return empty list when no payees have been added")
    void test_getPayees_emptyStore_returnsEmptyList() {
        // Act
        List<Payee> result = payeeService.getPayees();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return all payees after multiple adds")
    void test_getPayees_afterMultipleAdds_returnsAllPayees() {
        // Arrange
        payeeService.addPayee(new Payee("Alice", "ACC001", "BNKA"));
        payeeService.addPayee(new Payee("Bob", "ACC002", "BNKB"));

        // Act
        List<Payee> result = payeeService.getPayees();

        // Assert
        assertEquals(2, result.size());
    }

    // -------------------------------------------------------------------------
    // deletePayee
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true and remove the payee when the id exists")
    void test_deletePayee_existingId_returnsTrueAndRemovesPayee() {
        // Arrange
        Payee added = payeeService.addPayee(new Payee("Alice", "ACC001", "BNKA"));
        String id = added.getId();

        // Act
        boolean result = payeeService.deletePayee(id);

        // Assert
        assertTrue(result);
        assertTrue(payeeService.getPayees().isEmpty());
    }

    @Test
    @DisplayName("Should return false when the id does not exist")
    void test_deletePayee_nonExistingId_returnsFalse() {
        // Act
        boolean result = payeeService.deletePayee("non-existent-id");

        // Assert
        assertFalse(result);
    }
}
