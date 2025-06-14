package com.ecommerce.inventory.service;

import com.ecommerce.events.InventoryEvent;
import com.ecommerce.inventory.entity.Inventory;

public interface InventoryEventService {
    
    void publishInventoryUpdatedEvent(Inventory inventory, Integer previousQuantity);
    
    void publishLowStockEvent(Inventory inventory, Integer threshold);
    
    void publishOutOfStockEvent(Inventory inventory);
    
    void publishRestockedEvent(Inventory inventory, Integer addedQuantity);
}
