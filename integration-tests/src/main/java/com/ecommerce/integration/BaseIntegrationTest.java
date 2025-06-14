package com.ecommerce.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成測試基類
 * 提供 PostgreSQL, Redis, Kafka 等基礎設施的 TestContainers 配置
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL 配置
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Redis 配置
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        
        // Kafka 配置
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        
        // JPA 配置 - 針對測試環境優化
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        
        // 禁用 Eureka 註冊
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
        
        // 測試環境配置
        registry.add("logging.level.org.springframework.web", () -> "DEBUG");
        registry.add("logging.level.org.testcontainers", () -> "INFO");
        registry.add("logging.level.com.github.dockerjava", () -> "WARN");
    }

    @BeforeEach
    void setUpBase() {
        cleanupTestData();
        prepareTestData();
    }

    /**
     * 清理測試數據
     * 子類可以重寫此方法來執行特定的清理邏輯
     */
    protected void cleanupTestData() {
        // 默認實現為空，子類可以重寫
    }

    /**
     * 準備測試數據
     * 子類可以重寫此方法來準備特定的測試數據
     */
    protected void prepareTestData() {
        // 默認實現為空，子類可以重寫
    }

    /**
     * 獲取 PostgreSQL 容器
     */
    protected static PostgreSQLContainer<?> getPostgresContainer() {
        return postgres;
    }

    /**
     * 獲取 Redis 容器
     */
    protected static GenericContainer<?> getRedisContainer() {
        return redis;
    }

    /**
     * 獲取 Kafka 容器
     */
    protected static KafkaContainer getKafkaContainer() {
        return kafka;
    }
}
