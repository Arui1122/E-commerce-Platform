package com.ecommerce.order.client;

import com.ecommerce.order.client.fallback.InventoryClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Inventory Service Feign Client
 * 用於調用庫存服務的 API
 */
@FeignClient(
    name = "inventory-service",
    path = "/api/v1/inventory",
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {

    /**
     * 獲取商品庫存信息
     */
    @GetMapping("/{productId}")
    ResponseEntity<Map<String, Object>> getInventory(@PathVariable("productId") Long productId);

    /**
     * 檢查庫存是否充足
     */
    @GetMapping("/check/{productId}")
    ResponseEntity<Map<String, Object>> checkStock(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 預留庫存
     */
    @PostMapping("/reserve")
    ResponseEntity<Map<String, Object>> reserveStock(@RequestBody Map<String, Object> reservationRequest);

    /**
     * 確認預留庫存
     */
    @PostMapping("/{productId}/confirm")
    ResponseEntity<Map<String, String>> confirmReservedStock(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 釋放預留庫存
     */
    @PostMapping("/{productId}/release")
    ResponseEntity<Map<String, String>> releaseReservedStock(
            @PathVariable("productId") Long productId,
            @RequestParam("quantity") Integer quantity
    );

    /**
     * 批量查詢庫存
     */
    @PostMapping("/batch")
    ResponseEntity<List<Map<String, Object>>> getBatchInventory(@RequestBody List<Long> productIds);
}
