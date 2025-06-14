package com.ecommerce.order.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Product Service Feign Client
 * 用於調用商品服務的 API
 */
@FeignClient(
    name = "product-service",
    path = "/api/v1/products"
)
public interface ProductClient {

    /**
     * 根據商品ID獲取商品信息
     */
    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getProductById(@PathVariable("id") Long id);

    /**
     * 根據SKU獲取商品信息
     */
    @GetMapping("/sku/{sku}")
    ResponseEntity<Map<String, Object>> getProductBySku(@PathVariable("sku") String sku);

    /**
     * 獲取熱門商品
     */
    @GetMapping("/popular")
    ResponseEntity<List<Map<String, Object>>> getPopularProducts();

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir
    );

    /**
     * 根據分類獲取商品
     */
    @GetMapping("/category/{categoryId}")
    ResponseEntity<Map<String, Object>> getProductsByCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    );
}
