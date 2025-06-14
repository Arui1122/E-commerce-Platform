package com.ecommerce.notification.listener;

import com.ecommerce.notification.dto.InventoryEventMessage;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {
    
    private final NotificationService notificationService;
    
    @Value("${notification.email.admin:admin@ecommerce.com}")
    private String adminEmail;
    
    @KafkaListener(topics = "inventory.low-stock", groupId = "notification-service")
    public void handleLowStock(InventoryEventMessage inventoryEvent) {
        log.info("Received low stock event: {}", inventoryEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("productName", inventoryEvent.getProductName());
        templateData.put("productId", inventoryEvent.getProductId());
        templateData.put("currentQuantity", inventoryEvent.getCurrentQuantity());
        templateData.put("threshold", inventoryEvent.getThreshold());
        
        notificationService.sendEmailNotification(
            adminEmail,
            "Low Stock Alert - " + inventoryEvent.getProductName(),
            "inventory-low",
            templateData
        );
    }
    
    @KafkaListener(topics = "inventory.out-of-stock", groupId = "notification-service")
    public void handleOutOfStock(InventoryEventMessage inventoryEvent) {
        log.info("Received out of stock event: {}", inventoryEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("productName", inventoryEvent.getProductName());
        templateData.put("productId", inventoryEvent.getProductId());
        templateData.put("currentQuantity", inventoryEvent.getCurrentQuantity());
        
        notificationService.sendEmailNotification(
            adminEmail,
            "Out of Stock Alert - " + inventoryEvent.getProductName(),
            "inventory-out-of-stock",
            templateData
        );
    }
    
    @KafkaListener(topics = "inventory.restocked", groupId = "notification-service")
    public void handleRestocked(InventoryEventMessage inventoryEvent) {
        log.info("Received restocked event: {}", inventoryEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("productName", inventoryEvent.getProductName());
        templateData.put("productId", inventoryEvent.getProductId());
        templateData.put("currentQuantity", inventoryEvent.getCurrentQuantity());
        
        notificationService.sendEmailNotification(
            adminEmail,
            "Inventory Restocked - " + inventoryEvent.getProductName(),
            "inventory-restocked",
            templateData
        );
    }
}
