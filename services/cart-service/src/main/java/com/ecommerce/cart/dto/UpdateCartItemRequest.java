package com.ecommerce.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 更新購物車項目請求 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "更新購物車項目請求")
public class UpdateCartItemRequest {
    
    @Schema(description = "商品數量", example = "3")
    @NotNull(message = "商品數量不能為空")
    @Positive(message = "商品數量必須大於0")
    private Integer quantity;
}
