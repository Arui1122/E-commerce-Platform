package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "cart-service")
public interface CartClient {
    
    @GetMapping("/api/v1/cart/{userId}")
    Map<String, Object> getCartItems(@PathVariable("userId") Long userId);
    
    @DeleteMapping("/api/v1/cart/{userId}/clear")
    Map<String, Object> clearCart(@PathVariable("userId") Long userId);
}
