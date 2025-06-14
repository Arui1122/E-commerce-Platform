package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InventoryConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Long testProductId;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        inventoryRepository.deleteAll();
        
        // Create test inventory with 100 items
        testProductId = 1L;
        Inventory testInventory = new Inventory();
        testInventory.setProductId(testProductId);
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(0);
        inventoryRepository.save(testInventory);
    }

    @Test
    void concurrentStockReservation_ShouldPreventOverselling() throws InterruptedException {
        // Given
        int numberOfThreads = 20;
        int quantityPerRequest = 10;
        int totalRequestedQuantity = numberOfThreads * quantityPerRequest; // 200 > 100 available

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        // When - Multiple threads try to reserve stock simultaneously
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    StockReservationRequest request = new StockReservationRequest();
                    request.setProductId(testProductId);
                    request.setQuantity(quantityPerRequest);
                    request.setReferenceId("ORDER-" + threadId);
                    
                    boolean success = inventoryService.reserveStock(request);
                    
                    if (success) {
                        successfulReservations.incrementAndGet();
                    } else {
                        failedReservations.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failedReservations.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All threads should complete within timeout");
        
        // Verify that no overselling occurred
        Inventory finalInventory = inventoryRepository.findByProductId(testProductId).orElseThrow();
        int expectedSuccessfulReservations = 100 / quantityPerRequest; // 10 successful reservations
        int expectedFailedReservations = numberOfThreads - expectedSuccessfulReservations; // 10 failed

        assertEquals(expectedSuccessfulReservations, successfulReservations.get(), 
                "Should have exactly " + expectedSuccessfulReservations + " successful reservations");
        assertEquals(expectedFailedReservations, failedReservations.get(), 
                "Should have " + expectedFailedReservations + " failed reservations");
        assertEquals(100, finalInventory.getQuantity(), "Total quantity should remain 100");
        assertEquals(100, finalInventory.getReservedQuantity(), "Should have reserved exactly 100 items");
        assertEquals(0, finalInventory.getAvailableQuantity(), "No available stock should remain");
    }

    @Test
    void concurrentMixedOperations_ShouldMaintainConsistency() throws InterruptedException {
        // Given
        int numberOfOperations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch completionLatch = new CountDownLatch(numberOfOperations);
        
        List<Future<String>> futures = new ArrayList<>();

        // When - Mix of reserve, release, and confirm operations
        for (int i = 0; i < numberOfOperations; i++) {
            final int operationId = i;
            Future<String> future = executor.submit(() -> {
                try {
                    String operation;
                    switch (operationId % 3) {
                        case 0: // Reserve operation
                            StockReservationRequest reserveRequest = new StockReservationRequest();
                            reserveRequest.setProductId(testProductId);
                            reserveRequest.setQuantity(5);
                            reserveRequest.setReferenceId("OP-" + operationId);
                            
                            boolean reserved = inventoryService.reserveStock(reserveRequest);
                            operation = reserved ? "RESERVE_SUCCESS" : "RESERVE_FAILED";
                            break;
                            
                        case 1: // Release operation (only if we have reserved stock)
                            try {
                                inventoryService.releaseReservedStock(testProductId, 3);
                                operation = "RELEASE_SUCCESS";
                            } catch (Exception e) {
                                operation = "RELEASE_FAILED";
                            }
                            break;
                            
                        case 2: // Confirm operation (only if we have reserved stock)
                            try {
                                inventoryService.confirmReservedStock(testProductId, 2);
                                operation = "CONFIRM_SUCCESS";
                            } catch (Exception e) {
                                operation = "CONFIRM_FAILED";
                            }
                            break;
                            
                        default:
                            operation = "UNKNOWN";
                    }
                    return operation;
                } finally {
                    completionLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for all operations to complete
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All operations should complete within timeout");
        
        // Verify data consistency
        Inventory finalInventory = inventoryRepository.findByProductId(testProductId).orElseThrow();
        
        // The inventory should still be in a valid state
        assertTrue(finalInventory.getQuantity() >= 0, "Quantity should not be negative");
        assertTrue(finalInventory.getReservedQuantity() >= 0, "Reserved quantity should not be negative");
        assertTrue(finalInventory.getReservedQuantity() <= finalInventory.getQuantity(), 
                "Reserved quantity should not exceed total quantity");
        assertTrue(finalInventory.getAvailableQuantity() >= 0, "Available quantity should not be negative");
        
        // Count operation results
        long reserveSuccessCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return "ERROR";
                    }
                })
                .filter(result -> "RESERVE_SUCCESS".equals(result))
                .count();
        
        System.out.println("Final inventory state:");
        System.out.println("Quantity: " + finalInventory.getQuantity());
        System.out.println("Reserved: " + finalInventory.getReservedQuantity());
        System.out.println("Available: " + finalInventory.getAvailableQuantity());
        System.out.println("Successful reservations: " + reserveSuccessCount);
    }

    @Test
    void highConcurrencyStressTest_ShouldMaintainDataIntegrity() throws InterruptedException {
        // Given
        int numberOfThreads = 100;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);

        // When - High load concurrent operations
        for (int t = 0; t < numberOfThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < operationsPerThread; op++) {
                        totalOperations.incrementAndGet();
                        
                        try {
                            // Randomly choose operation type
                            int operationType = (threadId + op) % 4;
                            boolean success = false;
                            
                            switch (operationType) {
                                case 0: // Reserve small amount
                                    StockReservationRequest request = new StockReservationRequest();
                                    request.setProductId(testProductId);
                                    request.setQuantity(1);
                                    request.setReferenceId("STRESS-" + threadId + "-" + op);
                                    success = inventoryService.reserveStock(request);
                                    break;
                                    
                                case 1: // Release small amount
                                    try {
                                        inventoryService.releaseReservedStock(testProductId, 1);
                                        success = true;
                                    } catch (Exception e) {
                                        success = false;
                                    }
                                    break;
                                    
                                case 2: // Confirm small amount
                                    try {
                                        inventoryService.confirmReservedStock(testProductId, 1);
                                        success = true;
                                    } catch (Exception e) {
                                        success = false;
                                    }
                                    break;
                                    
                                case 3: // Check stock
                                    success = inventoryService.checkStock(testProductId, 1);
                                    break;
                            }
                            
                            if (success) {
                                successfulOperations.incrementAndGet();
                            }
                            
                        } catch (Exception e) {
                            // Expected in high concurrency scenarios
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start stress test
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "Stress test should complete within timeout");
        
        // Verify final state is valid
        Inventory finalInventory = inventoryRepository.findByProductId(testProductId).orElseThrow();
        
        assertTrue(finalInventory.getQuantity() >= 0, "Final quantity should not be negative");
        assertTrue(finalInventory.getReservedQuantity() >= 0, "Final reserved quantity should not be negative");
        assertTrue(finalInventory.getReservedQuantity() <= finalInventory.getQuantity(), 
                "Reserved should not exceed total quantity");
        
        int totalExpectedQuantity = finalInventory.getQuantity() + 
                                  (100 - finalInventory.getQuantity() - finalInventory.getReservedQuantity());
        assertTrue(totalExpectedQuantity <= 100, "Total accounted quantity should not exceed initial");
        
        System.out.println("Stress test completed:");
        System.out.println("Total operations: " + totalOperations.get());
        System.out.println("Successful operations: " + successfulOperations.get());
        System.out.println("Final state - Quantity: " + finalInventory.getQuantity() + 
                          ", Reserved: " + finalInventory.getReservedQuantity() + 
                          ", Available: " + finalInventory.getAvailableQuantity());
    }
}
