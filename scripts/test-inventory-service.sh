#!/bin/bash

# Inventory Service API Test Script
# 庫存服務 API 測試腳本

BASE_URL="http://localhost:8085/api/v1/inventory"

echo "🧪 Inventory Service API Tests"
echo "=============================="

# Health Check
echo "1. 健康檢查..."
curl -s -X GET "$BASE_URL/health" | jq .
echo ""

# Create Inventory
echo "2. 創建庫存 (Product ID: 1, Quantity: 100)..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 100
  }' | jq .
echo ""

# Get Inventory
echo "3. 查詢庫存 (Product ID: 1)..."
curl -s -X GET "$BASE_URL/1" | jq .
echo ""

# Check Stock
echo "4. 檢查庫存 (Product ID: 1, Quantity: 50)..."
curl -s -X GET "$BASE_URL/check/1?quantity=50" | jq .
echo ""

# Reserve Stock
echo "5. 預留庫存 (Product ID: 1, Quantity: 30)..."
curl -s -X POST "$BASE_URL/reserve" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 30,
    "referenceId": "ORDER-123"
  }' | jq .
echo ""

# Check Updated Inventory
echo "6. 查詢更新後的庫存..."
curl -s -X GET "$BASE_URL/1" | jq .
echo ""

# Try to Reserve Too Much Stock (Should fail)
echo "7. 嘗試預留過多庫存 (應該失敗)..."
curl -s -X POST "$BASE_URL/reserve" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 100,
    "referenceId": "ORDER-124"
  }' | jq .
echo ""

# Release Reserved Stock
echo "8. 釋放預留庫存 (Quantity: 10)..."
curl -s -X POST "$BASE_URL/1/release?quantity=10" | jq .
echo ""

# Confirm Reserved Stock
echo "9. 確認預留庫存 (Quantity: 15)..."
curl -s -X POST "$BASE_URL/1/confirm?quantity=15" | jq .
echo ""

# Check Final Inventory
echo "10. 查詢最終庫存狀態..."
curl -s -X GET "$BASE_URL/1" | jq .
echo ""

# Replenish Stock
echo "11. 補充庫存 (Quantity: 50)..."
curl -s -X POST "$BASE_URL/1/replenish?quantity=50" | jq .
echo ""

# Create Low Stock Product
echo "12. 創建低庫存商品 (Product ID: 2, Quantity: 5)..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 2,
    "quantity": 5
  }' | jq .
echo ""

# Get Low Stock Products
echo "13. 查詢低庫存商品 (minQuantity: 10)..."
curl -s -X GET "$BASE_URL/low-stock?minQuantity=10" | jq .
echo ""

# Batch Get Inventories
echo "14. 批量查詢庫存 (Product IDs: [1, 2, 3])..."
curl -s -X POST "$BASE_URL/batch" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]' | jq .
echo ""

echo "✅ 測試完成！"
