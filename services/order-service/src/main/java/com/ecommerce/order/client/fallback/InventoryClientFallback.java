package com.ecommerce.order.client.fallback;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ecommerce.order.client.InventoryClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Inventory Service Fallback
 * 庫存服務的熔斷降級處理
 */
@Component
@Slf4j
public class InventoryClientFallback implements InventoryClient {

    @Override
    public ResponseEntity<Map<String, Object>> getInventory(Long productId) {
        log.warn("Inventory service is unavailable, using fallback for product: {}", productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "quantity", 0,
                "available", false,
                "message", "庫存服務暫時不可用"
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> checkStock(Long productId, Integer quantity) {
        log.warn("Inventory service is unavailable, using fallback for stock check: product={}, quantity={}", 
                productId, quantity);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "requestedQuantity", quantity,
                "available", false,
                "sufficient", false,
                "message", "庫存服務暫時不可用，無法檢查庫存"
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> reserveStock(Map<String, Object> reservationRequest) {
        log.warn("Inventory service is unavailable, using fallback for stock reservation: {}", reservationRequest);
        return ResponseEntity.ok(Map.of(
                "reserved", false,
                "message", "庫存服務暫時不可用，預留失敗"
        ));
    }

    @Override
    public ResponseEntity<Map<String, String>> confirmReservedStock(Long productId, Integer quantity) {
        log.warn("Inventory service is unavailable, using fallback for stock confirmation: product={}, quantity={}", 
                productId, quantity);
        return ResponseEntity.ok(Map.of(
                "message", "庫存服務暫時不可用，確認失敗",
                "productId", productId.toString(),
                "quantity", quantity.toString()
        ));
    }

    @Override
    public ResponseEntity<Map<String, String>> releaseReservedStock(Long productId, Integer quantity) {
        log.warn("Inventory service is unavailable, using fallback for stock release: product={}, quantity={}", 
                productId, quantity);
        return ResponseEntity.ok(Map.of(
                "message", "庫存服務暫時不可用，釋放失敗",
                "productId", productId.toString(),
                "quantity", quantity.toString()
        ));
    }

    @Override
    public ResponseEntity<List<Map<String, Object>>> getBatchInventory(List<Long> productIds) {
        log.warn("Inventory service is unavailable, using fallback for batch inventory: {}", productIds);
        return ResponseEntity.ok(List.of(Map.of(
                "message", "庫存服務暫時不可用"
        )));
    }
}
