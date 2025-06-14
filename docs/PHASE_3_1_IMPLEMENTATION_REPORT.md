# Phase 3.1: 服務間通信實現報告

## 概述

本階段實現了電商平台微服務之間的通信機制，包括 OpenFeign 客戶端、熔斷器、負載均衡和降級處理。

## 已完成的功能

### 1. OpenFeign 客戶端配置

#### 1.1 依賴添加

為所有需要服務間通信的服務添加了 OpenFeign 依賴：

- `spring-cloud-starter-openfeign`
- `spring-cloud-starter-loadbalancer`
- `spring-cloud-starter-circuitbreaker-resilience4j`

#### 1.2 服務啟用 FeignClients

在以下服務的主應用類添加了 `@EnableFeignClients` 註解：

- order-service
- cart-service
- user-service
- inventory-service
- product-service
- notification-service

### 2. 服務間 API 調用

#### 2.1 Order Service Feign 客戶端

創建了以下 Feign 客戶端接口：

**InventoryClient.java**

- `getInventory(productId)` - 獲取商品庫存
- `checkStock(productId, quantity)` - 檢查庫存充足性
- `reserveStock(request)` - 預留庫存
- `confirmReservedStock(productId, quantity)` - 確認預留庫存
- `releaseReservedStock(productId, quantity)` - 釋放預留庫存
- `getBatchInventory(productIds)` - 批量查詢庫存

**CartClient.java**

- `getCart(userId)` - 獲取用戶購物車
- `clearCart(userId)` - 清空購物車
- `getCartItemCount(userId)` - 獲取購物車商品數量

**UserClient.java**

- `getUserById(userId)` - 根據用戶 ID 獲取用戶信息
- `getUserByUsername(username)` - 根據用戶名獲取用戶信息

**ProductClient.java**

- `getProductById(id)` - 根據商品 ID 獲取商品信息
- `getProductBySku(sku)` - 根據 SKU 獲取商品信息
- `getPopularProducts()` - 獲取熱門商品
- `searchProducts(...)` - 搜索商品
- `getProductsByCategory(...)` - 根據分類獲取商品

#### 2.2 服務集成邏輯

在 OrderServiceImpl 中實現了完整的服務間調用流程：

```java
public OrderResponse createOrder(CreateOrderRequest request) {
    // 1. 驗證用戶是否存在
    validateUser(request.getUserId());

    // 2. 檢查並預留庫存
    reserveInventoryForOrder(request);

    // 3. 創建訂單
    // ...訂單創建邏輯...

    // 4. 清空購物車（如果需要）
    if (request.isClearCart()) {
        clearUserCart(request.getUserId());
    }

    // 5. 發送訂單事件
    orderEventService.sendOrderCreatedEvent(savedOrder);
}
```

### 3. 熔斷器配置

#### 3.1 Resilience4j 配置

在 `application.yml` 中配置了熔斷器：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 60
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 5
      cart-service:
        sliding-window-size: 15
        minimum-number-of-calls: 8
        failure-rate-threshold: 40
        wait-duration-in-open-state: 20s
      default:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

#### 3.2 超時配置

```yaml
resilience4j:
  timelimiter:
    instances:
      inventory-service:
        timeout-duration: 5s
      cart-service:
        timeout-duration: 3s
      default:
        timeout-duration: 10s
```

### 4. Fallback 降級處理

#### 4.1 InventoryClientFallback

為庫存服務提供降級處理：

- 當庫存服務不可用時返回默認響應
- 記錄降級日誌
- 提供友好的錯誤信息

#### 4.2 CartClientFallback

為購物車服務提供降級處理：

- 當購物車服務不可用時返回空購物車
- 清空購物車操作失敗時的處理

### 5. Feign 客戶端配置

#### 5.1 FeignConfig.java

```java
@Configuration
public class FeignConfig extends FeignClientsConfiguration {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10000, // connectTimeout 10 秒
                60000, // readTimeout 60 秒
                true   // followRedirects
        );
    }

    @Bean
    @Override
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
```

#### 5.2 application.yml 配置

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
      client:
        config:
          default:
            connectTimeout: 10000
            readTimeout: 60000
            loggerLevel: basic
          inventory-service:
            connectTimeout: 5000
            readTimeout: 30000
          cart-service:
            connectTimeout: 3000
            readTimeout: 15000
```

## 服務間通信架構

### 調用關係圖

```
Order Service (8084)
├── User Service (8081) - 用戶驗證
├── Inventory Service (8085) - 庫存管理
├── Cart Service (8083) - 購物車操作
└── Product Service (8082) - 商品信息
```

### 通信流程

1. **訂單創建流程**

   - 驗證用戶信息 → User Service
   - 檢查商品庫存 → Inventory Service
   - 預留庫存 → Inventory Service
   - 創建訂單 → Order Service
   - 清空購物車 → Cart Service

2. **錯誤處理流程**
   - 服務不可用 → 觸發 Fallback
   - 超時處理 → 熔斷器開啟
   - 失敗率過高 → 自動降級

## 測試驗證

### 測試腳本

創建了 `test-service-integration.sh` 腳本用於驗證服務間通信：

- 檢查所有服務健康狀態
- 測試訂單創建的服務間調用
- 驗證熔斷器配置
- 檢查 Feign 客戶端狀態

### 測試結果

- ✅ OpenFeign 客戶端正常配置
- ✅ 服務間 API 調用成功
- ✅ 熔斷器配置生效
- ✅ Fallback 降級機制工作
- ✅ 負載均衡配置正確

## 技術要點

### 1. 服務發現集成

- 使用 Eureka 進行服務發現
- 通過服務名稱進行調用
- 自動負載均衡

### 2. 容錯機制

- 熔斷器防止服務雪崩
- 超時控制避免長時間等待
- Fallback 提供降級服務

### 3. 監控與觀測

- Actuator 端點監控熔斷器狀態
- 詳細的日誌記錄
- Prometheus 指標收集

## 後續優化建議

1. **性能優化**

   - 實現服務間調用的並行處理
   - 添加緩存機制減少服務調用
   - 優化序列化/反序列化

2. **安全增強**

   - 添加服務間認證
   - 實現 API 密鑰管理
   - 配置 HTTPS 通信

3. **可觀測性**
   - 集成分布式追踪 (Sleuth/Zipkin)
   - 添加更詳細的業務指標
   - 實現調用鏈監控

## 總結

Phase 3.1 成功實現了電商平台微服務間的通信機制，為後續的集成測試和性能優化奠定了堅實基礎。所有配置都經過測試驗證，確保系統的穩定性和可靠性。
