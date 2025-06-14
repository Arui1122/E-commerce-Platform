#!/bin/bash

# E-commerce Platform - 慢網路環境啟動腳本
# 針對慢網路環境優化的服務啟動順序和等待時間

set -e

echo "🚀 Starting E-commerce Platform for Slow Network Environment..."
echo "📡 網路環境慢時，請耐心等待服務啟動..."

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 檢查 Docker 是否運行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# 檢查 Docker Compose 是否安裝
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed.${NC}"
    exit 1
fi

cd "$(dirname "$0")/../infrastructure"

# 停止現有容器（如果有）
echo -e "${YELLOW}🛑 Stopping existing containers...${NC}"
docker-compose down --remove-orphans

# 清理網絡和卷（可選）
echo -e "${YELLOW}🧹 Cleaning up networks and volumes...${NC}"
docker network prune -f
docker volume prune -f

# 1. 啟動基礎設施服務（增加更長的等待時間）
echo -e "${BLUE}🏗️  Step 1: Starting Infrastructure Services...${NC}"
docker-compose up -d postgres redis zookeeper kafka

echo -e "${YELLOW}⏳ Waiting for database and cache to be ready (60 seconds for slow network)...${NC}"
sleep 60

# 檢查 PostgreSQL 健康狀態
echo -e "${BLUE}🔍 Checking PostgreSQL health...${NC}"
for i in {1..10}; do
    if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PostgreSQL is ready${NC}"
        break
    fi
    echo -e "${YELLOW}⏳ Waiting for PostgreSQL... (attempt $i/10)${NC}"
    sleep 10
done

# 檢查 Redis 健康狀態
echo -e "${BLUE}🔍 Checking Redis health...${NC}"
for i in {1..10}; do
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Redis is ready${NC}"
        break
    fi
    echo -e "${YELLOW}⏳ Waiting for Redis... (attempt $i/10)${NC}"
    sleep 5
done

# 2. 啟動 Eureka Server（服務發現）
echo -e "${BLUE}🌐 Step 2: Starting Eureka Server...${NC}"
docker-compose up -d eureka-server

echo -e "${YELLOW}⏳ Waiting for Eureka Server to start (90 seconds for slow network)...${NC}"
sleep 90

# 檢查 Eureka Server 健康狀態
echo -e "${BLUE}🔍 Checking Eureka Server health...${NC}"
for i in {1..15}; do
    if curl -s -f http://localhost:8761/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Eureka Server is ready${NC}"
        break
    fi
    echo -e "${YELLOW}⏳ Waiting for Eureka Server... (attempt $i/15)${NC}"
    sleep 10
done

# 3. 啟動 Config Server（配置中心）
echo -e "${BLUE}⚙️  Step 3: Starting Config Server...${NC}"
docker-compose up -d config-server

echo -e "${YELLOW}⏳ Waiting for Config Server to start (60 seconds for slow network)...${NC}"
sleep 60

# 檢查 Config Server 健康狀態
echo -e "${BLUE}🔍 Checking Config Server health...${NC}"
for i in {1..12}; do
    if curl -s -f http://localhost:8888/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Config Server is ready${NC}"
        break
    fi
    echo -e "${YELLOW}⏳ Waiting for Config Server... (attempt $i/12)${NC}"
    sleep 10
done

# 4. 啟動業務服務（延長等待時間）
echo -e "${BLUE}🏪 Step 4: Starting Business Services...${NC}"

# 先啟動不依賴其他服務的服務
echo -e "${BLUE}👤 Starting User Service...${NC}"
docker-compose up -d user-service
sleep 45

echo -e "${BLUE}📦 Starting Product Service...${NC}"
docker-compose up -d product-service
sleep 45

echo -e "${BLUE}📦 Starting Inventory Service...${NC}"
docker-compose up -d inventory-service
sleep 45

echo -e "${BLUE}🛒 Starting Cart Service...${NC}"
docker-compose up -d cart-service
sleep 45

# 5. 啟動 API Gateway（最後啟動）
echo -e "${BLUE}🌉 Step 5: Starting API Gateway...${NC}"
docker-compose up -d api-gateway

echo -e "${YELLOW}⏳ Waiting for API Gateway to start (60 seconds for slow network)...${NC}"
sleep 60

# 6. 啟動監控服務
echo -e "${BLUE}📊 Step 6: Starting Monitoring Services...${NC}"
docker-compose up -d prometheus grafana

echo -e "${YELLOW}⏳ Waiting for monitoring services (30 seconds)...${NC}"
sleep 30

# 檢查所有服務狀態
echo -e "${BLUE}🔍 Checking all services status...${NC}"
echo "=================================================================="

# 檢查服務健康狀態的函數
check_service_health() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-10}
    
    echo -e "Checking ${service_name}..."
    for i in $(seq 1 $max_attempts); do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ ${service_name} is healthy${NC}"
            return 0
        fi
        echo -e "${YELLOW}⏳ Waiting for ${service_name}... (attempt $i/$max_attempts)${NC}"
        sleep 10
    done
    echo -e "${RED}❌ ${service_name} is not responding${NC}"
    return 1
}

# 檢查各服務健康狀態
check_service_health "Eureka Server" "http://localhost:8761/actuator/health" 5
check_service_health "Config Server" "http://localhost:8888/actuator/health" 5
check_service_health "API Gateway" "http://localhost:8080/actuator/health" 8
check_service_health "User Service" "http://localhost:8081/actuator/health" 8
check_service_health "Product Service" "http://localhost:8082/actuator/health" 8
check_service_health "Cart Service" "http://localhost:8083/actuator/health" 8
check_service_health "Inventory Service" "http://localhost:8085/actuator/health" 8

echo "=================================================================="
echo -e "${GREEN}🎉 All services have been started!${NC}"
echo ""
echo -e "${BLUE}📋 Service URLs:${NC}"
echo "- Eureka Dashboard: http://localhost:8761"
echo "- API Gateway: http://localhost:8080"
echo "- User Service: http://localhost:8081"
echo "- Product Service: http://localhost:8082" 
echo "- Cart Service: http://localhost:8083"
echo "- Inventory Service: http://localhost:8085"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo -e "${YELLOW}📝 Note: Due to slow network, services may take longer to fully initialize.${NC}"
echo -e "${YELLOW}    Please wait a few more minutes before testing the APIs.${NC}"
echo ""
echo -e "${BLUE}🔍 To check logs:${NC}"
echo "docker-compose logs -f [service-name]"
echo ""
echo -e "${BLUE}🛑 To stop all services:${NC}"
echo "./stop-infrastructure.sh"