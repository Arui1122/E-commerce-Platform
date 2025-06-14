package com.ecommerce.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.inventory.entity.Inventory;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 根據商品ID查找庫存，使用樂觀鎖
     */
    Optional<Inventory> findByProductId(Long productId);

    /**
     * 根據商品ID查找庫存，使用悲觀鎖
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * 查找庫存不足的商品（可用庫存 < 指定數量）
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) < :minQuantity")
    List<Inventory> findLowStockProducts(@Param("minQuantity") Integer minQuantity);

    /**
     * 查找有庫存的商品列表
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) > 0")
    List<Inventory> findInStockProducts();

    /**
     * 批量查找商品庫存
     */
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds")
    List<Inventory> findByProductIdIn(@Param("productIds") List<Long> productIds);

    /**
     * 檢查商品是否有足夠庫存
     */
    @Query("SELECT CASE WHEN (i.quantity - i.reservedQuantity) >= :requestedQuantity THEN true ELSE false END " +
           "FROM Inventory i WHERE i.productId = :productId")
    Boolean hasEnoughStock(@Param("productId") Long productId, @Param("requestedQuantity") Integer requestedQuantity);
}
