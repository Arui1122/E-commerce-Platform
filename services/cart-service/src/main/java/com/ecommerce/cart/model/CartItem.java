package com.ecommerce.cart.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 購物車項目
 * 
 * 存儲在 Redis 中的購物車商品信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    
    /**
     * 用戶ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名稱 (冗余存儲，提高查詢效率)
     */
    private String productName;
    
    /**
     * 商品價格 (冗余存儲)
     */
    private BigDecimal price;
    
    /**
     * 商品圖片URL (冗余存儲)
     */
    private String imageUrl;
    
    /**
     * 商品SKU (冗余存儲)
     */
    private String sku;
    
    /**
     * 購買數量
     */
    private Integer quantity;
    
    /**
     * 添加到購物車時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addedAt;
    
    /**
     * 最後更新時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 計算總價
     */
    public BigDecimal getTotalPrice() {
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
