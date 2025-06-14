package com.ecommerce.inventory.service.impl;

import com.ecommerce.events.InventoryEvent;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.service.InventoryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventServiceImpl implements InventoryEventService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String INVENTORY_UPDATED_TOPIC = "inventory.updated";
    private static final String INVENTORY_LOW_STOCK_TOPIC = "inventory.low-stock";
    private static final String INVENTORY_OUT_OF_STOCK_TOPIC = "inventory.out-of-stock";
    private static final String INVENTORY_RESTOCKED_TOPIC = "inventory.restocked";
    
    @Override
    public void publishInventoryUpdatedEvent(Inventory inventory, Integer previousQuantity) {
        try {
            InventoryEvent event = new InventoryEvent("INVENTORY_UPDATED", 
                inventory.getProductId(), "Product", inventory.getQuantity());
            event.setPreviousQuantity(previousQuantity);
            event.setReservedQuantity(inventory.getReservedQuantity());
            
            kafkaTemplate.send(INVENTORY_UPDATED_TOPIC, inventory.getProductId().toString(), event);
            log.info("Published INVENTORY_UPDATED event for product: {}", inventory.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_UPDATED event for product: {}", inventory.getProductId(), e);
        }
    }
    
    @Override
    public void publishLowStockEvent(Inventory inventory, Integer threshold) {
        try {
            InventoryEvent event = new InventoryEvent("INVENTORY_LOW_STOCK", 
                inventory.getProductId(), "Product", inventory.getQuantity());
            event.setThreshold(threshold);
            event.setReservedQuantity(inventory.getReservedQuantity());
            
            kafkaTemplate.send(INVENTORY_LOW_STOCK_TOPIC, inventory.getProductId().toString(), event);
            log.info("Published INVENTORY_LOW_STOCK event for product: {}", inventory.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_LOW_STOCK event for product: {}", inventory.getProductId(), e);
        }
    }
    
    @Override
    public void publishOutOfStockEvent(Inventory inventory) {
        try {
            InventoryEvent event = new InventoryEvent("INVENTORY_OUT_OF_STOCK", 
                inventory.getProductId(), "Product", inventory.getQuantity());
            event.setReservedQuantity(inventory.getReservedQuantity());
            
            kafkaTemplate.send(INVENTORY_OUT_OF_STOCK_TOPIC, inventory.getProductId().toString(), event);
            log.info("Published INVENTORY_OUT_OF_STOCK event for product: {}", inventory.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_OUT_OF_STOCK event for product: {}", inventory.getProductId(), e);
        }
    }
    
    @Override
    public void publishRestockedEvent(Inventory inventory, Integer addedQuantity) {
        try {
            InventoryEvent event = new InventoryEvent("INVENTORY_RESTOCKED", 
                inventory.getProductId(), "Product", inventory.getQuantity());
            event.setPreviousQuantity(inventory.getQuantity() - addedQuantity);
            event.setReservedQuantity(inventory.getReservedQuantity());
            
            kafkaTemplate.send(INVENTORY_RESTOCKED_TOPIC, inventory.getProductId().toString(), event);
            log.info("Published INVENTORY_RESTOCKED event for product: {}", inventory.getProductId());
        } catch (Exception e) {
            log.error("Failed to publish INVENTORY_RESTOCKED event for product: {}", inventory.getProductId(), e);
        }
    }
}
