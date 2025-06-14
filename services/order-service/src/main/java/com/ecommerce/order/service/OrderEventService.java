package com.ecommerce.order.service;

import com.ecommerce.order.entity.Order;

public interface OrderEventService {
    
    void sendOrderCreatedEvent(Order order);
    
    void sendOrderStatusUpdatedEvent(Order order, Order.OrderStatus oldStatus, Order.OrderStatus newStatus);
    
    void sendPaymentProcessedEvent(Order order);
}
