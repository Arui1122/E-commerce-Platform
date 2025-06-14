package com.ecommerce.order.saga.steps;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaStep;
import com.ecommerce.order.service.OrderEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 發送訂單事件步驟
 * 發送訂單創建成功的事件通知
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendOrderEventStep implements SagaStep {
    
    private final OrderEventService orderEventService;
    
    @Override
    public String getStepName() {
        return "發送訂單事件";
    }
    
    @Override
    public int getOrder() {
        return 4;
    }
    
    @Override
    public CompletableFuture<Void> execute(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始發送訂單事件，交易ID: {}", context.getTransactionId());
                
                Order order = context.getData("order");
                if (order == null) {
                    throw new RuntimeException("訂單資料不存在");
                }
                
                // 發送訂單創建事件
                orderEventService.sendOrderCreatedEvent(order);
                
                log.info("訂單事件發送成功: 訂單號={}", order.getOrderNumber());
                return null;
                
            } catch (Exception e) {
                log.error("發送訂單事件步驟執行失敗: {}", e.getMessage());
                // 事件發送失敗不應該影響整個訂單創建流程
                // 但可以考慮重試機制
                throw new RuntimeException("發送訂單事件失敗", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> compensate(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始補償訂單事件，交易ID: {}", context.getTransactionId());
                
                Order order = context.getData("order");
                if (order != null) {
                    // 發送訂單取消事件
                    orderEventService.sendOrderCancelledEvent(order, "Saga 交易失敗");
                    log.info("訂單取消事件發送成功: 訂單號={}", order.getOrderNumber());
                }
                
                return null;
                
            } catch (Exception e) {
                log.error("訂單事件補償失敗: {}", e.getMessage());
                return null; // 補償失敗不拋出異常
            }
        });
    }
    
    @Override
    public boolean needsCompensation(SagaContext context) {
        // 只有在有訂單資料時才需要補償
        Order order = context.getData("order");
        return order != null;
    }
}
