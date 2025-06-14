package com.ecommerce.order.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * User Service Feign Client
 * 用於調用用戶服務的 API
 */
@FeignClient(
    name = "user-service",
    path = "/api/v1/users"
)
public interface UserClient {

    /**
     * 根據用戶ID獲取用戶信息
     */
    @GetMapping("/{userId}")
    ResponseEntity<Map<String, Object>> getUserById(@PathVariable("userId") Long userId);

    /**
     * 根據用戶名獲取用戶信息
     */
    @GetMapping("/profile/{username}")
    ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable("username") String username);
}
