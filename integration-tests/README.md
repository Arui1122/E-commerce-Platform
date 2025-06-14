# E-commerce Platform - 集成測試文檔

## 📋 概述

本文檔描述了 E-commerce Platform 的集成測試實現，包括測試環境搭建、端到端測試、性能測試和異常場景測試。

## 🏗️ 架構設計

### 測試架構

```
integration-tests/
├── src/
│   ├── main/java/
│   │   └── com/ecommerce/integration/
│   │       ├── BaseIntegrationTest.java    # 集成測試基類
│   │       └── util/
│   │           └── TestDataBuilder.java    # 測試數據構建工具
│   └── test/java/
│       └── com/ecommerce/integration/
│           ├── e2e/
│           │   └── E2EIntegrationTest.java # 端到端測試
│           ├── performance/
│           │   └── PerformanceIntegrationTest.java # 性能測試
│           └── exception/
│               └── ExceptionScenarioIntegrationTest.java # 異常場景測試
├── pom.xml                                 # Maven 配置
└── README.md                              # 本文檔
```

### 技術棧

- **TestContainers**: 用於提供隔離的測試環境（PostgreSQL, Redis, Kafka）
- **REST Assured**: 用於 API 測試
- **JUnit 5**: 測試框架
- **Spring Boot Test**: Spring 集成測試支持
- **Maven Surefire/Failsafe**: 測試運行和報告

## 🧪 測試類型

### 1. 端到端測試 (E2E)

**目的**: 驗證完整的業務流程，從用戶註冊到訂單完成

**測試流程**:

1. 檢查所有服務健康狀態
2. 用戶註冊
3. 用戶登入獲取認證令牌
4. 創建測試商品
5. 初始化商品庫存
6. 添加商品到購物車
7. 查看購物車內容
8. 創建訂單
9. 驗證庫存扣減
10. 驗證購物車已清空

**覆蓋服務**:

- User Service (用戶服務)
- Product Service (商品服務)
- Inventory Service (庫存服務)
- Cart Service (購物車服務)
- Order Service (訂單服務)

### 2. 性能測試 (Performance)

**目的**: 驗證系統在併發負載下的性能表現

**測試場景**:

- **商品查詢併發測試**: 50 個併發用戶，每用戶 10 次請求
- **庫存查詢併發測試**: 模擬高併發庫存查詢
- **併發下單防超賣測試**: 150 個併發訂單競爭 100 個庫存

**性能指標**:

- 響應時間 < 2000ms
- 成功率 ≥ 95%
- 吞吐量 ≥ 20 requests/sec
- 防超賣機制有效性

### 3. 異常場景測試 (Exception Scenarios)

**目的**: 驗證系統的錯誤處理和安全防護能力

**測試場景**:

- 無效認證令牌
- 訪問不存在的資源
- 無效請求數據
- 庫存不足場景
- 重複註冊相同用戶名
- 惡意請求限流
- SQL 注入防護
- XSS 攻擊防護
- 大文件上傳限制
- 系統恢復能力

## 🚀 運行測試

### 前置條件

1. **Docker**: 用於運行 TestContainers
2. **Java 17+**: 運行環境
3. **Maven 3.6+**: 構建工具
4. **8GB+ RAM**: 推薦內存大小

### 快速開始

```bash
# 運行完整的集成測試套件
./scripts/run-integration-tests.sh

# 或者運行特定類型的測試
./scripts/run-integration-tests.sh e2e          # 端到端測試
./scripts/run-integration-tests.sh performance  # 性能測試
./scripts/run-integration-tests.sh exception    # 異常場景測試
```

### 手動執行

```bash
# 1. 啟動基礎設施
docker compose -f infrastructure/docker-compose.yml up -d

# 2. 啟動微服務
cd services/user-service && mvn spring-boot:run &
cd services/product-service && mvn spring-boot:run &
cd services/inventory-service && mvn spring-boot:run &
cd services/cart-service && mvn spring-boot:run &
cd services/order-service && mvn spring-boot:run &

# 3. 運行集成測試
cd integration-tests
mvn clean test

# 4. 停止服務
./scripts/run-integration-tests.sh stop
```

## 🔧 配置

### TestContainers 配置

```java
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
```

### 測試環境屬性

```properties
# 測試配置
spring.profiles.active=test
spring.jpa.hibernate.ddl-auto=create-drop
eureka.client.enabled=false

# 性能調優
spring.jpa.show-sql=false
logging.level.org.testcontainers=INFO
```

## 📊 測試報告

### 生成報告

```bash
# 生成 Surefire 測試報告
mvn surefire-report:report

# 報告位置
target/site/surefire-report.html
```

### 報告內容

- 測試執行結果統計
- 成功/失敗測試詳情
- 執行時間分析
- 錯誤日誌和堆棧信息

## 🔍 測試數據管理

### 測試數據構建

```java
// 創建測試用戶
Map<String, Object> userData = TestDataBuilder.createTestUser("testuser", "test@example.com");

// 創建測試商品
Map<String, Object> productData = TestDataBuilder.createTestProduct("Test Product", 99.99);

// 創建測試庫存
Map<String, Object> inventoryData = TestDataBuilder.createTestInventory(productId, 100);
```

### 數據清理策略

- **每個測試類**: 使用 `@DirtiesContext` 確保隔離
- **每個測試方法**: `@BeforeEach` 中清理和準備數據
- **TestContainers**: 自動管理容器生命周期

## 🐛 故障排除

### 常見問題

1. **TestContainers 啟動失敗**

   ```bash
   # 檢查 Docker 狀態
   docker info

   # 檢查端口占用
   lsof -i :5432  # PostgreSQL
   lsof -i :6379  # Redis
   ```

2. **服務連接超時**

   ```bash
   # 增加等待時間
   # 檢查服務健康狀態
   curl http://localhost:8080/actuator/health
   ```

3. **內存不足**
   ```bash
   # 增加 JVM 堆內存
   export MAVEN_OPTS="-Xmx4g -Xms2g"
   ```

### 調試模式

```bash
# 啟用調試日誌
mvn test -Dlogging.level.com.ecommerce=DEBUG

# 保留 TestContainers 容器
mvn test -Dtestcontainers.reuse.enable=true
```

## 📈 性能基準

### 目標性能指標

| 指標              | 目標值   | 實際值     |
| ----------------- | -------- | ---------- |
| 平均響應時間      | < 200ms  | 測試時確定 |
| 95 百分位響應時間 | < 500ms  | 測試時確定 |
| 併發用戶數        | 50+      | 測試驗證   |
| 吞吐量            | 100+ TPS | 測試驗證   |
| 成功率            | ≥ 99%    | 測試驗證   |

### 性能監控

```bash
# 查看測試過程中的系統資源使用
top -p $(pgrep -f "spring-boot")

# 監控 Docker 容器資源
docker stats
```

## 🔄 持續集成

### CI/CD 配置

```yaml
# .github/workflows/integration-tests.yml
name: Integration Tests
on: [push, pull_request]

jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: "17"
      - name: Run Integration Tests
        run: ./scripts/run-integration-tests.sh
```

## 📝 測試清單

### 部署前檢查

- [ ] 所有端到端測試通過
- [ ] 性能測試滿足基準要求
- [ ] 異常場景測試覆蓋關鍵路徑
- [ ] 無安全漏洞檢測警告
- [ ] 測試報告生成正常

### 定期測試

- [ ] 每週執行完整測試套件
- [ ] 每月更新測試數據和場景
- [ ] 季度性能基準回顧
- [ ] 年度測試策略評估

## 🤝 貢獻指南

### 添加新測試

1. 在適當的包中創建測試類
2. 繼承 `BaseIntegrationTest`
3. 使用 `TestDataBuilder` 創建測試數據
4. 更新本文檔

### 測試命名規範

- 測試類: `*IntegrationTest.java`
- 測試方法: `test{Scenario}_{Expected}`
- 顯示名稱: `@DisplayName("描述性測試名稱")`

---

## 📞 支持

如有問題或建議，請：

1. 查看本文檔的故障排除部分
2. 檢查 GitHub Issues
3. 聯繫開發團隊

**最後更新**: 2025 年 6 月 15 日
