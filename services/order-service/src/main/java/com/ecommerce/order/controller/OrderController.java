package com.ecommerce.order.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    @Operation(summary = "Create a new order", description = "Create a new order with the provided details")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable("id") Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Retrieve order details by order number")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable("orderNumber") String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID", description = "Retrieve all orders for a specific user")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable("userId") Long userId) {
        List<OrderResponse> response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get paginated orders by user ID", description = "Retrieve paginated orders for a specific user")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUserIdPaginated(
            @PathVariable("userId") Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<OrderResponse> response = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Update the status of an order")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String statusString = statusUpdate.get("status");
        Order.OrderStatus status = Order.OrderStatus.valueOf(statusString.toUpperCase());
        
        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable("id") Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/payment")
    @Operation(summary = "Process payment", description = "Process payment for an order")
    public ResponseEntity<String> processPayment(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> paymentDetails) {
        String paymentInfo = paymentDetails.get("paymentDetails");
        orderService.processPayment(id, paymentInfo);
        return ResponseEntity.ok("Payment processed successfully");
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieve all orders with a specific status")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable("status") String status) {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        List<OrderResponse> response = orderService.getOrdersByStatus(orderStatus);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the order service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Order Service is running");
    }
    
    @PostMapping("/saga")
    @Operation(summary = "使用 Saga 模式創建訂單", description = "使用分散式交易 Saga 模式創建訂單，確保資料一致性")
    public ResponseEntity<Map<String, Object>> createOrderWithSaga(@Valid @RequestBody CreateOrderRequest request) {
        try {
            orderService.createOrderWithSaga(request)
                    .thenAccept(orderResponse -> {
                        // 異步處理成功
                        log.info("Saga 訂單創建成功: 訂單號={}", orderResponse.getOrderNumber());
                    })
                    .exceptionally(throwable -> {
                        // 異步處理失敗
                        log.error("Saga 訂單創建失敗: {}", throwable.getMessage());
                        return null;
                    });
            
            // 立即返回接受狀態
            Map<String, Object> response = Map.of(
                    "message", "訂單創建請求已接受，正在處理中",
                    "status", "ACCEPTED"
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    "message", "訂單創建失敗: " + e.getMessage(),
                    "status", "FAILED"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
