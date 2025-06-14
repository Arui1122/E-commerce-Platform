package com.ecommerce.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class InventoryEvent {
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("productId")
    private Long productId;
    
    @JsonProperty("productName")
    private String productName;
    
    @JsonProperty("sku")
    private String sku;
    
    @JsonProperty("currentQuantity")
    private Integer currentQuantity;
    
    @JsonProperty("previousQuantity")
    private Integer previousQuantity;
    
    @JsonProperty("reservedQuantity")
    private Integer reservedQuantity;
    
    @JsonProperty("threshold")
    private Integer threshold;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public InventoryEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public InventoryEvent(String eventType, Long productId, String productName, Integer currentQuantity) {
        this();
        this.eventType = eventType;
        this.productId = productId;
        this.productName = productName;
        this.currentQuantity = currentQuantity;
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public Integer getCurrentQuantity() {
        return currentQuantity;
    }
    
    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
    
    public Integer getPreviousQuantity() {
        return previousQuantity;
    }
    
    public void setPreviousQuantity(Integer previousQuantity) {
        this.previousQuantity = previousQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getThreshold() {
        return threshold;
    }
    
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "InventoryEvent{" +
                "eventType='" + eventType + '\'' +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", currentQuantity=" + currentQuantity +
                ", previousQuantity=" + previousQuantity +
                ", timestamp=" + timestamp +
                '}';
    }
}
