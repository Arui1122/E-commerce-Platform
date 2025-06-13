package com.ecommerce.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Cart Service Application
 * 
 * 購物車微服務主應用程序
 * 提供購物車管理功能，包括：
 * - 添加商品到購物車
 * - 修改商品數量
 * - 刪除購物車商品
 * - 獲取購物車列表
 * - 清空購物車
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
