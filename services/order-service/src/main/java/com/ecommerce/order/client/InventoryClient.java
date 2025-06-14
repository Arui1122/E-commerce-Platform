package com.ecommerce.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    @GetMapping("/api/v1/inventory/{productId}")
    Map<String, Object> getInventory(@PathVariable("productId") Long productId);
    
    @PutMapping("/api/v1/inventory/{productId}/reserve")
    Map<String, Object> reserveInventory(@PathVariable("productId") Long productId, 
                                       @RequestParam("quantity") Integer quantity);
    
    @PutMapping("/api/v1/inventory/{productId}/confirm")
    Map<String, Object> confirmInventory(@PathVariable("productId") Long productId, 
                                       @RequestParam("quantity") Integer quantity);
    
    @PutMapping("/api/v1/inventory/{productId}/release")
    Map<String, Object> releaseInventory(@PathVariable("productId") Long productId, 
                                       @RequestParam("quantity") Integer quantity);
}
