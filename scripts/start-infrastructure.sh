#!/bin/bash

# E-commerce Platform 基礎設施啟動腳本

echo "🚀 Starting E-commerce Platform Infrastructure..."

# 檢查 Docker 是否運行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# 切換到基礎設施目錄
cd "$(dirname "$0")/../infrastructure" || exit 1

echo "📦 Starting infrastructure services..."

# 啟動基礎設施服務
docker-compose up -d

echo "⏳ Waiting for services to be ready..."

# 等待 PostgreSQL
echo "Waiting for PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo "✅ PostgreSQL is ready"

# 等待 Redis
echo "Waiting for Redis..."
until docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; do
    sleep 1
done
echo "✅ Redis is ready"

# 等待 Kafka
echo "Waiting for Kafka..."
sleep 30
echo "✅ Kafka should be ready"

echo "🎉 Infrastructure services started successfully!"
echo ""
echo "📊 Service Status:"
echo "- PostgreSQL: http://localhost:5432"
echo "- Redis: http://localhost:6379"
echo "- Kafka: http://localhost:9092"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "📝 Next steps:"
echo "1. Build and start Eureka Server: cd infrastructure/eureka-server && mvn spring-boot:run"
echo "2. Build and start Config Server: cd infrastructure/config-server && mvn spring-boot:run"
echo "3. Build and start API Gateway: cd infrastructure/api-gateway && mvn spring-boot:run"
echo ""
echo "🔍 Check service health:"
echo "docker-compose ps"
