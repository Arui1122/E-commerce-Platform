package com.ecommerce.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEventMessage {
    private Long productId;
    private String productName;
    private Integer currentQuantity;
    private Integer threshold;
    private String status; // LOW_STOCK, OUT_OF_STOCK, RESTOCKED
}
