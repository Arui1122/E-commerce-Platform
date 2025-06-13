#!/bin/bash

# E-commerce Platform 微服務啟動腳本

echo "🚀 Starting E-commerce Platform Microservices..."

# 項目根目錄
PROJECT_ROOT="$(dirname "$0")/.."

# 啟動 Eureka Server
echo "Starting Eureka Server..."
cd "$PROJECT_ROOT/infrastructure/eureka-server" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
EUREKA_PID=$!
echo "Eureka Server started (PID: $EUREKA_PID)"

# 等待 Eureka Server 啟動
echo "Waiting for Eureka Server to be ready..."
sleep 30

# 啟動 Config Server
echo "Starting Config Server..."
cd "$PROJECT_ROOT/infrastructure/config-server" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
CONFIG_PID=$!
echo "Config Server started (PID: $CONFIG_PID)"

# 等待 Config Server 啟動
echo "Waiting for Config Server to be ready..."
sleep 20

# 啟動 API Gateway
echo "Starting API Gateway..."
cd "$PROJECT_ROOT/infrastructure/api-gateway" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
GATEWAY_PID=$!
echo "API Gateway started (PID: $GATEWAY_PID)"

echo ""
echo "🎉 All microservices started successfully!"
echo ""
echo "📊 Service URLs:"
echo "- Eureka Dashboard: http://localhost:8761"
echo "- Config Server: http://localhost:8888"
echo "- API Gateway: http://localhost:8080"
echo ""
echo "📝 Process IDs:"
echo "- Eureka Server: $EUREKA_PID"
echo "- Config Server: $CONFIG_PID"
echo "- API Gateway: $GATEWAY_PID"
echo ""
echo "🛑 To stop services, run: ./scripts/stop-services.sh"
