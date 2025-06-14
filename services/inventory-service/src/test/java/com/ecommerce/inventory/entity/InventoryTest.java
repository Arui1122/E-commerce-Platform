package com.ecommerce.inventory.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(1L);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(20);
        inventory.setVersion(1);
    }

    @Test
    void getAvailableQuantity_ShouldReturnCorrectValue() {
        // When
        Integer availableQuantity = inventory.getAvailableQuantity();

        // Then
        assertEquals(80, availableQuantity);
    }

    @Test
    void hasAvailableStock_SufficientStock_ShouldReturnTrue() {
        // When
        boolean result = inventory.hasAvailableStock(50);

        // Then
        assertTrue(result);
    }

    @Test
    void hasAvailableStock_InsufficientStock_ShouldReturnFalse() {
        // When
        boolean result = inventory.hasAvailableStock(100);

        // Then
        assertFalse(result);
    }

    @Test
    void hasAvailableStock_ExactMatch_ShouldReturnTrue() {
        // When
        boolean result = inventory.hasAvailableStock(80);

        // Then
        assertTrue(result);
    }

    @Test
    void reserveStock_SufficientStock_ShouldReserveSuccessfully() {
        // Given
        int initialReserved = inventory.getReservedQuantity();

        // When
        inventory.reserveStock(30);

        // Then
        assertEquals(initialReserved + 30, inventory.getReservedQuantity());
        assertEquals(50, inventory.getAvailableQuantity());
    }

    @Test
    void reserveStock_InsufficientStock_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventory.reserveStock(100)
        );
        
        assertEquals("Insufficient stock available", exception.getMessage());
    }

    @Test
    void releaseReservedStock_ValidAmount_ShouldReleaseSuccessfully() {
        // Given
        int initialReserved = inventory.getReservedQuantity();

        // When
        inventory.releaseReservedStock(10);

        // Then
        assertEquals(initialReserved - 10, inventory.getReservedQuantity());
        assertEquals(90, inventory.getAvailableQuantity());
    }

    @Test
    void releaseReservedStock_ExcessiveAmount_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventory.releaseReservedStock(30)
        );
        
        assertEquals("Cannot release more than reserved quantity", exception.getMessage());
    }

    @Test
    void confirmReservedStock_ValidAmount_ShouldConfirmSuccessfully() {
        // Given
        int initialQuantity = inventory.getQuantity();
        int initialReserved = inventory.getReservedQuantity();

        // When
        inventory.confirmReservedStock(15);

        // Then
        assertEquals(initialQuantity - 15, inventory.getQuantity());
        assertEquals(initialReserved - 15, inventory.getReservedQuantity());
        assertEquals(80, inventory.getAvailableQuantity());
    }

    @Test
    void confirmReservedStock_ExcessiveAmount_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventory.confirmReservedStock(25)
        );
        
        assertEquals("Cannot confirm more than reserved quantity", exception.getMessage());
    }

    @Test
    void addStock_ShouldIncreaseQuantity() {
        // Given
        int initialQuantity = inventory.getQuantity();

        // When
        inventory.addStock(50);

        // Then
        assertEquals(initialQuantity + 50, inventory.getQuantity());
        assertEquals(130, inventory.getAvailableQuantity());
    }

    @Test
    void businessLogicFlow_CompleteReservationAndConfirmation() {
        // Given
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);

        // Reserve stock
        inventory.reserveStock(30);
        assertEquals(30, inventory.getReservedQuantity());
        assertEquals(70, inventory.getAvailableQuantity());

        // Reserve more stock
        inventory.reserveStock(20);
        assertEquals(50, inventory.getReservedQuantity());
        assertEquals(50, inventory.getAvailableQuantity());

        // Confirm part of reserved stock
        inventory.confirmReservedStock(30);
        assertEquals(20, inventory.getReservedQuantity());
        assertEquals(70, inventory.getQuantity());
        assertEquals(50, inventory.getAvailableQuantity());

        // Release remaining reserved stock
        inventory.releaseReservedStock(20);
        assertEquals(0, inventory.getReservedQuantity());
        assertEquals(70, inventory.getAvailableQuantity());
    }

    @Test
    void businessLogicFlow_PartialCancellation() {
        // Given - Customer reserves 50 items but only buys 30
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);

        // Reserve stock for order
        inventory.reserveStock(50);
        assertEquals(50, inventory.getReservedQuantity());
        assertEquals(50, inventory.getAvailableQuantity());

        // Customer reduces order quantity - confirm 30, release 20
        inventory.confirmReservedStock(30);
        assertEquals(20, inventory.getReservedQuantity());
        assertEquals(70, inventory.getQuantity());

        inventory.releaseReservedStock(20);
        assertEquals(0, inventory.getReservedQuantity());
        assertEquals(70, inventory.getAvailableQuantity());
    }
}
