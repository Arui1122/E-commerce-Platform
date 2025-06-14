package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;
    private InventoryRequest testRequest;
    private StockReservationRequest testReservationRequest;

    @BeforeEach
    void setUp() {
        testInventory = new Inventory();
        testInventory.setId(1L);
        testInventory.setProductId(1L);
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(10);
        testInventory.setVersion(1);

        testRequest = new InventoryRequest();
        testRequest.setProductId(1L);
        testRequest.setQuantity(100);

        testReservationRequest = new StockReservationRequest();
        testReservationRequest.setProductId(1L);
        testReservationRequest.setQuantity(20);
        testReservationRequest.setReferenceId("ORDER-123");
    }

    @Test
    void createOrUpdateInventory_NewInventory_ShouldCreateSuccessfully() {
        // Given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        InventoryResponse response = inventoryService.createOrUpdateInventory(testRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals(100, response.getQuantity());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createOrUpdateInventory_ExistingInventory_ShouldUpdateSuccessfully() {
        // Given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        InventoryResponse response = inventoryService.createOrUpdateInventory(testRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void getInventoryByProductId_ExistingProduct_ShouldReturnInventory() {
        // Given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        // When
        InventoryResponse response = inventoryService.getInventoryByProductId(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals(100, response.getQuantity());
        assertEquals(10, response.getReservedQuantity());
        assertEquals(90, response.getAvailableQuantity());
    }

    @Test
    void getInventoryByProductId_NonExistingProduct_ShouldThrowException() {
        // Given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(InventoryNotFoundException.class, 
                () -> inventoryService.getInventoryByProductId(1L));
    }

    @Test
    void checkStock_SufficientStock_ShouldReturnTrue() {
        // Given
        when(inventoryRepository.hasEnoughStock(1L, 50)).thenReturn(true);

        // When
        boolean result = inventoryService.checkStock(1L, 50);

        // Then
        assertTrue(result);
    }

    @Test
    void checkStock_InsufficientStock_ShouldReturnFalse() {
        // Given
        when(inventoryRepository.hasEnoughStock(1L, 150)).thenReturn(false);

        // When
        boolean result = inventoryService.checkStock(1L, 150);

        // Then
        assertFalse(result);
    }

    @Test
    void reserveStock_SufficientStock_ShouldReserveSuccessfully() throws InterruptedException {
        // Given
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        boolean result = inventoryService.reserveStock(testReservationRequest);

        // Then
        assertTrue(result);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(rLock).unlock();
    }

    @Test
    void reserveStock_InsufficientStock_ShouldReturnFalse() throws InterruptedException {
        // Given
        testInventory.setQuantity(10);
        testInventory.setReservedQuantity(5);
        testReservationRequest.setQuantity(10); // Request more than available

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        // When
        boolean result = inventoryService.reserveStock(testReservationRequest);

        // Then
        assertFalse(result);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void reserveStock_LockNotAcquired_ShouldReturnFalse() throws InterruptedException {
        // Given
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        // When
        boolean result = inventoryService.reserveStock(testReservationRequest);

        // Then
        assertFalse(result);
        verify(inventoryRepository, never()).findByProductId(anyLong());
    }

    @Test
    void releaseReservedStock_ValidRequest_ShouldReleaseSuccessfully() throws InterruptedException {
        // Given
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        assertDoesNotThrow(() -> inventoryService.releaseReservedStock(1L, 5));

        // Then
        verify(inventoryRepository).save(any(Inventory.class));
        verify(rLock).unlock();
    }

    @Test
    void confirmReservedStock_ValidRequest_ShouldConfirmSuccessfully() throws InterruptedException {
        // Given
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        assertDoesNotThrow(() -> inventoryService.confirmReservedStock(1L, 5));

        // Then
        verify(inventoryRepository).save(any(Inventory.class));
        verify(rLock).unlock();
    }

    @Test
    void replenishStock_ValidRequest_ShouldReplenishSuccessfully() {
        // Given
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        InventoryResponse response = inventoryService.replenishStock(1L, 50);

        // Then
        assertNotNull(response);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void getLowStockProducts_ShouldReturnLowStockList() {
        // Given
        List<Inventory> lowStockInventories = Arrays.asList(testInventory);
        when(inventoryRepository.findLowStockProducts(10)).thenReturn(lowStockInventories);

        // When
        List<InventoryResponse> responses = inventoryService.getLowStockProducts(10);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getProductId());
    }

    @Test
    void getInventoriesByProductIds_ShouldReturnInventoryList() {
        // Given
        List<Long> productIds = Arrays.asList(1L, 2L);
        List<Inventory> inventories = Arrays.asList(testInventory);
        when(inventoryRepository.findByProductIdIn(productIds)).thenReturn(inventories);

        // When
        List<InventoryResponse> responses = inventoryService.getInventoriesByProductIds(productIds);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getProductId());
    }
}
