package com.ecommerce.inventory.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 庫存服務監控指標服務
 */
@Service
public class InventoryMetricsService {

    private final Counter inventoryUpdateCounter;
    private final Counter inventoryReserveCounter;
    private final Counter inventoryReleaseCounter;
    private final Counter lowStockAlertCounter;
    private final Counter outOfStockCounter;
    private final Timer inventoryOperationTimer;
    private final MeterRegistry meterRegistry;

    // 實時監控指標
    private final AtomicLong totalProducts = new AtomicLong(0);
    private final AtomicLong lowStockProducts = new AtomicLong(0);
    private final AtomicLong outOfStockProducts = new AtomicLong(0);
    private final AtomicLong reservedQuantity = new AtomicLong(0);

    public InventoryMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 庫存更新計數器
        this.inventoryUpdateCounter = Counter.builder("inventory.update.count")
                .description("Number of inventory updates")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 庫存預留計數器
        this.inventoryReserveCounter = Counter.builder("inventory.reserve.count")
                .description("Number of inventory reservations")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 庫存釋放計數器
        this.inventoryReleaseCounter = Counter.builder("inventory.release.count")
                .description("Number of inventory releases")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 低庫存警告計數器
        this.lowStockAlertCounter = Counter.builder("inventory.low_stock.alert.count")
                .description("Number of low stock alerts")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 缺貨計數器
        this.outOfStockCounter = Counter.builder("inventory.out_of_stock.count")
                .description("Number of out of stock events")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 庫存操作計時器
        this.inventoryOperationTimer = Timer.builder("inventory.operation.timer")
                .description("Inventory operation processing time")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 商品總數 Gauge
        Gauge.builder("inventory.products.total", totalProducts, AtomicLong::get)
                .description("Total number of products in inventory")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 低庫存商品數 Gauge
        Gauge.builder("inventory.products.low_stock", lowStockProducts, AtomicLong::get)
                .description("Number of products with low stock")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 缺貨商品數 Gauge
        Gauge.builder("inventory.products.out_of_stock", outOfStockProducts, AtomicLong::get)
                .description("Number of out of stock products")
                .tag("service", "inventory")
                .register(meterRegistry);

        // 預留庫存總量 Gauge
        Gauge.builder("inventory.reserved.quantity", reservedQuantity, AtomicLong::get)
                .description("Total reserved inventory quantity")
                .tag("service", "inventory")
                .register(meterRegistry);
    }

    // 事件記錄方法
    public void recordInventoryUpdate() {
        inventoryUpdateCounter.increment();
    }

    public void recordInventoryReserve(int quantity) {
        inventoryReserveCounter.increment();
        reservedQuantity.addAndGet(quantity);
    }

    public void recordInventoryRelease(int quantity) {
        inventoryReleaseCounter.increment();
        reservedQuantity.addAndGet(-quantity);
    }

    public void recordLowStockAlert() {
        lowStockAlertCounter.increment();
    }

    public void recordOutOfStock() {
        outOfStockCounter.increment();
    }

    // 統計數據更新方法
    public void updateTotalProducts(long count) {
        totalProducts.set(count);
    }

    public void updateLowStockProducts(long count) {
        lowStockProducts.set(count);
    }

    public void updateOutOfStockProducts(long count) {
        outOfStockProducts.set(count);
    }

    // 計時器方法
    public Timer.Sample startInventoryOperationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopInventoryOperationTimer(Timer.Sample sample) {
        sample.stop(inventoryOperationTimer);
    }

    // Getter methods
    public long getTotalProducts() {
        return totalProducts.get();
    }

    public long getLowStockProducts() {
        return lowStockProducts.get();
    }

    public long getOutOfStockProducts() {
        return outOfStockProducts.get();
    }

    public long getReservedQuantity() {
        return reservedQuantity.get();
    }
}
