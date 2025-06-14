package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    @NotBlank(message = "Shipping address cannot be blank")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;
    
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    private String paymentMethod;
    
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        
        @NotNull(message = "Product ID cannot be null")
        private Long productId;
        
        @NotBlank(message = "Product name cannot be blank")
        @Size(max = 255, message = "Product name cannot exceed 255 characters")
        private String productName;
        
        @Size(max = 100, message = "Product SKU cannot exceed 100 characters")
        private String productSku;
        
        @NotNull(message = "Unit price cannot be null")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Unit price format is invalid")
        private BigDecimal unitPrice;
        
        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
