#!/bin/bash

# E-commerce Platform 服務狀態檢查腳本
echo "🚀 E-commerce Platform 服務狀態檢查"
echo "=================================================="

# 檢查服務函數
check_service() {
    local service_name="$1"
    local url="$2"
    echo -n "檢查 $service_name ... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        echo "✅ 運行正常"
    else
        echo "❌ 服務異常 (HTTP $response)"
    fi
}

# 檢查各個服務
check_service "Eureka Server" "http://localhost:8761/actuator/health"
check_service "Config Server" "http://localhost:8888/actuator/health"
check_service "API Gateway" "http://localhost:8080/actuator/health"
check_service "User Service" "http://localhost:8081/api/v1/users/health"
check_service "Product Service" "http://localhost:8082/api/v1/products/health"
check_service "Cart Service" "http://localhost:8083/api/v1/carts/health"

echo "=================================================="

# 檢查通過 API Gateway 的服務訪問
echo "🔗 API Gateway 路由測試"
echo "=================================================="

check_service "User Service via Gateway" "http://localhost:8080/api/v1/users/health"
check_service "Product Service via Gateway" "http://localhost:8080/api/v1/products/health"
check_service "Cart Service via Gateway" "http://localhost:8080/api/v1/carts/health"

echo "=================================================="

# 顯示 Eureka 註冊的服務
echo "📋 Eureka 服務註冊狀態"
echo "=================================================="
registered_services=$(curl -s http://localhost:8761/eureka/apps | grep -o '<name>[^<]*</name>' | sed 's/<name>//g' | sed 's/<\/name>//g' | grep -v "MyOwn" | sort | uniq)

if [ -n "$registered_services" ]; then
    echo "$registered_services" | while read service; do
        echo "✅ $service"
    done
else
    echo "❌ 無法獲取服務註冊信息"
fi

echo "=================================================="
echo "✨ 服務狀態檢查完成"
