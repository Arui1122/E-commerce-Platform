package com.ecommerce.order.config;

import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.Retryer;

/**
 * Feign 客戶端配置
 */
@Configuration
public class FeignConfig extends FeignClientsConfiguration {

    /**
     * Feign 日誌級別配置
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Feign 請求配置
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10000, // connectTimeout 連接超時時間 10 秒
                60000, // readTimeout 讀取超時時間 60 秒
                true   // followRedirects
        );
    }

    /**
     * Feign 重試配置
     */
    @Bean
    @Override
    public Retryer feignRetryer() {
        // 最大重試次數為 3，重試間隔從 100ms 開始，最大間隔 1s
        return new Retryer.Default(100, 1000, 3);
    }
}
