package com.ecommerce.user.service;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 用戶服務監控指標服務
 * 負責更新業務相關的 Prometheus 指標
 */
@Service
public class UserMetricsService {

    private final Counter userRegistrationCounter;
    private final Counter userLoginCounter;
    private final Counter userLoginFailedCounter;
    private final Timer userRegistrationTimer;
    private final Timer userLoginTimer;
    private final MeterRegistry meterRegistry;

    public UserMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.userRegistrationCounter = Counter.builder("user.registration.count")
                .description("Number of user registrations")
                .tag("service", "user")
                .register(meterRegistry);

        this.userLoginCounter = Counter.builder("user.login.count")
                .description("Number of successful user logins")
                .tag("service", "user")
                .tag("status", "success")
                .register(meterRegistry);

        this.userLoginFailedCounter = Counter.builder("user.login.count")
                .description("Number of failed user logins")
                .tag("service", "user")
                .tag("status", "failed")
                .register(meterRegistry);

        this.userRegistrationTimer = Timer.builder("user.registration.timer")
                .description("User registration response time")
                .tag("service", "user")
                .register(meterRegistry);

        this.userLoginTimer = Timer.builder("user.login.timer")
                .description("User login response time")
                .tag("service", "user")
                .register(meterRegistry);
    }

    /**
     * 記錄用戶註冊事件
     */
    public void recordUserRegistration() {
        userRegistrationCounter.increment();
    }

    /**
     * 記錄成功登入事件
     */
    public void recordSuccessfulLogin() {
        userLoginCounter.increment();
    }

    /**
     * 記錄登入失敗事件
     */
    public void recordFailedLogin() {
        userLoginFailedCounter.increment();
    }

    /**
     * 測量用戶註冊時間
     */
    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 結束註冊計時
     */
    public void stopRegistrationTimer(Timer.Sample sample) {
        sample.stop(userRegistrationTimer);
    }

    /**
     * 測量用戶登入時間
     */
    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 結束登入計時
     */
    public void stopLoginTimer(Timer.Sample sample) {
        sample.stop(userLoginTimer);
    }
}
