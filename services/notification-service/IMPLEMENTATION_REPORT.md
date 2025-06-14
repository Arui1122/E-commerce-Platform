# Notification Service Implementation Report

## 項目概述

Notification Service 是 E-commerce Platform 的通知服務，負責處理系統中的各種通知消息，包括郵件通知、短信通知和推送通知。該服務通過 Kafka 消息隊列接收其他服務的事件，並根據事件類型發送相應的通知。

## 技術架構

### 技術棧

- **Spring Boot 3.2.0** - 微服務框架
- **Spring Cloud** - 微服務治理
- **Spring Mail** - 郵件發送
- **Thymeleaf** - 郵件模板引擎
- **Kafka** - 異步消息處理
- **Docker** - 容器化部署
- **MailDev** - 郵件測試工具

### 服務端口

- **8086** - Notification Service REST API

## 核心功能

### 1. 郵件通知功能

#### 支持的郵件類型

- **訂單相關通知**

  - 訂單創建確認 (order-created)
  - 訂單支付確認 (order-confirmed)
  - 訂單發貨通知 (order-shipped)
  - 訂單送達通知 (order-delivered)
  - 訂單取消通知 (order-cancelled)

- **庫存預警通知**

  - 庫存不足警告 (inventory-low)
  - 庫存耗盡警告 (inventory-out-of-stock)
  - 庫存補充通知 (inventory-restocked)

- **用戶相關通知**
  - 歡迎郵件 (welcome)
  - 密碼重置 (password-reset)
  - 個人資料更新 (profile-updated)

#### 郵件模板

所有郵件使用 Thymeleaf 模板引擎，支援動態內容替換：

- 響應式 HTML 設計
- 品牌一致的視覺風格
- 支援變數和條件邏輯

### 2. Kafka 消息監聽

#### 監聽的主題 (Topics)

- `order.created` - 訂單創建事件
- `order.confirmed` - 訂單確認事件
- `order.shipped` - 訂單發貨事件
- `order.delivered` - 訂單送達事件
- `order.cancelled` - 訂單取消事件
- `inventory.low-stock` - 庫存不足事件
- `inventory.out-of-stock` - 庫存耗盡事件
- `inventory.restocked` - 庫存補充事件
- `user.registered` - 用戶註冊事件
- `user.password-reset` - 密碼重置事件
- `user.profile-updated` - 個人資料更新事件

### 3. REST API

#### 端點

- `POST /api/v1/notifications/send` - 發送通知消息
- `POST /api/v1/notifications/send-email` - 發送郵件通知
- `GET /api/v1/notifications/health` - 健康檢查

## 項目結構

```
notification-service/
├── src/
│   └── main/
│       ├── java/com/ecommerce/notification/
│       │   ├── NotificationServiceApplication.java
│       │   ├── config/
│       │   │   ├── KafkaConfig.java
│       │   │   └── OpenApiConfig.java
│       │   ├── controller/
│       │   │   └── NotificationController.java
│       │   ├── dto/
│       │   │   ├── NotificationMessage.java
│       │   │   ├── OrderEventMessage.java
│       │   │   ├── InventoryEventMessage.java
│       │   │   └── UserEventMessage.java
│       │   ├── listener/
│       │   │   ├── OrderEventListener.java
│       │   │   ├── InventoryEventListener.java
│       │   │   └── UserEventListener.java
│       │   └── service/
│       │       ├── NotificationService.java
│       │       └── impl/
│       │           └── NotificationServiceImpl.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── bootstrap.yml
│           └── templates/
│               ├── order-created.html
│               ├── order-confirmed.html
│               ├── order-shipped.html
│               ├── order-cancelled.html
│               ├── welcome.html
│               └── inventory-low.html
├── test-api.sh
├── Dockerfile
└── pom.xml
```

## 配置說明

### application-dev.yml 主要配置

```yaml
server:
  port: 8086

spring:
  mail:
    host: maildev # 開發環境使用 MailDev
    port: 1025

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service

notification:
  email:
    from: noreply@ecommerce.com
```

### Docker 配置

- 使用 MailDev 作為開發環境的 SMTP 服務器
- Web 界面端口：1080
- SMTP 端口：1025

## 部署說明

### Docker Compose 配置

已整合到主 docker-compose.yml 文件中：

```yaml
notification-service:
  build: ../services/notification-service
  ports:
    - "8086:8086"
  depends_on:
    - kafka
    - maildev
```

### 健康檢查

- 端點：`/actuator/health`
- 間隔：60 秒
- 超時：30 秒

## 測試

### API 測試

使用提供的測試腳本：

```bash
./test-api.sh
```

### 郵件查看

開發環境中發送的郵件可在 MailDev Web 界面查看：
http://localhost:1080

## 監控與運維

### 健康檢查

- Spring Boot Actuator 端點
- Prometheus 指標收集
- Docker 容器健康檢查

### 日誌

- 結構化日誌輸出
- Kafka 消息處理日誌
- 郵件發送狀態日誌

## 擴展功能規劃

### 短期計劃

- [ ] SMS 通知支援
- [ ] 推送通知支援
- [ ] 郵件模板管理界面
- [ ] 通知歷史記錄

### 長期計劃

- [ ] 通知規則引擎
- [ ] A/B 測試支援
- [ ] 多語言郵件模板
- [ ] 高級分析和報告

## 故障排除

### 常見問題

1. **郵件發送失敗**

   - 檢查 SMTP 配置
   - 確認 MailDev 服務運行正常

2. **Kafka 消息接收失敗**

   - 檢查 Kafka 連接配置
   - 確認主題是否存在

3. **模板渲染錯誤**
   - 檢查模板文件路徑
   - 確認變數名稱正確

### 日誌查看

```bash
docker logs notification-service
```

## 總結

Notification Service 成功實現了：
✅ Kafka 消息監聽功能
✅ 郵件通知發送
✅ HTML 郵件模板
✅ REST API 接口
✅ Docker 容器化部署
✅ 健康檢查和監控
✅ Swagger API 文檔

該服務為 E-commerce Platform 提供了完整的通知功能基礎，支援訂單、庫存、用戶等各種業務場景的通知需求。
