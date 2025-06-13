package com.ecommerce.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 添加到購物車請求 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "添加到購物車請求")
public class AddCartItemRequest {
    
    @Schema(description = "商品ID", example = "1")
    @NotNull(message = "商品ID不能為空")
    private Long productId;
    
    @Schema(description = "商品數量", example = "2")
    @NotNull(message = "商品數量不能為空")
    @Positive(message = "商品數量必須大於0")
    private Integer quantity;
}
