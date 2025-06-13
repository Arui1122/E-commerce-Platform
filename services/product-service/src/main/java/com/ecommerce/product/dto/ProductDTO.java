package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private CategoryDTO category;
    private String brand;
    private String sku;
    private String imageUrl;
    private ProductStatus status;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ProductCreateRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String brand;
    private String sku;
    private String imageUrl;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ProductSearchRequest {
    private String name;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String keyword;
}
