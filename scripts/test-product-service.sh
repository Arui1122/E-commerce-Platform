#!/bin/bash

echo "🧪 Product Service API 測試腳本"
echo "================================="

BASE_URL="http://localhost:8082"

# 檢查服務健康狀態
echo "1. 檢查服務健康狀態..."
curl -s "${BASE_URL}/actuator/health" | jq '.'

echo -e "\n2. 獲取所有分類..."
curl -s "${BASE_URL}/api/v1/categories" | jq '.'

echo -e "\n3. 獲取所有商品..."
curl -s "${BASE_URL}/api/v1/products?page=0&size=5" | jq '.content[] | {id, name, price, category}'

echo -e "\n4. 搜索商品 (iPhone)..."
curl -s "${BASE_URL}/api/v1/products/search?name=iPhone" | jq '.content[] | {id, name, price}'

echo -e "\n5. 獲取熱門商品..."
curl -s "${BASE_URL}/api/v1/products/popular" | jq '.[] | {id, name, viewCount}'

echo -e "\n6. 檢查 API 文檔..."
curl -s "${BASE_URL}/v3/api-docs" > /dev/null && echo "✅ OpenAPI 文檔可用" || echo "❌ OpenAPI 文檔不可用"

echo -e "\n✅ 測試完成！"
