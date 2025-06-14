package com.ecommerce.notification.listener;

import com.ecommerce.notification.dto.OrderEventMessage;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "order.created", groupId = "notification-service")
    public void handleOrderCreated(Map<String, Object> orderEventMap) {
        log.info("Received order created event: {}", orderEventMap);
        
        try {
            // 從 Map 中提取數據
            String orderNumber = (String) orderEventMap.get("orderNumber");
            Long userId = orderEventMap.get("userId") != null ? ((Number) orderEventMap.get("userId")).longValue() : null;
            Object totalAmountObj = orderEventMap.get("totalAmount");
            String totalAmount = totalAmountObj != null ? totalAmountObj.toString() : "0.00";
            
            // 創建郵件模板數據
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("customerName", "Customer"); // TODO: 從用戶服務獲取用戶名
            templateData.put("orderNumber", orderNumber);
            templateData.put("totalAmount", totalAmount);
            templateData.put("orderDate", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            templateData.put("shippingAddress", "Address will be provided"); // TODO: 添加實際地址
            
            // 發送訂單確認郵件
            String recipientEmail = "customer@example.com"; // TODO: 從用戶服務獲取郵件地址
            notificationService.sendEmailNotification(
                recipientEmail,
                "Order Confirmation - " + orderNumber,
                "order-created",
                templateData
            );
            
            log.info("Order confirmation email sent for order: {}", orderNumber);
            
        } catch (Exception e) {
            log.error("Failed to process order created event: {}", orderEventMap, e);
        }
    }
    
    @KafkaListener(topics = "order.confirmed", groupId = "notification-service")
    public void handleOrderConfirmed(OrderEventMessage orderEvent) {
        log.info("Received order confirmed event: {}", orderEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", orderEvent.getOrderNumber());
        templateData.put("userName", orderEvent.getUserName());
        templateData.put("totalAmount", orderEvent.getTotalAmount());
        
        notificationService.sendEmailNotification(
            orderEvent.getUserEmail(),
            "Order Confirmed - " + orderEvent.getOrderNumber(),
            "order-confirmed",
            templateData
        );
    }
    
    @KafkaListener(topics = "order.shipped", groupId = "notification-service")
    public void handleOrderShipped(OrderEventMessage orderEvent) {
        log.info("Received order shipped event: {}", orderEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", orderEvent.getOrderNumber());
        templateData.put("userName", orderEvent.getUserName());
        templateData.put("shippingAddress", orderEvent.getShippingAddress());
        
        notificationService.sendEmailNotification(
            orderEvent.getUserEmail(),
            "Order Shipped - " + orderEvent.getOrderNumber(),
            "order-shipped",
            templateData
        );
    }
    
    @KafkaListener(topics = "order.delivered", groupId = "notification-service")
    public void handleOrderDelivered(OrderEventMessage orderEvent) {
        log.info("Received order delivered event: {}", orderEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", orderEvent.getOrderNumber());
        templateData.put("userName", orderEvent.getUserName());
        
        notificationService.sendEmailNotification(
            orderEvent.getUserEmail(),
            "Order Delivered - " + orderEvent.getOrderNumber(),
            "order-delivered",
            templateData
        );
    }
    
    @KafkaListener(topics = "order.cancelled", groupId = "notification-service")
    public void handleOrderCancelled(OrderEventMessage orderEvent) {
        log.info("Received order cancelled event: {}", orderEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderNumber", orderEvent.getOrderNumber());
        templateData.put("userName", orderEvent.getUserName());
        templateData.put("totalAmount", orderEvent.getTotalAmount());
        templateData.put("cancelReason", "Customer requested cancellation");
        
        notificationService.sendEmailNotification(
            orderEvent.getUserEmail(),
            "Order Cancelled - " + orderEvent.getOrderNumber(),
            "order-cancelled",
            templateData
        );
    }
}
