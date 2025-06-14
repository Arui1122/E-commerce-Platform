package com.ecommerce.order.saga.steps;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 庫存預留步驟
 * 為訂單商品預留庫存
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReserveInventoryStep implements SagaStep {
    
    private final InventoryClient inventoryClient;
    
    @Override
    public String getStepName() {
        return "預留庫存";
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
    
    @Override
    public CompletableFuture<Void> execute(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始預留庫存，交易ID: {}", context.getTransactionId());
                
                CreateOrderRequest orderRequest = context.getData("orderRequest");
                if (orderRequest == null) {
                    throw new RuntimeException("訂單請求資料不存在");
                }
                
                // 為每個商品預留庫存
                orderRequest.getOrderItems().forEach(item -> {
                    try {
                        log.info("預留商品庫存: 商品ID={}, 數量={}", 
                                item.getProductId(), item.getQuantity());
                        
                        // 構建預留請求
                        Map<String, Object> reservationRequest = Map.of(
                                "productId", item.getProductId(),
                                "quantity", item.getQuantity(),
                                "transactionId", context.getTransactionId()
                        );
                        
                        ResponseEntity<Map<String, Object>> response = inventoryClient.reserveStock(reservationRequest);
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new RuntimeException("預留庫存失敗");
                        }
                        
                        // 儲存預留資訊以便補償
                        context.addCompensationData(
                                "reserved_" + item.getProductId(), 
                                item.getQuantity()
                        );
                        
                    } catch (Exception e) {
                        log.error("預留庫存失敗: 商品ID={}, 錯誤={}", 
                                item.getProductId(), e.getMessage());
                        throw new RuntimeException("預留庫存失敗: " + e.getMessage(), e);
                    }
                });
                
                log.info("庫存預留成功，交易ID: {}", context.getTransactionId());
                return null;
                
            } catch (Exception e) {
                log.error("預留庫存步驟執行失敗: {}", e.getMessage());
                throw new RuntimeException("預留庫存失敗", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> compensate(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始補償預留庫存，交易ID: {}", context.getTransactionId());
                
                CreateOrderRequest orderRequest = context.getData("orderRequest");
                if (orderRequest == null) {
                    log.warn("無法獲取訂單請求資料，跳過庫存補償");
                    return null;
                }
                
                // 釋放已預留的庫存
                orderRequest.getOrderItems().forEach(item -> {
                    try {
                        Integer reservedQuantity = context.getCompensationData("reserved_" + item.getProductId());
                        if (reservedQuantity != null) {
                            log.info("釋放預留庫存: 商品ID={}, 數量={}", 
                                    item.getProductId(), reservedQuantity);
                            
                            ResponseEntity<Map<String, String>> response = inventoryClient.releaseReservedStock(
                                    item.getProductId(),
                                    reservedQuantity
                            );
                            
                            if (!response.getStatusCode().is2xxSuccessful()) {
                                log.warn("釋放庫存回應異常: 商品ID={}", item.getProductId());
                            }
                        }
                    } catch (Exception e) {
                        log.error("釋放庫存失敗: 商品ID={}, 錯誤={}", 
                                item.getProductId(), e.getMessage());
                        // 補償失敗不應該拋出異常，只記錄日誌
                    }
                });
                
                log.info("庫存預留補償完成，交易ID: {}", context.getTransactionId());
                return null;
                
            } catch (Exception e) {
                log.error("庫存預留補償失敗: {}", e.getMessage());
                return null; // 補償失敗不拋出異常
            }
        });
    }
}
