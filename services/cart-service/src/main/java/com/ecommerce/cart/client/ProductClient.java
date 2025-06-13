package com.ecommerce.cart.client;

import com.ecommerce.cart.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Product Service Feign 客戶端
 * 
 * 用於調用 Product Service 的 API
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductClient {
    
    /**
     * 根據商品ID獲取商品信息
     */
    @GetMapping("/{id}")
    Product getProductById(@PathVariable("id") Long id);
}
