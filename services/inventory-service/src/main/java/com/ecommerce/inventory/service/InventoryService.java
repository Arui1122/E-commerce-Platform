package com.ecommerce.inventory.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;
    
    private static final String LOCK_PREFIX = "inventory:lock:";
    private static final int LOCK_WAIT_TIME = 10;
    private static final int LOCK_LEASE_TIME = 30;

    public InventoryService(InventoryRepository inventoryRepository, RedissonClient redissonClient) {
        this.inventoryRepository = inventoryRepository;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElse(new Inventory());
        
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        return convertToResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        
        return convertToResponse(inventory);
    }

    @Transactional(readOnly = true)
    public boolean checkStock(Long productId, Integer quantity) {
        return inventoryRepository.hasEnoughStock(productId, quantity);
    }

    @Transactional
    public boolean reserveStock(StockReservationRequest request) {
        String lockKey = LOCK_PREFIX + request.getProductId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!lockAcquired) {
                return false;
            }
            
            return reserveStockWithLock(request);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean reserveStockWithLock(StockReservationRequest request) {
        try {
            Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                    .orElseThrow(() -> new InventoryNotFoundException(request.getProductId()));
            
            if (!inventory.hasAvailableStock(request.getQuantity())) {
                return false;
            }
            
            inventory.reserveStock(request.getQuantity());
            inventoryRepository.save(inventory);
            return true;
            
        } catch (OptimisticLockingFailureException e) {
            return false;
        }
    }

    @Transactional
    public void releaseReservedStock(Long productId, Integer quantity) {
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new RuntimeException("Failed to acquire lock for releasing stock");
            }
            
            Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new InventoryNotFoundException(productId));
            
            inventory.releaseReservedStock(quantity);
            inventoryRepository.save(inventory);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while releasing stock");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void confirmReservedStock(Long productId, Integer quantity) {
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean lockAcquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new RuntimeException("Failed to acquire lock for confirming stock");
            }
            
            Inventory inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new InventoryNotFoundException(productId));
            
            inventory.confirmReservedStock(quantity);
            inventoryRepository.save(inventory);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while confirming stock");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public InventoryResponse replenishStock(Long productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        
        inventory.addStock(quantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        return convertToResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockProducts(Integer minQuantity) {
        List<Inventory> lowStockInventories = inventoryRepository.findLowStockProducts(minQuantity);
        
        return lowStockInventories.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoriesByProductIds(List<Long> productIds) {
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        
        return inventories.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private InventoryResponse convertToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getVersion()
        );
    }
}
