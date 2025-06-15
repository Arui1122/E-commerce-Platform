package com.ecommerce.order.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 訂單服務監控指標服務
 */
@Service
public class OrderMetricsService {

    private final Counter orderCreatedCounter;
    private final Counter orderConfirmedCounter;
    private final Counter orderCancelledCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Timer orderProcessingTimer;
    private final Timer paymentProcessingTimer;
    private final MeterRegistry meterRegistry;

    // 用於追蹤實時指標
    private final AtomicLong activeOrders = new AtomicLong(0);
    private final AtomicLong totalOrderValue = new AtomicLong(0);

    public OrderMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 訂單創建計數器
        this.orderCreatedCounter = Counter.builder("order.created.count")
                .description("Number of orders created")
                .tag("service", "order")
                .register(meterRegistry);

        // 訂單確認計數器
        this.orderConfirmedCounter = Counter.builder("order.confirmed.count")
                .description("Number of orders confirmed")
                .tag("service", "order")
                .register(meterRegistry);

        // 訂單取消計數器
        this.orderCancelledCounter = Counter.builder("order.cancelled.count")
                .description("Number of orders cancelled")
                .tag("service", "order")
                .register(meterRegistry);

        // 支付成功計數器
        this.paymentSuccessCounter = Counter.builder("payment.success.count")
                .description("Number of successful payments")
                .tag("service", "order")
                .register(meterRegistry);

        // 支付失敗計數器
        this.paymentFailedCounter = Counter.builder("payment.failed.count")
                .description("Number of failed payments")
                .tag("service", "order")
                .register(meterRegistry);

        // 訂單處理時間計時器
        this.orderProcessingTimer = Timer.builder("order.processing.timer")
                .description("Order processing time")
                .tag("service", "order")
                .register(meterRegistry);

        // 支付處理時間計時器
        this.paymentProcessingTimer = Timer.builder("payment.processing.timer")
                .description("Payment processing time")
                .tag("service", "order")
                .register(meterRegistry);

        // 活躍訂單數量 Gauge
        Gauge.builder("order.active.count", activeOrders, AtomicLong::get)
                .description("Number of active orders")
                .tag("service", "order")
                .register(meterRegistry);

        // 訂單總金額 Gauge  
        Gauge.builder("order.total.value", totalOrderValue, AtomicLong::get)
                .description("Total value of all orders")
                .tag("service", "order")
                .register(meterRegistry);
    }

    // 訂單事件記錄方法
    public void recordOrderCreated() {
        orderCreatedCounter.increment();
        activeOrders.incrementAndGet();
    }

    public void recordOrderConfirmed() {
        orderConfirmedCounter.increment();
    }

    public void recordOrderCancelled() {
        orderCancelledCounter.increment();
        activeOrders.decrementAndGet();
    }

    public void recordPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
    }

    public void updateTotalOrderValue(long value) {
        totalOrderValue.addAndGet(value);
    }

    // 計時器方法
    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderProcessingTimer(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
    }

    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopPaymentProcessingTimer(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }

    // Getter for current metrics
    public long getActiveOrdersCount() {
        return activeOrders.get();
    }

    public long getTotalOrderValue() {
        return totalOrderValue.get();
    }
}
