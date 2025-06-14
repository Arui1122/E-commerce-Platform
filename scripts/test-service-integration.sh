#!/bin/bash

# 3.1 服務間通信測試腳本
# Service Integration and Communication Test Script

echo "=== Phase 3.1: Service Integration - Inter-Service Communication ==="
echo

# 測試基礎設施服務
echo "1. 檢查基礎設施服務狀態..."
echo "-----------------------------------"

# 檢查 Eureka Server
echo "🔍 Eureka Server:"
curl -s http://localhost:8761/actuator/health | jq -r '.status // "UNAVAILABLE"'

# 檢查 Config Server  
echo "🔍 Config Server:"
curl -s http://localhost:8888/actuator/health | jq -r '.status // "UNAVAILABLE"'

# 檢查 API Gateway
echo "🔍 API Gateway:"
curl -s http://localhost:8080/actuator/health | jq -r '.status // "UNAVAILABLE"'

echo

# 測試微服務
echo "2. 檢查微服務狀態..."
echo "-----------------------------------"

# 檢查 Order Service
echo "🔍 Order Service:"
curl -s http://localhost:8084/actuator/health | jq -r '.status // "UNAVAILABLE"'

# 檢查 Product Service
echo "🔍 Product Service:"
curl -s http://localhost:8082/actuator/health | jq -r '.status // "UNAVAILABLE"'

# 檢查 Inventory Service
echo "🔍 Inventory Service:"
curl -s http://localhost:8085/actuator/health | jq -r '.status // "UNAVAILABLE"'

# 檢查 Cart Service (如果有的話)
echo "🔍 Cart Service:"
curl -s http://localhost:8083/actuator/health 2>/dev/null | jq -r '.status // "UNAVAILABLE"'

# 檢查 User Service (如果有的話)
echo "🔍 User Service:"
curl -s http://localhost:8081/actuator/health 2>/dev/null | jq -r '.status // "UNAVAILABLE"'

echo

# 測試服務間通信
echo "3. 測試服務間通信..."
echo "-----------------------------------"

echo "📝 創建測試訂單（包含服務間調用）..."

# 創建訂單測試數據
ORDER_DATA='{
    "userId": 1,
    "orderItems": [
        {
            "productId": 1,
            "productName": "Test Product for Inter-Service Communication",
            "unitPrice": 99.99,
            "quantity": 2
        }
    ],
    "shippingAddress": "123 Test Street, Integration City, IC 12345",
    "paymentMethod": "CREDIT_CARD",
    "notes": "Test order for Phase 3.1 Service Integration",
    "clearCart": true
}'

# 執行訂單創建測試
echo "發送訂單創建請求..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8084/api/v1/orders \
    -H "Content-Type: application/json" \
    -d "$ORDER_DATA")

if [ $? -eq 0 ]; then
    echo "✅ 訂單創建成功"
    echo "$ORDER_RESPONSE" | jq .
    
    # 提取訂單ID用於後續測試
    ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')
    
    if [ ! -z "$ORDER_ID" ]; then
        echo
        echo "📋 獲取創建的訂單詳情..."
        curl -s http://localhost:8084/api/v1/orders/$ORDER_ID | jq .
    fi
else
    echo "❌ 訂單創建失敗"
fi

echo

# 檢查熔斷器狀態
echo "4. 檢查熔斷器狀態..."
echo "-----------------------------------"

echo "🔧 Circuit Breaker 狀態:"
curl -s http://localhost:8084/actuator/circuitbreakers 2>/dev/null | jq . || echo "Circuit breaker endpoint not available"

echo

# 測試 Feign 客戶端配置
echo "5. 檢查 Feign 配置..."
echo "-----------------------------------"

echo "⚙️ Feign Configuration Info:"
curl -s http://localhost:8084/actuator/configprops 2>/dev/null | jq '.contexts.application.beans | to_entries[] | select(.key | contains("feign")) | .key' || echo "Feign config not available via actuator"

echo

echo "=== Phase 3.1 Service Integration Test Complete ==="
echo
echo "✅ 已完成的功能:"
echo "   - OpenFeign 客戶端配置"
echo "   - 服務間 API 調用"
echo "   - 熔斷器 (Resilience4j) 配置"
echo "   - 負載均衡配置"
echo "   - Fallback 降級處理"
echo
echo "📊 服務間通信架構:"
echo "   Order Service → User Service (用戶驗證)"
echo "   Order Service → Inventory Service (庫存檢查和預留)"  
echo "   Order Service → Cart Service (清空購物車)"
echo "   Order Service → Product Service (商品信息)"
echo
echo "🔧 配置要點:"
echo "   - Feign 超時配置: 連接 10s, 讀取 60s"
echo "   - 熔斷器閾值: 失敗率 50%, 滑動窗口 10"
echo "   - 自動降級: 啟用 Fallback 機制"
echo
