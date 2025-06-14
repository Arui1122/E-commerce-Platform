package com.ecommerce.integration.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 測試數據準備工具類
 */
@Component
public class TestDataBuilder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 創建測試用戶數據
     */
    public static Map<String, Object> createTestUser(String username, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("password", "Test123!@#");
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("phone", "+1234567890");
        return user;
    }

    /**
     * 創建測試商品數據
     */
    public static Map<String, Object> createTestProduct(String name, double price) {
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("description", "Test product description");
        product.put("price", price);
        product.put("categoryId", 1L);
        product.put("brand", "TestBrand");
        product.put("sku", "TEST-" + System.currentTimeMillis());
        product.put("status", "ACTIVE");
        return product;
    }

    /**
     * 創建測試庫存數據
     */
    public static Map<String, Object> createTestInventory(Long productId, int quantity) {
        Map<String, Object> inventory = new HashMap<>();
        inventory.put("productId", productId);
        inventory.put("quantity", quantity);
        return inventory;
    }

    /**
     * 創建測試訂單數據
     */
    public static Map<String, Object> createTestOrder(Long userId) {
        Map<String, Object> order = new HashMap<>();
        order.put("userId", userId);
        order.put("shippingAddress", "123 Test Street, Test City, TC 12345");
        order.put("paymentMethod", "CREDIT_CARD");
        order.put("notes", "Integration test order");
        order.put("clearCart", true);
        
        // 添加訂單項目
        Map<String, Object> orderItem = new HashMap<>();
        orderItem.put("productId", 1L);
        orderItem.put("productName", "Test Product");
        orderItem.put("unitPrice", 99.99);
        orderItem.put("quantity", 2);
        
        order.put("orderItems", new Object[]{orderItem});
        return order;
    }

    /**
     * 創建購物車項目數據
     */
    public static Map<String, Object> createCartItem(Long productId, int quantity) {
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("quantity", quantity);
        return cartItem;
    }

    /**
     * 將對象轉換為 JSON 字符串
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * 從 JSON 字符串解析對象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to parse JSON to object", e);
        }
    }
}
