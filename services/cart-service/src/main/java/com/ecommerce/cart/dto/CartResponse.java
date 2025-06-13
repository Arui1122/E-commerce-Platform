package com.ecommerce.cart.dto;

import com.ecommerce.cart.model.CartItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 購物車響應 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "購物車響應")
public class CartResponse {
    
    @Schema(description = "用戶ID", example = "1")
    private Long userId;
    
    @Schema(description = "購物車項目列表")
    private List<CartItem> items;
    
    @Schema(description = "商品總數量", example = "5")
    private Integer totalItems;
    
    @Schema(description = "購物車總價", example = "299.50")
    private BigDecimal totalPrice;
    
    @Schema(description = "購物車項目數量（不同商品種類數）", example = "3")
    private Integer itemCount;
}
