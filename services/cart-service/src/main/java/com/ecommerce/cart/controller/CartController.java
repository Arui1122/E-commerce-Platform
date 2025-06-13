package com.ecommerce.cart.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable("userId") Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("items", new ArrayList<>());
        response.put("totalItems", 0);
        response.put("totalPrice", 0.0);
        response.put("itemCount", 0);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "添加商品到購物車")
    @PostMapping("/{userId}/items")
    public ResponseEntity<Map<String, Object>> addToCart(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("message", "商品已添加到購物車");
        response.put("productId", request.get("productId"));
        response.put("quantity", request.get("quantity"));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "更新購物車項目數量")
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<Map<String, Object>> updateCartItem(
            @PathVariable("userId") Long userId,
            @PathVariable("productId") Long productId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("productId", productId);
        response.put("message", "商品數量已更新");
        response.put("quantity", request.get("quantity"));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "從購物車移除商品")
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(
            @PathVariable("userId") Long userId,
            @PathVariable("productId") Long productId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("productId", productId);
        response.put("message", "商品已從購物車移除");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "清空購物車")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> clearCart(@PathVariable("userId") Long userId) {
        Map<String, String> response = new HashMap<>();
        response.put("userId", userId.toString());
        response.put("message", "購物車已清空");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "獲取購物車商品數量")
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Object>> getCartItemCount(@PathVariable("userId") Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("count", 0);
        return ResponseEntity.ok(response);
    }
}
