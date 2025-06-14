package com.ecommerce.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", unique = true, nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory() {}

    public Inventory(Long productId, Integer quantity, Integer reservedQuantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean hasAvailableStock(Integer requestedQuantity) {
        return getAvailableQuantity() >= requestedQuantity;
    }

    public void reserveStock(Integer quantityToReserve) {
        if (!hasAvailableStock(quantityToReserve)) {
            throw new IllegalArgumentException("Insufficient stock available");
        }
        this.reservedQuantity += quantityToReserve;
    }

    public void releaseReservedStock(Integer quantityToRelease) {
        if (this.reservedQuantity < quantityToRelease) {
            throw new IllegalArgumentException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantityToRelease;
    }

    public void confirmReservedStock(Integer quantityToConfirm) {
        if (this.reservedQuantity < quantityToConfirm) {
            throw new IllegalArgumentException("Cannot confirm more than reserved quantity");
        }
        this.reservedQuantity -= quantityToConfirm;
        this.quantity -= quantityToConfirm;
    }

    public void addStock(Integer quantityToAdd) {
        this.quantity += quantityToAdd;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
