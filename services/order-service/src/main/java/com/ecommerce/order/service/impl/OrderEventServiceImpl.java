package com.ecommerce.order.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventServiceImpl implements OrderEventService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_STATUS_UPDATED_TOPIC = "order.status.updated";
    private static final String PAYMENT_PROCESSED_TOPIC = "payment.processed";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";
    
    @Override
    public void sendOrderCreatedEvent(Order order) {
        try {
            Map<String, Object> event = createOrderEvent(order);
            event.put("eventType", "ORDER_CREATED");
            
            kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getOrderNumber(), event);
            log.info("Order created event sent for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order created event for order: {}", order.getOrderNumber(), e);
        }
    }
    
    @Override
    public void sendOrderStatusUpdatedEvent(Order order, Order.OrderStatus oldStatus, Order.OrderStatus newStatus) {
        try {
            Map<String, Object> event = createOrderEvent(order);
            event.put("eventType", "ORDER_STATUS_UPDATED");
            event.put("oldStatus", oldStatus.name());
            event.put("newStatus", newStatus.name());
            
            kafkaTemplate.send(ORDER_STATUS_UPDATED_TOPIC, order.getOrderNumber(), event);
            log.info("Order status updated event sent for order: {} from {} to {}", 
                    order.getOrderNumber(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send order status updated event for order: {}", order.getOrderNumber(), e);
        }
    }
    
    @Override
    public void sendPaymentProcessedEvent(Order order) {
        try {
            Map<String, Object> event = createOrderEvent(order);
            event.put("eventType", "PAYMENT_PROCESSED");
            
            kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, order.getOrderNumber(), event);
            log.info("Payment processed event sent for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send payment processed event for order: {}", order.getOrderNumber(), e);
        }
    }
    
    @Override
    public void sendOrderCancelledEvent(Order order, String reason) {
        try {
            Map<String, Object> event = createOrderEvent(order);
            event.put("eventType", "ORDER_CANCELLED");
            event.put("reason", reason);
            
            kafkaTemplate.send(ORDER_CANCELLED_TOPIC, order.getOrderNumber(), event);
            log.info("Order cancelled event sent for order: {} with reason: {}", order.getOrderNumber(), reason);
        } catch (Exception e) {
            log.error("Failed to send order cancelled event for order: {}", order.getOrderNumber(), e);
        }
    }
    
    private Map<String, Object> createOrderEvent(Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId());
        event.put("orderNumber", order.getOrderNumber());
        event.put("userId", order.getUserId());
        event.put("totalAmount", order.getTotalAmount());
        event.put("status", order.getStatus().name());
        event.put("createdAt", order.getCreatedAt());
        return event;
    }
}
