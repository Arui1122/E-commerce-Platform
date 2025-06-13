# E-commerce Platform - Project Requirement Document (PRD)

## 1. 項目概述

### 1.1 項目目標

構建一個高併發、高可用、高效能的分散式電商平台，實踐現代微服務架構和相關技術棧，作為技術學習和展示的 side project。

### 1.2 技術學習目標

- 微服務架構設計與實現
- 分散式系統高併發處理
- Spring 生態系統深度應用
- 容器化部署與監控
- 消息隊列與異步處理
- 分散式緩存與數據一致性

## 2. 技術架構

### 2.1 核心技術棧

#### 後端框架

- **Spring Boot** 3.x - 微服務基礎框架
- **Spring Cloud** - 微服務治理
  - Spring Cloud Gateway - API 網關
  - Eureka Server - 服務發現
  - Spring Cloud Config - 配置中心
  - Resilience4j - 熔斷器
- **Spring Security** - 認證與授權
- **Spring Data JPA** - 數據存取層

#### 數據存儲

- **PostgreSQL** - 主要關聯式數據庫
- **Redis** - 緩存與會話存儲

#### 消息中間件

- **Apache Kafka** - 異步消息處理

#### 部署與容器化

- **Docker** - 容器化
- **Docker Compose** - 本地部署編排

#### 監控與觀測

- **Prometheus** - 指標收集
- **Grafana** - 數據視覺化
- **Spring Boot Actuator** - 應用健康檢查

#### 開發工具

- **Swagger/OpenAPI 3** - API 文檔
- **Git + GitHub** - 版本控制

### 2.2 微服務架構設計

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │ (Spring Cloud)  │
                    └─────────┬───────┘
                              │
                    ┌─────────┴───────┐
                    │ Eureka Server   │
                    │ (Service Discovery) │
                    └─────────────────┘

┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│ user-service │product-service│ cart-service │order-service │inventory-    │notification- │
│              │              │              │              │service       │service       │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
                                        │
                                ┌───────┴────────┐
                                │  Message Queue │
                                │    (Kafka)     │
                                └────────────────┘
                                        │
                        ┌───────────────┼───────────────┐
                        │               │               │
                  ┌──────────┐    ┌──────────┐   ┌──────────┐
                  │PostgreSQL│    │  Redis   │   │Prometheus│
                  │          │    │          │   │ Grafana  │
                  └──────────┘    └──────────┘   └──────────┘
```

### 2.3 服務職責劃分

| 服務名稱                 | 端口 | 職責         | 主要功能                       |
| ------------------------ | ---- | ------------ | ------------------------------ |
| **api-gateway**          | 8080 | API 統一入口 | 路由、認證、限流、熔斷         |
| **eureka-server**        | 8761 | 服務註冊中心 | 服務發現與註冊                 |
| **config-server**        | 8888 | 配置中心     | 統一配置管理                   |
| **user-service**         | 8081 | 用戶管理     | 註冊、登入、個人資料、JWT 認證 |
| **product-service**      | 8082 | 商品管理     | 商品 CRUD、搜索、分類          |
| **cart-service**         | 8083 | 購物車       | 購物車操作、Redis 緩存         |
| **order-service**        | 8084 | 訂單處理     | 訂單創建、狀態管理、支付       |
| **inventory-service**    | 8085 | 庫存管理     | 庫存檢查、扣減、防超賣         |
| **notification-service** | 8086 | 消息通知     | 郵件、SMS、推送通知            |

## 3. 功能需求

### 3.1 用戶管理模組 (user-service)

- 用戶註冊（郵箱驗證）
- 用戶登入/登出
- JWT Token 認證
- 個人資料管理
- 密碼找回
- 用戶權限管理（普通用戶、管理員）

### 3.2 商品管理模組 (product-service)

- 商品基本資訊 CRUD
- 商品分類管理
- 商品搜索（支持關鍵字、分類、價格範圍）
- 商品圖片上傳
- 商品評價系統
- 熱門商品推薦

### 3.3 購物車模組 (cart-service)

- 添加商品到購物車
- 修改商品數量
- 刪除購物車商品
- 購物車持久化（Redis）
- 購物車合併（登入後）

### 3.4 訂單管理模組 (order-service)

- 訂單創建
- 訂單狀態管理（待付款、已付款、已發貨、已完成、已取消）
- 訂單詳情查詢
- 訂單歷史記錄
- 模擬支付流程
- 訂單取消與退款

### 3.5 庫存管理模組 (inventory-service)

- 庫存查詢
- 庫存扣減（防超賣）
- 庫存補充
- 庫存預警
- 庫存鎖定/釋放

### 3.6 通知服務模組 (notification-service)

- 訂單狀態變更通知
- 庫存不足警告
- 系統公告
- 郵件通知模板

### 3.7 管理後台

- 商品管理界面
- 訂單管理界面
- 用戶管理界面
- 庫存管理界面
- 數據統計儀表板

## 4. 非功能需求

### 4.1 性能需求

- **響應時間**: API 響應時間 < 200ms (95th percentile)
- **吞吐量**: 支持 1000-5000 QPS
- **並發用戶**: 支持 5000 並發用戶

### 4.2 可用性需求

- **系統可用性**: 99.9%
- **服務熔斷**: 異常服務自動熔斷與恢復
- **優雅降級**: 非核心功能降級保證核心流程

### 4.3 安全需求

- **身份認證**: JWT Token 認證
- **API 安全**: Rate Limiting 防止 API 濫用
- **數據安全**: 敏感數據加密存儲
- **輸入驗證**: 防 SQL 注入、XSS 攻擊

### 4.4 可擴展性

- **水平擴展**: 支持服務實例水平擴展
- **數據庫分片**: 支持讀寫分離
- **緩存策略**: 多層緩存架構

## 5. 高併發場景設計

### 5.1 秒殺搶購場景

**技術方案:**

- Redis 分散式鎖防止超賣
- Kafka 異步處理下單請求
- 分層緩存（本地緩存 + Redis）
- 限流與熔斷保護

**流程設計:**

1. 用戶點擊搶購 → API Gateway 限流
2. 檢查 Redis 庫存 → 獲取分散式鎖
3. 創建預訂單 → 發送 Kafka 消息
4. 異步處理訂單 → 扣減庫存
5. 通知用戶結果

### 5.2 庫存防超賣

**技術方案:**

- 樂觀鎖 + 數據庫版本號
- Redis 原子操作 (DECR)
- 分散式鎖確保一致性

### 5.3 熱門商品查詢

**緩存策略:**

- L1: 本地緩存 (Caffeine)
- L2: Redis 緩存
- L3: 數據庫

**緩存更新:**

- 數據變更時主動刷新
- TTL 過期自動更新

### 5.4 並發下單處理

**異步處理:**

- Kafka 消息隊列削峰填谷
- 分散式事務確保一致性
- 冪等性處理防重複

## 6. 數據模型設計

### 6.1 用戶表 (users)

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2 商品表 (products)

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT,
    brand VARCHAR(100),
    sku VARCHAR(100) UNIQUE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.3 訂單表 (orders)

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    shipping_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.4 庫存表 (inventory)

```sql
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT UNIQUE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 7. API 設計規範

### 7.1 RESTful API 設計

- 使用標準 HTTP 方法 (GET, POST, PUT, DELETE)
- 統一響應格式
- 版本控制 (/api/v1/)
- 錯誤碼標準化

### 7.2 API 響應格式

```json
{
  "code": 200,
  "message": "success",
  "data": {...},
  "timestamp": "2025-06-13T10:30:00Z"
}
```

### 7.3 錯誤處理

```json
{
  "code": 400,
  "message": "Invalid request parameters",
  "errors": [
    {
      "field": "email",
      "message": "Email format is invalid"
    }
  ],
  "timestamp": "2025-06-13T10:30:00Z"
}
```

## 8. 部署架構

### 8.1 Docker Compose 部署

```yaml
# 主要服務組件
services:
  - PostgreSQL (主數據庫)
  - Redis (緩存)
  - Kafka + Zookeeper (消息隊列)
  - Eureka Server (服務發現)
  - API Gateway
  - 各微服務 (user, product, cart, order, inventory, notification)
  - Prometheus (監控)
  - Grafana (視覺化)
```

### 8.2 網絡配置

- 內部服務網絡隔離
- 對外只暴露 API Gateway 端口
- 數據庫網絡安全配置

## 9. 監控與運維

### 9.1 Prometheus 監控指標

- **系統指標**: CPU、內存、磁盤、網絡
- **應用指標**: QPS、響應時間、錯誤率
- **業務指標**: 訂單量、用戶活躍度、庫存狀態
- **JVM 指標**: 堆內存、GC 情況

### 9.2 Grafana 儀表板

- **系統概覽**: 整體系統健康狀態
- **服務監控**: 各微服務詳細指標
- **業務監控**: 核心業務指標趨勢
- **告警面板**: 異常情況實時告警

### 9.3 健康檢查

- Spring Boot Actuator 健康端點
- 數據庫連接檢查
- 外部依賴服務檢查

## 10. 開發規範

### 10.1 代碼規範

- Java 編碼規範 (Google Style)
- 統一的包結構
- 完整的單元測試
- 代碼 Review 流程

### 10.2 Git 工作流

- Feature 分支開發
- Pull Request 合併
- 語義化提交消息
- 版本標籤管理

### 10.3 文檔規範

- README 文檔
- API 文檔 (Swagger)
- 架構文檔
- 部署文檔

## 11. MVP 功能優先級

### Phase 1 (核心功能)

- [ ] 用戶註冊/登入
- [ ] 商品展示
- [ ] 購物車
- [ ] 簡單下單流程

### Phase 2 (完善功能)

- [ ] 庫存管理
- [ ] 訂單狀態管理
- [ ] 分散式緩存
- [ ] 異步消息處理

### Phase 3 (高級功能)

- [ ] 秒殺功能
- [ ] 監控系統
- [ ] 性能優化
- [ ] 高併發測試

---

**技術學習重點:**

1. 分散式架構設計思維
2. 微服務間通信與數據一致性
3. 高併發場景的技術解決方案
4. 監控與運維實踐
5. 容器化部署與 DevOps 流程
