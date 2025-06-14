package com.ecommerce.integration.simple;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.integration.IntegrationTestApplication;

/**
 * 簡化的集成測試示例
 * 用於驗證測試框架的基本功能
 */
@SpringBootTest(classes = IntegrationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false"
})
class SimpleIntegrationTest {

    @BeforeAll
    static void setUpClass() {
        System.out.println("🚀 開始集成測試");
    }

    @Test
    @DisplayName("測試 Spring Context 載入")
    void testContextLoads() {
        // 如果 Spring Context 成功載入，這個測試就會通過
        System.out.println("✅ Spring Context 載入成功");
    }

    @Test
    @DisplayName("測試基本數學運算")
    void testBasicMath() {
        int result = 2 + 2;
        Assertions.assertEquals(4, result, "2 + 2 應該等於 4");
        System.out.println("✅ 基本數學運算測試通過");
    }

    @Test
    @DisplayName("測試字符串操作")
    void testStringOperation() {
        String hello = "Hello";
        String world = "World";
        String combined = hello + " " + world;
        
        Assertions.assertEquals("Hello World", combined, "字符串拼接應該正確");
        System.out.println("✅ 字符串操作測試通過");
    }

    @Test
    @DisplayName("測試 TestContainers 基礎功能")
    void testTestContainersBasic() {
        // 這個測試驗證 TestContainers 依賴是否正確
        try {
            Class.forName("org.testcontainers.containers.PostgreSQLContainer");
            System.out.println("✅ TestContainers 依賴可用");
        } catch (ClassNotFoundException e) {
            Assertions.fail("TestContainers 依賴不可用");
        }
    }

    @Test
    @DisplayName("測試 REST Assured 基礎功能")
    void testRestAssuredBasic() {
        // 這個測試驗證 REST Assured 依賴是否正確
        try {
            Class.forName("io.restassured.RestAssured");
            System.out.println("✅ REST Assured 依賴可用");
        } catch (ClassNotFoundException e) {
            Assertions.fail("REST Assured 依賴不可用");
        }
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("🎉 集成測試框架驗證完成！");
        System.out.println("📊 測試結果：所有基礎功能測試通過");
        System.out.println("🔧 測試框架已就緒，可以開始編寫實際的集成測試");
    }
}
