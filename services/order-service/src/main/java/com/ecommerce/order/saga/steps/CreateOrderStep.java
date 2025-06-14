package com.ecommerce.order.saga.steps;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.saga.SagaContext;
import com.ecommerce.order.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 創建訂單步驟
 * 在資料庫中建立訂單記錄
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderStep implements SagaStep {
    
    private final OrderRepository orderRepository;
    
    @Override
    public String getStepName() {
        return "創建訂單";
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
    
    @Override
    public CompletableFuture<Void> execute(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始創建訂單，交易ID: {}", context.getTransactionId());
                
                CreateOrderRequest orderRequest = context.getData("orderRequest");
                if (orderRequest == null) {
                    throw new RuntimeException("訂單請求資料不存在");
                }
                
                // 生成訂單號
                String orderNumber = generateOrderNumber();
                
                // 計算總金額
                BigDecimal totalAmount = calculateTotalAmount(orderRequest);
                
                // 建立訂單實體
                Order order = Order.builder()
                        .orderNumber(orderNumber)
                        .userId(orderRequest.getUserId())
                        .totalAmount(totalAmount)
                        .status(Order.OrderStatus.PENDING)
                        .shippingAddress(orderRequest.getShippingAddress())
                        .paymentMethod(orderRequest.getPaymentMethod())
                        .notes(orderRequest.getNotes())
                        .build();
                
                // 建立訂單明細
                List<OrderItem> orderItems = orderRequest.getOrderItems().stream()
                        .map(item -> createOrderItem(item, order))
                        .collect(Collectors.toList());
                
                order.setOrderItems(orderItems);
                
                // 儲存訂單
                Order savedOrder = orderRepository.save(order);
                
                // 將訂單資料儲存到上下文
                context.setOrderId(savedOrder.getId());
                context.addData("order", savedOrder);
                context.addCompensationData("orderId", savedOrder.getId());
                
                log.info("訂單創建成功: {}, 訂單ID: {}", orderNumber, savedOrder.getId());
                return null;
                
            } catch (Exception e) {
                log.error("創建訂單步驟執行失敗: {}", e.getMessage());
                throw new RuntimeException("創建訂單失敗", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> compensate(SagaContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("開始補償創建訂單，交易ID: {}", context.getTransactionId());
                
                Long orderId = context.getCompensationData("orderId");
                if (orderId != null) {
                    // 刪除或標記訂單為取消狀態
                    orderRepository.findById(orderId).ifPresent(order -> {
                        log.info("取消訂單: {}", order.getOrderNumber());
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                    });
                }
                
                log.info("創建訂單補償完成，交易ID: {}", context.getTransactionId());
                return null;
                
            } catch (Exception e) {
                log.error("創建訂單補償失敗: {}", e.getMessage());
                return null; // 補償失敗不拋出異常
            }
        });
    }
    
    /**
     * 生成訂單號
     */
    private String generateOrderNumber() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + String.format("%03d", (int) (Math.random() * 1000));
    }
    
    /**
     * 計算訂單總金額
     */
    private BigDecimal calculateTotalAmount(CreateOrderRequest request) {
        return request.getOrderItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 建立訂單明細項目
     */
    private OrderItem createOrderItem(CreateOrderRequest.OrderItemRequest itemRequest, Order order) {
        return OrderItem.builder()
                .order(order)
                .productId(itemRequest.getProductId())
                .productName(itemRequest.getProductName())
                .unitPrice(itemRequest.getUnitPrice())
                .quantity(itemRequest.getQuantity())
                .subtotal(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                .build();
    }
}
