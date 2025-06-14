package com.ecommerce.order.saga.steps;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 清空購物車步驟
 * 訂單創建成功後清空使用者購物車
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClearCartStep implements SagaStep {
    
    private final CartClient cartClient;
    
    @Override
    public String getStepName() {
        return "清空購物車";
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
    
    @Override
    public CompletableFuture<Void> execute(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始清空購物車，交易ID: {}", context.getTransactionId());
                
                CreateOrderRequest orderRequest = context.getData("orderRequest");
                if (orderRequest == null || !orderRequest.isClearCart()) {
                    log.info("不需要清空購物車，跳過此步驟");
                    return null;
                }
                
                // 調用購物車服務清空購物車
                ResponseEntity<Map<String, String>> response = cartClient.clearCart(orderRequest.getUserId());
                
                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.warn("清空購物車失敗，但不影響訂單創建: 使用者ID={}", orderRequest.getUserId());
                } else {
                    log.info("購物車清空成功: 使用者ID={}", orderRequest.getUserId());
                }
                
                return null;
                
            } catch (Exception e) {
                log.error("清空購物車步驟執行失敗: {}", e.getMessage());
                // 清空購物車失敗不應該影響整個訂單創建流程
                // 所以這裡不拋出異常，只記錄日誌
                return null;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> compensate(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始補償清空購物車，交易ID: {}", context.getTransactionId());
                
                CreateOrderRequest orderRequest = context.getData("orderRequest");
                if (orderRequest == null || !orderRequest.isClearCart()) {
                    log.info("不需要補償購物車，跳過此步驟");
                    return null;
                }
                
                // 重新添加商品到購物車
                // TODO: 需要在 CartClient 中添加 addToCart 方法
                orderRequest.getOrderItems().forEach(item -> {
                    try {
                        log.info("恢復購物車商品: 使用者ID={}, 商品ID={}, 數量={}", 
                                orderRequest.getUserId(), item.getProductId(), item.getQuantity());
                        
                        // cartClient.addToCart(orderRequest.getUserId(), item.getProductId(), item.getQuantity());
                        log.warn("購物車恢復功能待實現");
                        
                    } catch (Exception e) {
                        log.error("恢復購物車商品失敗: 商品ID={}, 錯誤={}", 
                                item.getProductId(), e.getMessage());
                    }
                });
                
                log.info("清空購物車補償完成，交易ID: {}", context.getTransactionId());
                return null;
                
            } catch (Exception e) {
                log.error("清空購物車補償失敗: {}", e.getMessage());
                return null; // 補償失敗不拋出異常
            }
        });
    }
    
    @Override
    public boolean needsCompensation(SagaContext context) {
        CreateOrderRequest orderRequest = context.getData("orderRequest");
        return orderRequest != null && orderRequest.isClearCart();
    }
}
