package com.ecommerce.cart.repository;

import com.ecommerce.cart.model.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 購物車 Redis 存儲層
 * 
 * 使用 Redis Hash 存儲用戶購物車數據
 * Key 格式: cart:userId
 * Field: productId
 * Value: CartItem JSON
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_EXPIRATION = Duration.ofDays(30); // 購物車數據保存30天
    
    /**
     * 生成購物車 Redis Key
     */
    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
    
    /**
     * 添加或更新購物車項目
     */
    public void saveCartItem(CartItem cartItem) {
        try {
            String cartKey = getCartKey(cartItem.getUserId());
            String productKey = cartItem.getProductId().toString();
            String cartItemJson = objectMapper.writeValueAsString(cartItem);
            
            redisTemplate.opsForHash().put(cartKey, productKey, cartItemJson);
            redisTemplate.expire(cartKey, CART_EXPIRATION);
            
            log.debug("購物車項目已保存: userId={}, productId={}", 
                cartItem.getUserId(), cartItem.getProductId());
        } catch (JsonProcessingException e) {
            log.error("序列化購物車項目失敗", e);
            throw new RuntimeException("保存購物車項目失敗", e);
        }
    }
    
    /**
     * 獲取用戶的所有購物車項目
     */
    public List<CartItem> findByUserId(Long userId) {
        String cartKey = getCartKey(userId);
        List<Object> cartItemJsonList = redisTemplate.opsForHash().values(cartKey);
        
        List<CartItem> cartItems = new ArrayList<>();
        for (Object cartItemJson : cartItemJsonList) {
            try {
                CartItem cartItem = objectMapper.readValue(
                    cartItemJson.toString(), CartItem.class);
                cartItems.add(cartItem);
            } catch (JsonProcessingException e) {
                log.error("反序列化購物車項目失敗: {}", cartItemJson, e);
            }
        }
        
        log.debug("獲取用戶購物車: userId={}, items={}", userId, cartItems.size());
        return cartItems;
    }
    
    /**
     * 獲取指定商品的購物車項目
     */
    public CartItem findByUserIdAndProductId(Long userId, Long productId) {
        String cartKey = getCartKey(userId);
        String productKey = productId.toString();
        String cartItemJson = (String) redisTemplate.opsForHash().get(cartKey, productKey);
        
        if (cartItemJson == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(cartItemJson, CartItem.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化購物車項目失敗: productId={}", productId, e);
            return null;
        }
    }
    
    /**
     * 刪除購物車項目
     */
    public void deleteCartItem(Long userId, Long productId) {
        String cartKey = getCartKey(userId);
        String productKey = productId.toString();
        redisTemplate.opsForHash().delete(cartKey, productKey);
        
        log.debug("購物車項目已刪除: userId={}, productId={}", userId, productId);
    }
    
    /**
     * 清空用戶購物車
     */
    public void clearCart(Long userId) {
        String cartKey = getCartKey(userId);
        redisTemplate.delete(cartKey);
        
        log.debug("用戶購物車已清空: userId={}", userId);
    }
    
    /**
     * 獲取購物車商品數量
     */
    public Long getCartItemCount(Long userId) {
        String cartKey = getCartKey(userId);
        return redisTemplate.opsForHash().size(cartKey);
    }
    
    /**
     * 檢查購物車是否存在指定商品
     */
    public boolean existsCartItem(Long userId, Long productId) {
        String cartKey = getCartKey(userId);
        String productKey = productId.toString();
        return redisTemplate.opsForHash().hasKey(cartKey, productKey);
    }
    
    /**
     * 獲取用戶購物車中的所有商品ID
     */
    public Set<String> getProductIds(Long userId) {
        String cartKey = getCartKey(userId);
        return redisTemplate.opsForHash().keys(cartKey);
    }
}
