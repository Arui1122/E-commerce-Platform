package com.ecommerce.order.config;

import com.ecommerce.order.saga.SagaManager;
import com.ecommerce.order.saga.impl.DefaultSagaManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Saga 配置類
 * 配置分散式交易相關的 Bean
 */
@Configuration
public class SagaConfig {
    
    /**
     * 配置 Saga 管理器
     */
    @Bean
    public SagaManager<Void> sagaManager() {
        return new DefaultSagaManager();
    }
}
