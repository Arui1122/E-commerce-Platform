package com.ecommerce.order.client.fallback;

import com.ecommerce.order.client.CartClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Cart Service Fallback
 * 購物車服務的熔斷降級處理
 */
@Component
@Slf4j
public class CartClientFallback implements CartClient {

    @Override
    public ResponseEntity<Map<String, Object>> getCart(Long userId) {
        log.warn("Cart service is unavailable, using fallback for user: {}", userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "items", List.of(),
                "totalItems", 0,
                "totalPrice", 0.0,
                "message", "購物車服務暫時不可用"
        ));
    }

    @Override
    public ResponseEntity<Map<String, String>> clearCart(Long userId) {
        log.warn("Cart service is unavailable, using fallback for clear cart: {}", userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId.toString(),
                "message", "購物車服務暫時不可用，清空失敗"
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCartItemCount(Long userId) {
        log.warn("Cart service is unavailable, using fallback for cart count: {}", userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "count", 0,
                "message", "購物車服務暫時不可用"
        ));
    }
}
