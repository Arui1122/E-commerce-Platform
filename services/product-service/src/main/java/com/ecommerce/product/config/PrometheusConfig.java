package com.ecommerce.product.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus 監控配置類
 * 配置自定義業務指標和監控項
 */
@Configuration
public class PrometheusConfig {

    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong totalProducts = new AtomicLong(0);
    private final AtomicLong popularProductViews = new AtomicLong(0);

    /**
     * 配置自定義指標
     */
    @Bean
    public void configureMetrics(MeterRegistry meterRegistry) {
        // 商品查詢次數計數器
        Counter.builder("product.search.count")
                .description("Number of product searches")
                .tag("service", "product")
                .register(meterRegistry);

        // 商品創建次數計數器
        Counter.builder("product.create.count")
                .description("Number of products created")
                .tag("service", "product")
                .register(meterRegistry);

        // 商品瀏覽次數計數器
        Counter.builder("product.view.count")
                .description("Number of product views")
                .tag("service", "product")
                .register(meterRegistry);

        // 活躍連接數 Gauge
        Gauge.builder("product.active.connections", activeConnections, AtomicLong::get)
                .description("Number of active connections")
                .tag("service", "product")
                .register(meterRegistry);

        // 商品總數 Gauge
        Gauge.builder("product.total.count", totalProducts, AtomicLong::get)
                .description("Total number of products")
                .tag("service", "product")
                .register(meterRegistry);

        // 熱門商品瀏覽數 Gauge
        Gauge.builder("product.popular.views", popularProductViews, AtomicLong::get)
                .description("Views of popular products")
                .tag("service", "product")
                .register(meterRegistry);

        // 商品查詢響應時間 Timer
        Timer.builder("product.search.timer")
                .description("Product search response time")
                .tag("service", "product")
                .register(meterRegistry);

        // JVM 內存使用 Gauge
        Gauge.builder("jvm.memory.used.custom", Runtime.getRuntime(), 
                runtime -> runtime.totalMemory() - runtime.freeMemory())
                .description("JVM memory used")
                .tag("service", "product")
                .register(meterRegistry);
    }

    /**
     * 信息貢獻者 - 為 /actuator/info 端點提供自定義信息
     */
    @Bean
    public InfoContributor customInfoContributor() {
        return new InfoContributor() {
            @Override
            public void contribute(Builder builder) {
                builder.withDetail("service", "product-service")
                       .withDetail("version", "1.0.0")
                       .withDetail("startup-time", Instant.now().toString())
                       .withDetail("description", "E-commerce Product Management Service")
                       .withDetail("features", new String[]{
                           "Product CRUD Operations",
                           "Category Management", 
                           "Product Search",
                           "Redis Caching",
                           "Prometheus Monitoring"
                       });
            }
        };
    }

    // Getter methods for updating metrics
    public AtomicLong getActiveConnections() {
        return activeConnections;
    }

    public AtomicLong getTotalProducts() {
        return totalProducts;
    }

    public AtomicLong getPopularProductViews() {
        return popularProductViews;
    }
}
