package com.ecommerce.order.config;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;

/**
 * 熔斷器配置
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 自定義熔斷器配置
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10)) // 超時時間 10 秒
                        .build())
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(10) // 滑動窗口大小
                        .minimumNumberOfCalls(5) // 最小呼叫次數
                        .failureRateThreshold(50) // 失敗率閾值 50%
                        .waitDurationInOpenState(Duration.ofSeconds(30)) // 熔斷器開啟持續時間
                        .permittedNumberOfCallsInHalfOpenState(3) // 半開狀態允許的呼叫次數
                        .automaticTransitionFromOpenToHalfOpenEnabled(true) // 自動從開啟轉為半開
                        .build())
                .build());
    }

    /**
     * 庫存服務專用熔斷器配置
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> inventoryServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build())
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(60)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .build())
                .build(), "inventory-service");
    }

    /**
     * 購物車服務專用熔斷器配置
     */
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> cartServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(3))
                        .build())
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .slidingWindowSize(15)
                        .minimumNumberOfCalls(8)
                        .failureRateThreshold(40)
                        .waitDurationInOpenState(Duration.ofSeconds(20))
                        .build())
                .build(), "cart-service");
    }
}
