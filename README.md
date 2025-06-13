# E-commerce Platform

一個基於微服務架構的高併發電商平台，使用 Spring Boot、Spring Cloud 等技術棧構建。

## 項目概述

這是一個完整的分散式電商平台，包含用戶管理、商品管理、購物車、訂單處理、庫存管理等核心功能，重點關注高併發、高可用性和可擴展性。

## 技術棧

### 後端

- **Spring Boot** 3.x - 微服務基礎框架
- **Spring Cloud** - 微服務治理
- **Spring Security** - 認證與授權
- **PostgreSQL** - 主要數據庫
- **Redis** - 緩存與會話存儲
- **Apache Kafka** - 消息隊列

### 部署

- **Docker** - 容器化
- **Docker Compose** - 本地部署編排

### 監控

- **Prometheus** - 指標收集
- **Grafana** - 數據視覺化
- **Spring Boot Actuator** - 健康檢查

## 項目結構

```
ecommerce-platform/
├── infrastructure/          # 基礎設施服務
│   ├── docker-compose.yml
│   ├── eureka-server/      # 服務發現
│   ├── config-server/      # 配置中心
│   └── api-gateway/        # API 網關
├── services/               # 業務服務
│   ├── user-service/       # 用戶服務
│   ├── product-service/    # 商品服務
│   ├── cart-service/       # 購物車服務
│   ├── order-service/      # 訂單服務
│   ├── inventory-service/  # 庫存服務
│   └── notification-service/ # 通知服務
├── monitoring/             # 監控相關
├── docs/                   # 文檔
└── scripts/               # 部署腳本
```

## 服務端口分配

| 服務名稱             | 端口 | 描述     |
| -------------------- | ---- | -------- |
| API Gateway          | 8080 | 統一入口 |
| Eureka Server        | 8761 | 服務發現 |
| Config Server        | 8888 | 配置中心 |
| User Service         | 8081 | 用戶管理 |
| Product Service      | 8082 | 商品管理 |
| Cart Service         | 8083 | 購物車   |
| Order Service        | 8084 | 訂單處理 |
| Inventory Service    | 8085 | 庫存管理 |
| Notification Service | 8086 | 消息通知 |

## 快速開始

### 前置要求

- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 啟動步驟

1. 克隆項目

```bash
git clone <repository-url>
cd E-commerce-Platform
```

2. 啟動基礎設施

```bash
cd infrastructure
docker-compose up -d
```

3. 啟動各微服務

```bash
./scripts/start-services.sh
```

### 訪問地址

- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090

## API 文檔

服務啟動後，可通過以下地址查看 API 文檔：

- http://localhost:8080/swagger-ui.html

## 監控

### Grafana 儀表板

- 系統監控: http://localhost:3000/d/system
- 業務監控: http://localhost:3000/d/business

### Prometheus 指標

- http://localhost:9090/targets

## 開發指南

詳細的開發指南和 API 文檔請參考 [docs](./docs/) 目錄。

## 貢獻

1. Fork 項目
2. 創建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 開啟 Pull Request

## 許可證

本項目採用 MIT 許可證 - 查看 [LICENSE](LICENSE) 文件了解詳情。
