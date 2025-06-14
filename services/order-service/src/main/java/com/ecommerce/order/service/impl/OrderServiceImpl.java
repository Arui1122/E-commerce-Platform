package com.ecommerce.order.service.impl;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.OrderEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderEventService orderEventService;
    
    // Feign clients for inter-service communication
    private final InventoryClient inventoryClient;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final ProductClient productClient;
    
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        
        try {
            // 1. 驗證用戶是否存在
            validateUser(request.getUserId());
            
            // 2. 檢查並預留庫存
            reserveInventoryForOrder(request);
            
            // 3. 生成訂單號
            String orderNumber = generateOrderNumber();
            
            // 4. 計算總金額
            BigDecimal totalAmount = calculateTotalAmount(request.getOrderItems());
            
            // 5. 創建訂單實體
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .userId(request.getUserId())
                    .totalAmount(totalAmount)
                    .status(Order.OrderStatus.PENDING)
                    .shippingAddress(request.getShippingAddress())
                    .paymentMethod(request.getPaymentMethod())
                    .notes(request.getNotes())
                    .build();
            
            // 6. 創建訂單明細
            List<OrderItem> orderItems = request.getOrderItems().stream()
                    .map(item -> createOrderItem(item, order))
                    .collect(Collectors.toList());
            
            order.setOrderItems(orderItems);
            
            // 7. 保存訂單
            Order savedOrder = orderRepository.save(order);
            
            // 8. 清空購物車（如果訂單來自購物車）
            if (request.isClearCart()) {
                clearUserCart(request.getUserId());
            }
            
            // 9. 發送訂單創建事件
            orderEventService.sendOrderCreatedEvent(savedOrder);
            
            log.info("Order created successfully: {}", orderNumber);
            return convertToResponse(savedOrder);
            
        } catch (Exception e) {
            log.error("Failed to create order for user: {}", request.getUserId(), e);
            // 釋放已預留的庫存
            releaseInventoryForOrder(request);
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }
    
    /**
     * 驗證用戶是否存在
     */
    private void validateUser(Long userId) {
        try {
            var response = userClient.getUserById(userId);
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("User not found or invalid: " + userId);
            }
            log.info("User validation successful for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to validate user: {}", userId, e);
            throw new RuntimeException("User validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 為訂單預留庫存
     */
    private void reserveInventoryForOrder(CreateOrderRequest request) {
        for (var item : request.getOrderItems()) {
            try {
                // 檢查庫存是否充足
                var stockCheckResponse = inventoryClient.checkStock(item.getProductId(), item.getQuantity());
                if (!stockCheckResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Insufficient stock for product: " + item.getProductId());
                }
                
                // 預留庫存
                Map<String, Object> reservationRequest = new HashMap<>();
                reservationRequest.put("productId", item.getProductId());
                reservationRequest.put("quantity", item.getQuantity());
                reservationRequest.put("referenceId", "ORDER_" + System.currentTimeMillis());
                
                var reserveResponse = inventoryClient.reserveStock(reservationRequest);
                if (reserveResponse == null || !reserveResponse.getStatusCode().is2xxSuccessful()) {
                    throw new RuntimeException("Failed to reserve stock for product: " + item.getProductId());
                }
                
                log.info("Stock reserved for product: {}, quantity: {}", item.getProductId(), item.getQuantity());
                
            } catch (Exception e) {
                log.error("Failed to reserve inventory for product: {}", item.getProductId(), e);
                throw new RuntimeException("Inventory reservation failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 釋放訂單的預留庫存
     */
    private void releaseInventoryForOrder(CreateOrderRequest request) {
        for (var item : request.getOrderItems()) {
            try {
                inventoryClient.releaseReservedStock(item.getProductId(), item.getQuantity());
                log.info("Released stock for product: {}, quantity: {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to release stock for product: {}", item.getProductId(), e);
            }
        }
    }
    
    /**
     * 清空用戶購物車
     */
    private void clearUserCart(Long userId) {
        try {
            var response = cartClient.clearCart(userId);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Cart cleared for user: {}", userId);
            } else {
                log.warn("Failed to clear cart for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error clearing cart for user: {}", userId, e);
            // 不拋出異常，因為這不是關鍵操作
        }
    }
    
    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return convertToResponse(order);
    }
    
    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));
        return convertToResponse(order);
    }
    
    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(userId, pageable);
        return orders.map(this::convertToResponse);
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        
        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        
        // Send order status updated event
        orderEventService.sendOrderStatusUpdatedEvent(savedOrder, oldStatus, status);
        
        log.info("Order status updated: {} from {} to {}", order.getOrderNumber(), oldStatus, status);
        return convertToResponse(savedOrder);
    }
    
    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        return updateOrderStatus(id, Order.OrderStatus.CANCELLED);
    }
    
    @Override
    @Transactional
    public void processPayment(Long orderId, String paymentDetails) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in pending status");
        }
        
        // Here you would integrate with actual payment gateway
        // For now, we'll simulate successful payment
        log.info("Processing payment for order: {} with details: {}", order.getOrderNumber(), paymentDetails);
        
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
        
        // Send payment processed event
        orderEventService.sendPaymentProcessedEvent(order);
        
        log.info("Payment processed successfully for order: {}", order.getOrderNumber());
    }
    
    @Override
    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORDER-" + timestamp + "-" + System.nanoTime() % 10000;
    }
    
    private BigDecimal calculateTotalAmount(List<CreateOrderRequest.OrderItemRequest> orderItems) {
        return orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private OrderItem createOrderItem(CreateOrderRequest.OrderItemRequest request, Order order) {
        BigDecimal subtotal = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        
        return OrderItem.builder()
                .order(order)
                .productId(request.getProductId())
                .productName(request.getProductName())
                .productSku(request.getProductSku())
                .unitPrice(request.getUnitPrice())
                .quantity(request.getQuantity())
                .subtotal(subtotal)
                .build();
    }
    
    private OrderResponse convertToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .notes(order.getNotes())
                .orderItems(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    private OrderResponse.OrderItemResponse convertToItemResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
