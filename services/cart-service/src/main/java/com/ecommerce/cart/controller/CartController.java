package com.ecommerce.cart.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.HashMap;

/**
 * 購物車控制器
 */
@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Cart", description = "購物車管理 API")
public class CartController {

    @Operation(summary = "健康檢查")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "cart-service");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "獲取用戶購物車")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("items", new Object[0]);
        response.put("totalItems", 0);
        response.put("totalPrice", 0.0);
        return ResponseEntity.ok(response);
    }
}
