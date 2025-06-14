package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockReservationRequest;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory", description = "庫存管理 API")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "創建或更新庫存", description = "為指定商品創建或更新庫存數量")
    public ResponseEntity<InventoryResponse> createOrUpdateInventory(
            @Valid @RequestBody InventoryRequest request) {
        
        log.info("Creating or updating inventory for product: {}", request.getProductId());
        InventoryResponse response = inventoryService.createOrUpdateInventory(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "查詢商品庫存", description = "根據商品ID查詢庫存信息")
    public ResponseEntity<InventoryResponse> getInventory(
            @Parameter(description = "商品ID") @PathVariable Long productId) {
        
        log.info("Getting inventory for product: {}", productId);
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "檢查庫存是否充足", description = "檢查指定商品是否有足夠庫存")
    public ResponseEntity<Map<String, Object>> checkStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "需要的數量") @RequestParam Integer quantity) {
        
        log.info("Checking stock for product: {}, quantity: {}", productId, quantity);
        boolean hasStock = inventoryService.checkStock(productId, quantity);
        
        Map<String, Object> response = Map.of(
                "productId", productId,
                "requestedQuantity", quantity,
                "hasStock", hasStock
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reserve")
    @Operation(summary = "預留庫存", description = "為訂單預留指定數量的庫存")
    public ResponseEntity<Map<String, Object>> reserveStock(
            @Valid @RequestBody StockReservationRequest request) {
        
        log.info("Reserving stock for product: {}, quantity: {}", 
                request.getProductId(), request.getQuantity());
        
        boolean reserved = inventoryService.reserveStock(request);
        
        Map<String, Object> response = Map.of(
                "productId", request.getProductId(),
                "quantity", request.getQuantity(),
                "reserved", reserved,
                "referenceId", request.getReferenceId() != null ? request.getReferenceId() : ""
        );
        
        HttpStatus status = reserved ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{productId}/release")
    @Operation(summary = "釋放預留庫存", description = "釋放之前預留的庫存")
    public ResponseEntity<Map<String, String>> releaseReservedStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "釋放數量") @RequestParam Integer quantity) {
        
        log.info("Releasing reserved stock for product: {}, quantity: {}", productId, quantity);
        inventoryService.releaseReservedStock(productId, quantity);
        
        Map<String, String> response = Map.of(
                "message", "Reserved stock released successfully",
                "productId", productId.toString(),
                "quantity", quantity.toString()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/confirm")
    @Operation(summary = "確認預留庫存", description = "確認預留的庫存，實際扣減庫存")
    public ResponseEntity<Map<String, String>> confirmReservedStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "確認數量") @RequestParam Integer quantity) {
        
        log.info("Confirming reserved stock for product: {}, quantity: {}", productId, quantity);
        inventoryService.confirmReservedStock(productId, quantity);
        
        Map<String, String> response = Map.of(
                "message", "Reserved stock confirmed successfully",
                "productId", productId.toString(),
                "quantity", quantity.toString()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/replenish")
    @Operation(summary = "補充庫存", description = "為指定商品補充庫存")
    public ResponseEntity<InventoryResponse> replenishStock(
            @Parameter(description = "商品ID") @PathVariable Long productId,
            @Parameter(description = "補充數量") @RequestParam Integer quantity) {
        
        log.info("Replenishing stock for product: {}, quantity: {}", productId, quantity);
        InventoryResponse response = inventoryService.replenishStock(productId, quantity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "查詢庫存不足商品", description = "查詢庫存量低於指定數量的商品")
    public ResponseEntity<List<InventoryResponse>> getLowStockProducts(
            @Parameter(description = "最小庫存量閾值") @RequestParam(defaultValue = "10") Integer minQuantity) {
        
        log.info("Getting low stock products with minimum quantity: {}", minQuantity);
        List<InventoryResponse> response = inventoryService.getLowStockProducts(minQuantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量查詢庫存", description = "根據商品ID列表批量查詢庫存信息")
    public ResponseEntity<List<InventoryResponse>> getInventoriesByProductIds(
            @RequestBody List<Long> productIds) {
        
        log.info("Getting inventories for products: {}", productIds);
        List<InventoryResponse> response = inventoryService.getInventoriesByProductIds(productIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "健康檢查", description = "檢查庫存服務狀態")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = Map.of(
                "status", "UP",
                "message", "Inventory Service is running"
        );
        return ResponseEntity.ok(response);
    }
}
