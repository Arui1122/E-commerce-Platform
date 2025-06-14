#!/bin/bash

# Inventory Service Concurrency Test Script
# 庫存服務並發測試腳本 - 測試防超賣機制

BASE_URL="http://localhost:8085/api/v1/inventory"
PRODUCT_ID=999
INITIAL_STOCK=100
CONCURRENT_REQUESTS=20
QUANTITY_PER_REQUEST=10

echo "🚀 Inventory Service Concurrency Test"
echo "====================================="
echo "Product ID: $PRODUCT_ID"
echo "Initial Stock: $INITIAL_STOCK"
echo "Concurrent Requests: $CONCURRENT_REQUESTS"
echo "Quantity per Request: $QUANTITY_PER_REQUEST"
echo "Total Requested: $((CONCURRENT_REQUESTS * QUANTITY_PER_REQUEST))"
echo ""

# Setup: Create test product with initial stock
echo "📦 設置測試商品..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"productId\": $PRODUCT_ID,
    \"quantity\": $INITIAL_STOCK
  }" > /dev/null

echo "✅ 測試商品創建完成"
echo ""

# Check initial state
echo "📊 初始庫存狀態:"
curl -s -X GET "$BASE_URL/$PRODUCT_ID" | jq .
echo ""

# Create temporary directory for results
TEMP_DIR="/tmp/inventory_test_$$"
mkdir -p "$TEMP_DIR"

echo "🔥 開始並發測試..."
echo "每個請求嘗試預留 $QUANTITY_PER_REQUEST 個商品"
echo ""

# Function to make reservation request
reserve_stock() {
    local order_id=$1
    local result_file="$TEMP_DIR/result_$order_id.json"
    
    curl -s -X POST "$BASE_URL/reserve" \
      -H "Content-Type: application/json" \
      -d "{
        \"productId\": $PRODUCT_ID,
        \"quantity\": $QUANTITY_PER_REQUEST,
        \"referenceId\": \"CONCURRENT-ORDER-$order_id\"
      }" > "$result_file"
    
    # Extract the result
    local reserved=$(jq -r '.reserved' "$result_file" 2>/dev/null || echo "false")
    echo "Order $order_id: $reserved"
}

# Export function for parallel execution
export -f reserve_stock
export BASE_URL PRODUCT_ID QUANTITY_PER_REQUEST TEMP_DIR

# Start concurrent requests
echo "開始 $CONCURRENT_REQUESTS 個並發請求..."
seq 1 $CONCURRENT_REQUESTS | xargs -n 1 -P $CONCURRENT_REQUESTS -I {} bash -c 'reserve_stock {}'

echo ""
echo "⏱️  等待所有請求完成..."
sleep 2

# Analyze results
echo ""
echo "📈 結果分析:"
echo "============"

successful_reservations=0
failed_reservations=0

for i in $(seq 1 $CONCURRENT_REQUESTS); do
    result_file="$TEMP_DIR/result_$i.json"
    if [ -f "$result_file" ]; then
        reserved=$(jq -r '.reserved' "$result_file" 2>/dev/null || echo "false")
        if [ "$reserved" = "true" ]; then
            ((successful_reservations++))
        else
            ((failed_reservations++))
        fi
    else
        ((failed_reservations++))
    fi
done

echo "✅ 成功預留: $successful_reservations 個訂單"
echo "❌ 預留失敗: $failed_reservations 個訂單"
echo "📦 成功預留總數量: $((successful_reservations * QUANTITY_PER_REQUEST))"
echo ""

# Check final inventory state
echo "📊 最終庫存狀態:"
final_state=$(curl -s -X GET "$BASE_URL/$PRODUCT_ID")
echo "$final_state" | jq .

# Extract values for verification
total_quantity=$(echo "$final_state" | jq -r '.quantity')
reserved_quantity=$(echo "$final_state" | jq -r '.reservedQuantity')
available_quantity=$(echo "$final_state" | jq -r '.availableQuantity')

echo ""
echo "🔍 驗證結果:"
echo "============"
echo "初始庫存: $INITIAL_STOCK"
echo "總庫存: $total_quantity"
echo "預留數量: $reserved_quantity"
echo "可用數量: $available_quantity"

# Verify no overselling occurred
expected_reserved=$((successful_reservations * QUANTITY_PER_REQUEST))
if [ "$reserved_quantity" -eq "$expected_reserved" ]; then
    echo "✅ 預留數量正確: $reserved_quantity = $expected_reserved"
else
    echo "❌ 預留數量錯誤: $reserved_quantity ≠ $expected_reserved"
fi

if [ "$available_quantity" -eq $((total_quantity - reserved_quantity)) ]; then
    echo "✅ 可用數量計算正確"
else
    echo "❌ 可用數量計算錯誤"
fi

if [ "$total_quantity" -eq "$INITIAL_STOCK" ]; then
    echo "✅ 總庫存量未變: $total_quantity"
else
    echo "❌ 總庫存量發生變化: $total_quantity ≠ $INITIAL_STOCK"
fi

if [ "$reserved_quantity" -le "$INITIAL_STOCK" ]; then
    echo "✅ 防超賣機制有效: 預留數量 ($reserved_quantity) ≤ 初始庫存 ($INITIAL_STOCK)"
else
    echo "❌ 防超賣機制失效: 預留數量 ($reserved_quantity) > 初始庫存 ($INITIAL_STOCK)"
fi

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
if [ "$reserved_quantity" -le "$INITIAL_STOCK" ] && [ "$available_quantity" -eq $((total_quantity - reserved_quantity)) ]; then
    echo "🎉 並發測試通過！防超賣機制正常工作！"
else
    echo "💥 並發測試失敗！需要檢查防超賣機制！"
fi
