#!/bin/bash

# Order Service API 測試腳本

echo "=== Order Service API 測試 ==="
echo ""

BASE_URL="http://localhost:8084/api/v1/orders"

# 健康檢查
echo "1. 健康檢查..."
curl -s $BASE_URL/health
echo -e "\n"

# 創建訂單
echo "2. 創建訂單..."
ORDER_RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "orderItems": [
      {
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "productSku": "IPHONE15PRO-256",
        "unitPrice": 1299.99,
        "quantity": 1
      },
      {
        "productId": 2,
        "productName": "AirPods Pro",
        "productSku": "AIRPODSPRO-2ND",
        "unitPrice": 249.99,
        "quantity": 2
      }
    ],
    "shippingAddress": "456 Tech St, San Francisco, CA 94102",
    "paymentMethod": "CREDIT_CARD",
    "notes": "測試訂單 - 請小心處理"
  }')

echo $ORDER_RESPONSE | jq '.'
ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')
ORDER_NUMBER=$(echo $ORDER_RESPONSE | jq -r '.orderNumber')
echo "訂單 ID: $ORDER_ID"
echo "訂單號: $ORDER_NUMBER"
echo ""

# 獲取訂單
echo "3. 根據 ID 獲取訂單..."
curl -s $BASE_URL/$ORDER_ID | jq '.'
echo ""

echo "4. 根據訂單號獲取訂單..."
curl -s $BASE_URL/number/$ORDER_NUMBER | jq '.'
echo ""

# 更新訂單狀態為已付款
echo "5. 更新訂單狀態為已付款..."
curl -s -X PUT $BASE_URL/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status": "paid"}' | jq '.'
echo ""

# 處理支付
echo "6. 處理支付..."
curl -s -X POST $BASE_URL/$ORDER_ID/payment \
  -H "Content-Type: application/json" \
  -d '{"paymentDetails": "信用卡 **** 1234 支付成功"}'
echo -e "\n"

# 更新訂單狀態為處理中
echo "7. 更新訂單狀態為處理中..."
curl -s -X PUT $BASE_URL/$ORDER_ID/status \
  -H "Content-Type: application/json" \
  -d '{"status": "processing"}' | jq '.'
echo ""

# 獲取用戶訂單
echo "8. 獲取用戶 123 的所有訂單..."
curl -s $BASE_URL/user/123 | jq '.'
echo ""

# 根據狀態查詢訂單
echo "9. 查詢處理中的訂單..."
curl -s $BASE_URL/status/processing | jq '.'
echo ""

echo "=== 測試完成 ==="
