#!/bin/bash

# Saga 分散式交易測試腳本
# 測試訂單創建的 Saga 模式

echo "========================================"
echo "Saga 分散式交易測試"
echo "========================================"

# 服務基礎 URL
ORDER_SERVICE_URL="http://localhost:8084/api/v1/orders"

# 測試數據
USER_ID=1
PRODUCT_ID=1
QUANTITY=2

echo "1. 測試使用 Saga 模式創建訂單..."
SAGA_ORDER_RESPONSE=$(curl -s -X POST "$ORDER_SERVICE_URL/saga" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "orderItems": [
      {
        "productId": 1,
        "productName": "測試商品",
        "unitPrice": 99.99,
        "quantity": 2
      }
    ],
    "shippingAddress": "台北市信義區信義路五段7號",
    "paymentMethod": "信用卡",
    "notes": "測試訂單 - Saga 模式",
    "clearCart": true
  }')

echo "Saga 訂單創建回應:"
echo "$SAGA_ORDER_RESPONSE" | jq '.'

# 檢查回應狀態
STATUS=$(echo "$SAGA_ORDER_RESPONSE" | jq -r '.status')
if [ "$STATUS" = "ACCEPTED" ]; then
    echo "✅ Saga 訂單創建請求已被接受"
else
    echo "❌ Saga 訂單創建失敗"
fi

echo ""
echo "2. 測試傳統模式創建訂單進行比較..."
TRADITIONAL_ORDER_RESPONSE=$(curl -s -X POST "$ORDER_SERVICE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "orderItems": [
      {
        "productId": 1,
        "productName": "測試商品",
        "unitPrice": 99.99,
        "quantity": 1
      }
    ],
    "shippingAddress": "台北市信義區信義路五段7號",
    "paymentMethod": "信用卡",
    "notes": "測試訂單 - 傳統模式",
    "clearCart": false
  }')

echo "傳統訂單創建回應:"
echo "$TRADITIONAL_ORDER_RESPONSE" | jq '.'

echo ""
echo "3. 檢查創建的訂單..."
sleep 2  # 等待 Saga 處理完成

# 獲取使用者的所有訂單
USER_ORDERS=$(curl -s "$ORDER_SERVICE_URL/user/1")
echo "使用者訂單列表:"
echo "$USER_ORDERS" | jq '.'

echo ""
echo "========================================"
echo "Saga 測試完成"
echo "========================================"

echo ""
echo "監控建議："
echo "1. 檢查日誌檔案查看 Saga 執行流程"
echo "2. 檢查 Kafka 主題的訊息"
echo "3. 檢查資料庫中的訂單狀態"
echo "4. 檢查庫存是否正確扣減"
