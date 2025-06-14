package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    
    OrderResponse createOrder(CreateOrderRequest request);
    
    OrderResponse getOrderById(Long id);
    
    OrderResponse getOrderByNumber(String orderNumber);
    
    List<OrderResponse> getOrdersByUserId(Long userId);
    
    Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable);
    
    OrderResponse updateOrderStatus(Long id, Order.OrderStatus status);
    
    OrderResponse cancelOrder(Long id);
    
    void processPayment(Long orderId, String paymentDetails);
    
    List<OrderResponse> getOrdersByStatus(Order.OrderStatus status);
}
