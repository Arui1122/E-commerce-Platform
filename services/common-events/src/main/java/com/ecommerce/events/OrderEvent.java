package com.ecommerce.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderEvent {
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("orderId")
    private Long orderId;
    
    @JsonProperty("orderNumber")
    private String orderNumber;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("userEmail")
    private String userEmail;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("items")
    private List<OrderItemEvent> items;
    
    @JsonProperty("shippingAddress")
    private String shippingAddress;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public OrderEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    public OrderEvent(String eventType, Long orderId, String orderNumber, Long userId, String userEmail) {
        this();
        this.eventType = eventType;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.userEmail = userEmail;
    }
    
    // Getters and Setters
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<OrderItemEvent> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemEvent> items) {
        this.items = items;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "OrderEvent{" +
                "eventType='" + eventType + '\'' +
                ", orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", userId=" + userId +
                ", userEmail='" + userEmail + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    
    public static class OrderItemEvent {
        @JsonProperty("productId")
        private Long productId;
        
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("price")
        private BigDecimal price;
        
        // Constructors
        public OrderItemEvent() {}
        
        public OrderItemEvent(Long productId, String productName, Integer quantity, BigDecimal price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
        
        // Getters and Setters
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
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
