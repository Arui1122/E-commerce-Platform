package com.ecommerce.order.client;

import com.ecommerce.order.client.fallback.CartClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cart Service Feign Client
 * 用於調用購物車服務的 API
 */
@FeignClient(
    name = "cart-service",
    path = "/api/v1/carts",
    fallback = CartClientFallback.class
)
public interface CartClient {

    /**
     * 獲取用戶購物車
     */
    @GetMapping("/{userId}")
    ResponseEntity<Map<String, Object>> getCart(@PathVariable("userId") Long userId);

    /**
     * 清空用戶購物車
     */
    @DeleteMapping("/{userId}")
    ResponseEntity<Map<String, String>> clearCart(@PathVariable("userId") Long userId);

    /**
     * 獲取購物車商品數量
     */
    @GetMapping("/{userId}/count")
    ResponseEntity<Map<String, Object>> getCartItemCount(@PathVariable("userId") Long userId);
}
