# Grafana 視覺化配置說明

## 概述

本項目已完成 Grafana 視覺化配置，包含完整的監控儀表板和告警系統。

## Grafana 訪問信息

- **URL**: http://localhost:3000
- **用戶名**: admin
- **密碼**: admin

## 儀表板說明

### 1. 系統概覽 (System Overview)

**UID**: `ecommerce-overview`

**功能**:

- CPU 使用率 - 各服務的 CPU 使用情況
- JVM 內存使用 - 堆內存使用量監控
- HTTP 請求率 - 每秒請求數統計
- HTTP 響應時間 - 95th 百分位響應時間
- 服務健康狀態 - 所有服務的在線狀態

**適用場景**: 快速查看整個系統的健康狀態和性能概覽

### 2. 服務詳細監控 (Service Details)

**UID**: `ecommerce-services`

**功能**:

- 請求率按端點分組 - 詳細的 API 端點監控
- 響應時間百分位 - 50th 和 95th 百分位響應時間
- 錯誤率監控 - 4xx 和 5xx 錯誤統計
- 數據庫連接池 - 活躍和空閒連接數
- JVM GC 時間 - 垃圾回收時間統計
- 線程數量 - JVM 線程監控

**特色**:

- 支援服務過濾器，可選擇特定服務進行監控
- 詳細的性能指標分析

### 3. 業務指標 (Business Metrics)

**UID**: `ecommerce-business`

**功能**:

- 用戶註冊總數 - 累計註冊用戶
- 訂單創建總數 - 累計訂單數量
- 庫存操作次數 - 庫存更新統計
- 總收入 - 累計交易金額
- 小時業務活動 - 每小時用戶註冊和訂單增長
- 訂單狀態分佈 - 餅圖顯示各狀態訂單比例
- 庫存告警 - 低庫存和缺貨商品監控
- 收入趨勢 - 每小時收入變化

**適用場景**: 業務人員和產品經理監控業務指標

### 4. JVM 詳細指標 (JVM Metrics)

**UID**: `ecommerce-jvm`

**功能**:

- JVM 堆內存使用 - 已用、已提交、最大內存
- JVM 非堆內存使用 - 元空間等非堆區域監控
- GC 收集率 - 垃圾回收頻率
- GC 時間率 - 垃圾回收耗時
- 線程計數 - 活躍、守護進程、峰值線程
- 類加載 - 已加載類數量和加載速率

**適用場景**: 深入分析 JVM 性能和調優

## 告警配置

### 告警規則

1. **服務下線告警**

   - 觸發條件: 服務 up 指標為 0
   - 持續時間: 1 分鐘
   - 嚴重級別: critical

2. **高錯誤率告警**

   - 觸發條件: 錯誤率 > 5%
   - 持續時間: 2 分鐘
   - 嚴重級別: warning

3. **高響應時間告警**

   - 觸發條件: 95th 百分位響應時間 > 1 秒
   - 持續時間: 3 分鐘
   - 嚴重級別: warning

4. **高內存使用告警**

   - 觸發條件: JVM 堆內存使用率 > 80%
   - 持續時間: 5 分鐘
   - 嚴重級別: warning

5. **數據庫連接池告警**
   - 觸發條件: 連接池使用率 > 80%
   - 持續時間: 2 分鐘
   - 嚴重級別: warning

### 通知配置

- **郵件通知**: admin@ecommerce-platform.com
- **Slack 通知**: 需要配置 Webhook URL

## 使用建議

### 日常監控

1. 首先查看 **系統概覽** 儀表板，了解整體狀況
2. 如發現異常，查看 **服務詳細監控** 定位問題
3. 定期檢查 **業務指標** 了解業務發展
4. 性能調優時使用 **JVM 詳細指標**

### 故障排查

1. 檢查服務健康狀態
2. 查看錯誤率和響應時間趨勢
3. 分析 JVM 內存和 GC 情況
4. 檢查數據庫連接池狀態

### 性能優化

1. 監控響應時間趨勢
2. 分析高負載端點
3. 觀察 JVM 內存使用模式
4. 調整數據庫連接池配置

## 自定義配置

### 添加新儀表板

1. 將 JSON 文件放入 `monitoring/grafana/provisioning/dashboards/json/`
2. 重啟 Grafana 容器

### 修改告警規則

1. 編輯 `monitoring/grafana/provisioning/alerting/rules.yml`
2. 重啟 Grafana 容器

### 配置新的數據源

1. 編輯 `monitoring/grafana/provisioning/datasources/datasource.yml`
2. 重啟 Grafana 容器

## 技術細節

### 數據源配置

- **Prometheus**: http://prometheus:9090
- **刷新間隔**: 5 秒
- **查詢超時**: 60 秒

### 儀表板特性

- **自動刷新**: 5 秒間隔
- **時間範圍**: 可調整，默認 15 分鐘
- **變量支持**: 服務選擇器
- **響應式設計**: 支持不同屏幕尺寸

### 安全配置

- 禁用匿名訪問
- 禁用用戶註冊
- 默認查看者權限
- 啟用 HTTPS（生產環境建議）

## 故障排除

### 常見問題

1. **儀表板無數據**

   - 檢查 Prometheus 是否正常運行
   - 確認服務是否暴露 `/actuator/prometheus` 端點
   - 驗證時間範圍設置

2. **告警不觸發**

   - 檢查告警規則配置
   - 確認 Prometheus 有相應指標數據
   - 驗證告警管理器配置

3. **性能問題**
   - 調整查詢時間範圍
   - 優化查詢語句
   - 增加 Grafana 資源限制

### 日誌檢查

```bash
# 查看 Grafana 日誌
docker logs ecommerce-grafana

# 查看 Prometheus 日誌
docker logs ecommerce-prometheus
```

### 配置驗證

```bash
# 驗證 Grafana 配置
./scripts/test-grafana-config.sh

# 驗證 Prometheus 配置
./scripts/test-prometheus-metrics.sh
```

---

## 後續擴展

1. **添加更多業務指標**

   - 用戶活躍度
   - 轉化率
   - 商品瀏覽量

2. **集成其他數據源**

   - 應用程序日誌 (Loki)
   - 分佈式跟蹤 (Jaeger)
   - 基礎設施指標 (Node Exporter)

3. **高級告警**

   - 異常檢測
   - 預測性告警
   - 智能告警聚合

4. **可視化增強**
   - 地理位置儀表板
   - 實時用戶行為
   - 3D 網絡拓撲
