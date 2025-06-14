# Saga 分散式交易實現報告

## 概述

本項目實現了基於 **Saga 模式** 的分散式交易管理，用於處理訂單創建過程中涉及多個微服務的交易一致性問題。

## 架構設計

### Saga 模式選擇

我們選擇 **Choreography-based Saga** 模式，每個服務負責管理自己的本地交易和補償邏輯。

### 核心元件

#### 1. SagaManager（交易管理器）

- **介面**: `SagaManager<T>`
- **實現**: `DefaultSagaManager`
- **職責**: 協調各個交易步驟的執行和補償

#### 2. SagaContext（交易上下文）

- 儲存交易執行過程中的資料和狀態
- 支援交易狀態追蹤：PENDING、EXECUTING、COMPLETED、COMPENSATING、COMPENSATED、FAILED

#### 3. SagaStep（交易步驟）

- **介面**: `SagaStep`
- **職責**: 定義每個交易步驟的執行和補償邏輯

## 訂單創建 Saga 流程

### 交易步驟順序

1. **ReserveInventoryStep** - 預留庫存
2. **CreateOrderStep** - 建立訂單
3. **ClearCartStep** - 清空購物車
4. **SendOrderEventStep** - 發送事件通知

### 補償邏輯

每個步驟都實現了對應的補償操作：

1. **庫存預留補償** - 釋放已預留的庫存
2. **訂單建立補償** - 取消訂單狀態
3. **購物車清空補償** - 恢復購物車商品（待完善）
4. **事件通知補償** - 發送訂單取消事件

## 技術實現

### 異步處理

```java
@Override
public CompletableFuture<OrderResponse> createOrderWithSaga(CreateOrderRequest request) {
    return sagaManager.execute(context)
        .thenApply(result -> convertToOrderResponse(order));
}
```

### 補償機制

```java
@Override
public CompletableFuture<Void> compensate(SagaContext context, int failedStepIndex) {
    // 反向執行補償操作
    for (int i = failedStepIndex - 1; i >= 0; i--) {
        SagaStep step = steps.get(i);
        if (step.needsCompensation(context)) {
            step.compensate(context);
        }
    }
}
```

## API 端點

### 使用 Saga 模式創建訂單

```http
POST /api/v1/orders/saga
Content-Type: application/json

{
  "userId": 1,
  "orderItems": [
    {
      "productId": 1,
      "productName": "測試商品",
      "unitPrice": 99.99,
      "quantity": 2
    }
  ],
  "shippingAddress": "台北市信義區信義路五段7號",
  "paymentMethod": "信用卡",
  "clearCart": true
}
```

### 回應格式

```json
{
  "message": "訂單創建請求已接受，正在處理中",
  "status": "ACCEPTED"
}
```

## 監控與日誌

### 日誌記錄

每個 Saga 步驟都有詳細的日誌記錄：

```
2025-06-14 23:53:00 [INFO] 開始執行 Saga 交易: 550e8400-e29b-41d4-a716-446655440000
2025-06-14 23:53:00 [INFO] 執行步驟 1: 預留庫存
2025-06-14 23:53:01 [INFO] 執行步驟 2: 創建訂單
2025-06-14 23:53:01 [INFO] 執行步驟 3: 清空購物車
2025-06-14 23:53:01 [INFO] 執行步驟 4: 發送訂單事件
2025-06-14 23:53:01 [INFO] Saga 交易執行完成: 550e8400-e29b-41d4-a716-446655440000
```

### 事件發布

通過 Kafka 發布相關事件：

- `order.created` - 訂單創建成功
- `order.cancelled` - 訂單取消（補償）
- `order.status.updated` - 訂單狀態更新

## 測試

### 測試腳本

提供 `test-saga-transaction.sh` 腳本進行端到端測試：

```bash
./scripts/test-saga-transaction.sh
```

### 測試場景

1. **正常流程測試** - 所有步驟成功執行
2. **異常場景測試** - 某個步驟失敗，觸發補償
3. **併發測試** - 多個 Saga 同時執行

## 優勢

### 1. 資料一致性

- 通過補償機制確保分散式交易的最終一致性
- 避免了分散式鎖的效能問題

### 2. 可靠性

- 每個步驟都有明確的補償邏輯
- 失敗時自動觸發補償操作

### 3. 可觀測性

- 完整的日誌記錄和狀態追蹤
- 事件驅動的監控機制

### 4. 擴展性

- 可以輕鬆添加新的交易步驟
- 支援複雜的業務流程

## 限制與改進

### 當前限制

1. **購物車恢復** - 需要在 CartClient 中實現 addToCart 方法
2. **重試機制** - 尚未實現步驟失敗的重試邏輯
3. **事務日誌** - 沒有持久化 Saga 狀態

### 未來改進

1. **Saga 狀態持久化** - 將 Saga 狀態儲存到資料庫
2. **重試與超時** - 實現步驟重試和超時機制
3. **監控儀表板** - 創建 Saga 監控和管理界面
4. **效能優化** - 並行執行不相依的步驟

## 配置

### Saga 配置

```java
@Configuration
public class SagaConfig {
    @Bean
    public SagaManager<Void> sagaManager() {
        return new DefaultSagaManager();
    }
}
```

### 步驟配置

```java
// 設定 Saga 步驟
sagaManager.addStep(reserveInventoryStep);    // 1. 預留庫存
sagaManager.addStep(createOrderStep);         // 2. 創建訂單
sagaManager.addStep(clearCartStep);           // 3. 清空購物車
sagaManager.addStep(sendOrderEventStep);      // 4. 發送事件
```

## 結論

本 Saga 實現為電商平台提供了強大的分散式交易管理能力，確保了訂單創建過程的資料一致性和可靠性。通過模組化的設計和完善的補償機制，為複雜的業務流程提供了穩定的技術支撐。
