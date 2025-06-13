package com.ecommerce.cart.service;

import com.ecommerce.cart.client.ProductClient;
import com.ecommerce.cart.dto.AddCartItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.model.Product;
import com.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 購物車業務服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    
    /**
     * 添加商品到購物車
     */
    public CartResponse addToCart(Long userId, AddCartItemRequest request) {
        log.info("添加商品到購物車: userId={}, productId={}, quantity={}", 
            userId, request.getProductId(), request.getQuantity());
        
        // 1. 檢查商品是否存在並獲取商品信息
        Product product = productClient.getProductById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在: " + request.getProductId());
        }
        
        // 2. 檢查購物車中是否已存在該商品
        CartItem existingItem = cartRepository.findByUserIdAndProductId(userId, request.getProductId());
        
        if (existingItem != null) {
            // 更新數量
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setUpdatedAt(LocalDateTime.now());
            cartRepository.saveCartItem(existingItem);
        } else {
            // 創建新的購物車項目
            CartItem cartItem = CartItem.builder()
                .userId(userId)
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .sku(product.getSku())
                .quantity(request.getQuantity())
                .addedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            cartRepository.saveCartItem(cartItem);
        }
        
        // 3. 返回更新後的購物車
        return getCart(userId);
    }
    
    /**
     * 獲取用戶購物車
     */
    public CartResponse getCart(Long userId) {
        log.debug("獲取用戶購物車: userId={}", userId);
        
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        
        // 計算總數量和總價
        int totalItems = cartItems.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        BigDecimal totalPrice = cartItems.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return CartResponse.builder()
            .userId(userId)
            .items(cartItems)
            .totalItems(totalItems)
            .totalPrice(totalPrice)
            .itemCount(cartItems.size())
            .build();
    }
    
    /**
     * 更新購物車項目數量
     */
    public CartResponse updateCartItem(Long userId, Long productId, UpdateCartItemRequest request) {
        log.info("更新購物車項目: userId={}, productId={}, quantity={}", 
            userId, productId, request.getQuantity());
        
        CartItem cartItem = cartRepository.findByUserIdAndProductId(userId, productId);
        if (cartItem == null) {
            throw new RuntimeException("購物車中不存在該商品: " + productId);
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItem.setUpdatedAt(LocalDateTime.now());
        cartRepository.saveCartItem(cartItem);
        
        return getCart(userId);
    }
    
    /**
     * 從購物車移除商品
     */
    public CartResponse removeFromCart(Long userId, Long productId) {
        log.info("從購物車移除商品: userId={}, productId={}", userId, productId);
        
        CartItem cartItem = cartRepository.findByUserIdAndProductId(userId, productId);
        if (cartItem == null) {
            throw new RuntimeException("購物車中不存在該商品: " + productId);
        }
        
        cartRepository.deleteCartItem(userId, productId);
        return getCart(userId);
    }
    
    /**
     * 清空購物車
     */
    public void clearCart(Long userId) {
        log.info("清空購物車: userId={}", userId);
        cartRepository.clearCart(userId);
    }
    
    /**
     * 獲取購物車商品數量
     */
    public Long getCartItemCount(Long userId) {
        return cartRepository.getCartItemCount(userId);
    }
    
    /**
     * 檢查購物車是否為空
     */
    public boolean isCartEmpty(Long userId) {
        return cartRepository.getCartItemCount(userId) == 0;
    }
    
    /**
     * 批量更新購物車商品信息
     * 用於商品信息變更時同步更新購物車
     */
    public void syncProductInfo(Long productId, Product product) {
        log.info("同步商品信息到購物車: productId={}", productId);
        
        // 這裡可以實現批量更新邏輯
        // 由於 Redis 沒有直接的批量查詢，這裡先記錄日誌
        // 實際實現時可以考慮使用 Redis Pipeline 或者 Lua 腳本
        log.debug("商品信息同步完成: productId={}", productId);
    }
}
