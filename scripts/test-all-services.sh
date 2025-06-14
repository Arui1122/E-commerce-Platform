#!/bin/bash

# E-commerce Platform - 綜合功能測試腳本
# 測試所有在 Order Service 之前的功能（針對慢網路優化）

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

API_GATEWAY="http://localhost:8080"

echo -e "${BLUE}🧪 E-commerce Platform Comprehensive Test (Pre-Order Service)${NC}"
echo "=================================================================="

# 等待函數（針對慢網路）
wait_for_response() {
    local url=$1
    local description=$2
    local max_attempts=${3:-10}
    local delay=${4:-5}
    
    echo -e "${YELLOW}⏳ Waiting for $description...${NC}"
    
    for i in $(seq 1 $max_attempts); do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $description is ready${NC}"
            return 0
        fi
        echo -e "${YELLOW}   Attempt $i/$max_attempts failed, waiting ${delay}s...${NC}"
        sleep $delay
    done
    
    echo -e "${RED}❌ $description failed to respond after $max_attempts attempts${NC}"
    return 1
}

# API 測試函數
test_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    local expected_status=${5:-200}
    
    echo -n -e "Testing $description... "
    
    local response
    local http_code
    
    if [[ "$method" == "GET" ]]; then
        response=$(curl -s -w "%{http_code}" "$endpoint" 2>/dev/null) || {
            echo -e "${RED}❌ Connection failed${NC}"
            return 1
        }
    else
        response=$(curl -s -w "%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "$endpoint" 2>/dev/null) || {
            echo -e "${RED}❌ Connection failed${NC}"
            return 1
        }
    fi
    
    http_code="${response: -3}"
    local body="${response%???}"
    
    if [[ "$http_code" == "$expected_status" ]]; then
        echo -e "${GREEN}✅ SUCCESS (HTTP $http_code)${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED (HTTP $http_code)${NC}"
        echo -e "${YELLOW}   Response: $body${NC}"
        return 1
    fi
}

# 主測試流程
main() {
    local failed_tests=0
    local total_tests=0
    
    echo -e "\n${BLUE}🔍 Step 1: Basic Health Checks${NC}"
    echo "----------------------------------------------------------"
    
    # 基本健康檢查
    ((total_tests++))
    if wait_for_response "$API_GATEWAY/actuator/health" "API Gateway" 10 5; then
        echo -e "${GREEN}✅ API Gateway is healthy${NC}"
    else
        echo -e "${RED}❌ API Gateway health check failed${NC}"
        ((failed_tests++))
    fi
    
    echo -e "\n${BLUE}👤 Step 2: User Service Tests${NC}"
    echo "----------------------------------------------------------"
    
    # 用戶註冊測試
    ((total_tests++))
    local user_data='{
        "username": "testuser",
        "email": "test@example.com",
        "password": "password123",
        "firstName": "Test",
        "lastName": "User"
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/users/register" "$user_data" "User Registration" 201; then
        echo -e "${GREEN}✅ User registration works${NC}"
    else
        echo -e "${RED}❌ User registration failed${NC}"
        ((failed_tests++))
    fi
    
    # 用戶登入測試
    ((total_tests++))
    local login_data='{
        "username": "testuser",
        "password": "password123"
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/users/login" "$login_data" "User Login" 200; then
        echo -e "${GREEN}✅ User login works${NC}"
        # 保存 JWT token 供後續測試使用
        JWT_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d "$login_data" "$API_GATEWAY/api/v1/users/login" | jq -r '.token' 2>/dev/null || echo "")
    else
        echo -e "${RED}❌ User login failed${NC}"
        ((failed_tests++))
    fi
    
    echo -e "\n${BLUE}📦 Step 3: Product Service Tests${NC}"
    echo "----------------------------------------------------------"
    
    # 創建商品分類
    ((total_tests++))
    local category_data='{
        "name": "Electronics",
        "description": "Electronic devices and gadgets"
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/categories" "$category_data" "Category Creation" 201; then
        echo -e "${GREEN}✅ Category creation works${NC}"
    else
        echo -e "${RED}❌ Category creation failed${NC}"
        ((failed_tests++))
    fi
    
    # 獲取商品分類列表
    ((total_tests++))
    if test_api "GET" "$API_GATEWAY/api/v1/categories" "" "Category List" 200; then
        echo -e "${GREEN}✅ Category listing works${NC}"
    else
        echo -e "${RED}❌ Category listing failed${NC}"
        ((failed_tests++))
    fi
    
    # 創建商品
    ((total_tests++))
    local product_data='{
        "name": "Test Laptop",
        "description": "A test laptop for testing purposes",
        "price": 999.99,
        "categoryId": 1,
        "brand": "TestBrand",
        "sku": "TEST-LAPTOP-001"
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/products" "$product_data" "Product Creation" 201; then
        echo -e "${GREEN}✅ Product creation works${NC}"
    else
        echo -e "${RED}❌ Product creation failed${NC}"
        ((failed_tests++))
    fi
    
    # 獲取商品列表
    ((total_tests++))
    if test_api "GET" "$API_GATEWAY/api/v1/products" "" "Product List" 200; then
        echo -e "${GREEN}✅ Product listing works${NC}"
    else
        echo -e "${RED}❌ Product listing failed${NC}"
        ((failed_tests++))
    fi
    
    # 商品搜索測試
    ((total_tests++))
    if test_api "GET" "$API_GATEWAY/api/v1/products/search?keyword=laptop" "" "Product Search" 200; then
        echo -e "${GREEN}✅ Product search works${NC}"
    else
        echo -e "${RED}❌ Product search failed${NC}"
        ((failed_tests++))
    fi
    
    echo -e "\n${BLUE}📦 Step 4: Inventory Service Tests${NC}"
    echo "----------------------------------------------------------"
    
    # 檢查庫存
    ((total_tests++))
    if test_api "GET" "$API_GATEWAY/api/v1/inventory/product/1" "" "Inventory Check" 200; then
        echo -e "${GREEN}✅ Inventory check works${NC}"
    else
        echo -e "${RED}❌ Inventory check failed${NC}"
        ((failed_tests++))
    fi
    
    # 庫存補充
    ((total_tests++))
    local inventory_data='{
        "productId": 1,
        "quantity": 100
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/inventory/restock" "$inventory_data" "Inventory Restock" 200; then
        echo -e "${GREEN}✅ Inventory restock works${NC}"
    else
        echo -e "${RED}❌ Inventory restock failed${NC}"
        ((failed_tests++))
    fi
    
    echo -e "\n${BLUE}🛒 Step 5: Cart Service Tests${NC}"
    echo "----------------------------------------------------------"
    
    # 添加商品到購物車
    ((total_tests++))
    local cart_data='{
        "productId": 1,
        "quantity": 2
    }'
    
    if test_api "POST" "$API_GATEWAY/api/v1/cart/testuser/items" "$cart_data" "Add to Cart" 200; then
        echo -e "${GREEN}✅ Add to cart works${NC}"
    else
        echo -e "${RED}❌ Add to cart failed${NC}"
        ((failed_tests++))
    fi
    
    # 獲取購物車內容
    ((total_tests++))
    if test_api "GET" "$API_GATEWAY/api/v1/cart/testuser" "" "Get Cart" 200; then
        echo -e "${GREEN}✅ Get cart works${NC}"
    else
        echo -e "${RED}❌ Get cart failed${NC}"
        ((failed_tests++))
    fi
    
    # 更新購物車商品數量
    ((total_tests++))
    local update_cart_data='{
        "quantity": 3
    }'
    
    if test_api "PUT" "$API_GATEWAY/api/v1/cart/testuser/items/1" "$update_cart_data" "Update Cart Item" 200; then
        echo -e "${GREEN}✅ Update cart item works${NC}"
    else
        echo -e "${RED}❌ Update cart item failed${NC}"
        ((failed_tests++))
    fi
    
    echo -e "\n${BLUE}🔄 Step 6: Service Integration Tests${NC}"
    echo "----------------------------------------------------------"
    
    # 測試服務間通信
    ((total_tests++))
    echo -n -e "Testing Service Discovery (Eureka)... "
    
    local eureka_apps=$(curl -s "http://localhost:8761/eureka/apps" -H "Accept: application/json" 2>/dev/null | jq -r '.applications.application | length' 2>/dev/null || echo "0")
    
    if [[ "$eureka_apps" -gt 0 ]]; then
        echo -e "${GREEN}✅ SUCCESS ($eureka_apps services registered)${NC}"
    else
        echo -e "${RED}❌ FAILED (No services registered)${NC}"
        ((failed_tests++))
    fi
    
    # 測試配置中心
    ((total_tests++))
    if test_api "GET" "http://localhost:8888/user-service/dev" "" "Config Server" 200; then
        echo -e "${GREEN}✅ Config Server works${NC}"
    else
        echo -e "${RED}❌ Config Server failed${NC}"
        ((failed_tests++))
    fi
    
    # 總結測試結果
    echo ""
    echo "=================================================================="
    echo -e "${BLUE}📊 Test Summary:${NC}"
    
    local passed_tests=$((total_tests - failed_tests))
    echo -e "Passed Tests: ${GREEN}$passed_tests${NC}/$total_tests"
    echo -e "Failed Tests: ${RED}$failed_tests${NC}/$total_tests"
    
    if [[ $failed_tests -eq 0 ]]; then
        echo -e "\n${GREEN}🎉 All tests passed! Ready to implement Order Service.${NC}"
        echo -e "${BLUE}💡 System is ready for Phase 2.5 - Order Service development.${NC}"
        exit 0
    else
        echo -e "\n${YELLOW}⚠️  Some tests failed. Please fix issues before proceeding.${NC}"
        echo -e "${BLUE}💡 Troubleshooting tips:${NC}"
        echo "1. Check service logs: docker-compose logs -f [service-name]"
        echo "2. Verify all containers are running: docker-compose ps"
        echo "3. Run health check: ./scripts/health-check.sh"
        echo "4. For slow networks, wait longer between tests"
        exit 1
    fi
}

# 檢查必要工具
check_prerequisites() {
    local missing_tools=()
    
    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi
    
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}⚠️  jq not found. Some tests may not work perfectly.${NC}"
    fi
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        echo -e "${RED}❌ Missing required tools: ${missing_tools[*]}${NC}"
        echo "Please install missing tools and try again."
        exit 1
    fi
}

# 執行檢查和測試
check_prerequisites
main "$@"
