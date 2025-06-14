package com.ecommerce.order.saga.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaManager;
import com.ecommerce.order.saga.steps.ClearCartStep;
import com.ecommerce.order.saga.steps.CreateOrderStep;
import com.ecommerce.order.saga.steps.ReserveInventoryStep;
import com.ecommerce.order.saga.steps.SendOrderEventStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 訂單 Saga 服務
 * 使用 Saga 模式管理訂單創建的分散式交易
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaService {
    
    private final SagaManager<Void> sagaManager;
    private final ReserveInventoryStep reserveInventoryStep;
    private final CreateOrderStep createOrderStep;
    private final ClearCartStep clearCartStep;
    private final SendOrderEventStep sendOrderEventStep;
    
    /**
     * 使用 Saga 模式創建訂單
     */
    public CompletableFuture<OrderResponse> createOrderWithSaga(CreateOrderRequest request) {
        log.info("開始使用 Saga 模式創建訂單: 使用者ID={}", request.getUserId());
        
        // 生成交易ID
        String transactionId = UUID.randomUUID().toString();
        
        // 建立 Saga 上下文
        SagaContext context = new SagaContext(transactionId, request.getUserId());
        context.addData("orderRequest", request);
        
        // 配置 Saga 步驟
        setupSagaSteps();
        
        // 執行 Saga 交易
        return sagaManager.execute(context)
                .thenApply(result -> {
                    // 從上下文獲取創建的訂單
                    Order order = context.getData("order");
                    if (order != null) {
                        log.info("Saga 訂單創建成功: 訂單號={}", order.getOrderNumber());
                        return convertToOrderResponse(order);
                    } else {
                        throw new RuntimeException("訂單創建失敗：無法獲取訂單資料");
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Saga 訂單創建失敗: 交易ID={}, 錯誤={}", 
                            transactionId, throwable.getMessage());
                    throw new RuntimeException("訂單創建失敗", throwable);
                });
    }
    
    /**
     * 設定 Saga 步驟
     */
    private void setupSagaSteps() {
        // 清空現有步驟
        sagaManager.getSteps().clear();
        
        // 按順序添加步驟
        sagaManager.addStep(reserveInventoryStep);    // 1. 預留庫存
        sagaManager.addStep(createOrderStep);         // 2. 創建訂單
        sagaManager.addStep(clearCartStep);           // 3. 清空購物車
        sagaManager.addStep(sendOrderEventStep);      // 4. 發送事件
        
        log.info("Saga 步驟配置完成，共 {} 個步驟", sagaManager.getSteps().size());
    }
    
    /**
     * 轉換為訂單回應物件
     */
    private OrderResponse convertToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
