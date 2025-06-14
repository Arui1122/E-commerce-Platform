# Inventory Service Testing Guide
# 庫存服務測試指南

## 📋 概述

本指南包含 Inventory Service (庫存服務) 的測試說明，包括單元測試、集成測試和並發測試。

## 🧪 測試類型

### 1. 單元測試 (Unit Tests)

測試個別組件的功能：

- **InventoryServiceTest** - 業務邏輯測試
- **InventoryTest** - 實體類測試

### 2. 集成測試 (Integration Tests)

測試組件間的交互：

- **InventoryControllerIntegrationTest** - REST API 測試

### 3. 並發測試 (Concurrency Tests)

測試高併發場景下的防超賣機制：

- **InventoryConcurrencyTest** - 分散式鎖和樂觀鎖測試

## 🚀 運行測試

### 前置條件

1. 確保有 Docker 環境（用於 TestContainers）
2. 確保有 Maven 3.6+ 和 JDK 17+
3. 確保 Redis 和 PostgreSQL 服務正常運行

### 運行所有測試

```bash
# 在 inventory-service 目錄下
mvn test
```

### 運行特定測試

```bash
# 運行單元測試
mvn test -Dtest=InventoryServiceTest

# 運行實體測試
mvn test -Dtest=InventoryTest

# 運行集成測試
mvn test -Dtest=InventoryControllerIntegrationTest

# 運行並發測試
mvn test -Dtest=InventoryConcurrencyTest
```

## 🔍 API 測試

### 手動 API 測試

運行預設的 API 測試腳本：

```bash
# 確保服務正在運行
cd /Users/arui/project/E-commerce-Platform
./scripts/test-inventory-service.sh
```

### 並發測試腳本

測試防超賣機制：

```bash
# 確保服務正在運行
./scripts/test-inventory-concurrency.sh
```

## 📊 測試場景

### 1. 基本功能測試

- ✅ 創建/更新庫存
- ✅ 查詢庫存信息
- ✅ 檢查庫存充足性
- ✅ 批量查詢庫存
- ✅ 查詢低庫存商品

### 2. 庫存生命周期測試

- ✅ 預留庫存
- ✅ 釋放預留庫存
- ✅ 確認預留庫存
- ✅ 補充庫存

### 3. 防超賣機制測試

- ✅ 分散式鎖機制
- ✅ 樂觀鎖版本控制
- ✅ 高併發場景下的庫存一致性
- ✅ 庫存不足時的正確處理

### 4. 異常情況測試

- ✅ 商品不存在
- ✅ 庫存不足
- ✅ 預留數量超過庫存
- ✅ 釋放數量超過預留數量
- ✅ 參數驗證

## 🎯 測試重點

### 並發測試重點

並發測試主要驗證以下關鍵點：

1. **無超賣**: 在高併發情況下，總預留數量不會超過可用庫存
2. **數據一致性**: 庫存數據在併發操作後保持一致
3. **原子性**: 每個庫存操作都是原子的，不會出現部分成功
4. **性能**: 在合理時間內完成併發操作

### 測試數據

- 初始庫存: 100
- 併發請求數: 20
- 每次請求數量: 10
- 總請求數量: 200（超過庫存量）

預期結果:
- 成功預留: 10 個請求（總計 100 個商品）
- 失敗請求: 10 個請求
- 最終庫存: 總量 100，預留 100，可用 0

## 🐛 故障排除

### 常見問題

1. **TestContainers 啟動失敗**
   - 檢查 Docker 是否運行
   - 檢查端口是否被占用

2. **並發測試不穩定**
   - 檢查 Redis 連接
   - 檢查數據庫連接池配置

3. **測試數據污染**
   - 確保每個測試都清理測試數據
   - 使用事務回滾

### 日誌配置

在測試環境中啟用詳細日誌：

```yaml
logging:
  level:
    com.ecommerce.inventory: DEBUG
    org.springframework.transaction: DEBUG
    org.hibernate: DEBUG
```

## ✅ 驗證清單

測試完成後，確認以下功能正常：

- [ ] 基本 CRUD 操作
- [ ] 庫存預留/釋放/確認流程
- [ ] 併發場景下的數據一致性
- [ ] 防超賣機制有效性
- [ ] 異常情況處理
- [ ] API 響應格式正確
- [ ] 性能在可接受範圍內

## 📈 性能指標

期望的性能指標：

- 單次庫存查詢: < 50ms
- 庫存預留操作: < 100ms
- 併發測試完成時間: < 30s
- 系統吞吐量: > 100 TPS

## 🔄 持續集成

在 CI/CD 流水線中運行測試：

```bash
# 運行測試並生成報告
mvn test -Dmaven.test.failure.ignore=true
mvn surefire-report:report
```
