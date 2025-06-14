package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String paymentMethod;
    private String notes;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}
