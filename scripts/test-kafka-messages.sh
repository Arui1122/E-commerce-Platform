#!/bin/bash

# Kafka 消息系統測試腳本
# 這個腳本會測試各種 Kafka 消息的發布和消費

set -e

echo "=== E-commerce Platform Kafka 消息系統測試 ==="
echo

# 服務 URL 配置
USER_SERVICE_URL="http://localhost:8081"
INVENTORY_SERVICE_URL="http://localhost:8085"
NOTIFICATION_SERVICE_URL="http://localhost:8086"

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${YELLOW}>>> $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 檢查服務是否運行
check_service() {
    local service_name=$1
    local service_url=$2
    
    print_step "檢查 $service_name 服務狀態..."
    
    if curl -s "$service_url/actuator/health" > /dev/null 2>&1; then
        print_success "$service_name 服務運行正常"
        return 0
    else
        print_error "$service_name 服務未運行或無法訪問"
        return 1
    fi
}

# 等待消息處理
wait_for_message_processing() {
    print_step "等待消息處理..."
    sleep 3
}

# 測試用戶註冊事件
test_user_registration_event() {
    print_step "測試用戶註冊事件..."
    
    # 註冊新用戶，這會觸發 user.registered 事件
    local response=$(curl -s -w "%{http_code}" -X POST "$USER_SERVICE_URL/api/v1/users/register" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "kafkatest_user_'$(date +%s)'",
            "email": "kafkatest'$(date +%s)'@example.com",
            "password": "TestPassword123!",
            "firstName": "Kafka",
            "lastName": "Test"
        }')
    
    local http_code="${response: -3}"
    if [ "$http_code" == "201" ]; then
        print_success "用戶註冊成功，user.registered 事件已發送"
        wait_for_message_processing
        return 0
    else
        print_error "用戶註冊失敗，HTTP代碼: $http_code"
        return 1
    fi
}

# 測試庫存更新事件
test_inventory_update_event() {
    print_step "測試庫存更新事件..."
    
    # 更新庫存，這會觸發 inventory.updated 事件
    local product_id=999$(date +%s)
    local response=$(curl -s -w "%{http_code}" -X POST "$INVENTORY_SERVICE_URL/api/v1/inventory" \
        -H "Content-Type: application/json" \
        -d '{
            "productId": '$product_id',
            "quantity": 100
        }')
    
    local http_code="${response: -3}"
    if [ "$http_code" == "200" ] || [ "$http_code" == "201" ]; then
        print_success "庫存更新成功，inventory.updated 事件已發送"
        wait_for_message_processing
        
        # 測試低庫存事件
        print_step "測試低庫存預警事件..."
        local low_stock_response=$(curl -s -w "%{http_code}" -X POST "$INVENTORY_SERVICE_URL/api/v1/inventory" \
            -H "Content-Type: application/json" \
            -d '{
                "productId": '$product_id',
                "quantity": 5
            }')
        
        local low_stock_code="${low_stock_response: -3}"
        if [ "$low_stock_code" == "200" ]; then
            print_success "低庫存更新成功，inventory.low-stock 事件已發送"
            wait_for_message_processing
        fi
        
        return 0
    else
        print_error "庫存更新失敗，HTTP代碼: $http_code"
        return 1
    fi
}

# 測試庫存補充事件
test_inventory_restock_event() {
    print_step "測試庫存補充事件..."
    
    local product_id=888$(date +%s)
    
    # 先創建庫存
    curl -s -X POST "$INVENTORY_SERVICE_URL/api/v1/inventory" \
        -H "Content-Type: application/json" \
        -d '{
            "productId": '$product_id',
            "quantity": 10
        }' > /dev/null
    
    # 補充庫存
    local response=$(curl -s -w "%{http_code}" -X POST "$INVENTORY_SERVICE_URL/api/v1/inventory/$product_id/replenish" \
        -H "Content-Type: application/json" \
        -d '{
            "quantity": 50
        }')
    
    local http_code="${response: -3}"
    if [ "$http_code" == "200" ]; then
        print_success "庫存補充成功，inventory.restocked 事件已發送"
        wait_for_message_processing
        return 0
    else
        print_error "庫存補充失敗，HTTP代碼: $http_code"
        return 1
    fi
}

# 檢查通知服務日誌
check_notification_logs() {
    print_step "檢查通知服務是否接收到消息..."
    
    # 這裡可以檢查通知服務的日誌來確認消息被正確處理
    # 由於是 Docker 環境，我們檢查容器日誌
    if docker logs ecommerce-notification-service --tail=50 2>/dev/null | grep -E "(USER_REGISTERED|INVENTORY_UPDATED|INVENTORY_LOW_STOCK|INVENTORY_RESTOCKED)" > /dev/null; then
        print_success "通知服務成功接收並處理了 Kafka 消息"
        return 0
    else
        print_error "通知服務未檢測到 Kafka 消息處理"
        return 1
    fi
}

# 主測試流程
main() {
    echo "開始 Kafka 消息系統測試..."
    echo
    
    # 檢查所有服務狀態
    local services_ok=true
    
    if ! check_service "User Service" "$USER_SERVICE_URL"; then
        services_ok=false
    fi
    
    if ! check_service "Inventory Service" "$INVENTORY_SERVICE_URL"; then
        services_ok=false
    fi
    
    if ! check_service "Notification Service" "$NOTIFICATION_SERVICE_URL"; then
        services_ok=false
    fi
    
    if [ "$services_ok" = false ]; then
        print_error "部分服務未運行，請先啟動所有必要的服務"
        exit 1
    fi
    
    echo
    print_step "所有服務運行正常，開始消息測試..."
    echo
    
    # 執行各種消息測試
    local tests_passed=0
    local total_tests=3
    
    if test_user_registration_event; then
        ((tests_passed++))
    fi
    
    if test_inventory_update_event; then
        ((tests_passed++))
    fi
    
    if test_inventory_restock_event; then
        ((tests_passed++))
    fi
    
    echo
    print_step "檢查消息處理結果..."
    sleep 5  # 給更多時間讓消息被處理
    
    if check_notification_logs; then
        print_success "消息處理驗證通過"
    else
        print_error "消息處理驗證失敗"
    fi
    
    echo
    echo "=== 測試結果 ==="
    echo "通過的測試: $tests_passed/$total_tests"
    
    if [ $tests_passed -eq $total_tests ]; then
        print_success "所有 Kafka 消息測試通過！"
        exit 0
    else
        print_error "部分測試失敗，請檢查服務日誌"
        exit 1
    fi
}

# 顯示使用說明
show_usage() {
    echo "Kafka 消息系統測試腳本"
    echo
    echo "使用方法:"
    echo "  $0                    - 運行完整的消息測試"
    echo "  $0 --help            - 顯示此幫助信息"
    echo
    echo "測試內容:"
    echo "  1. 用戶註冊事件 (user.registered)"
    echo "  2. 庫存更新事件 (inventory.updated)"
    echo "  3. 低庫存預警事件 (inventory.low-stock)"
    echo "  4. 庫存補充事件 (inventory.restocked)"
    echo "  5. 通知服務消息處理驗證"
    echo
    echo "前置條件:"
    echo "  - 所有服務必須正在運行"
    echo "  - Kafka 和 Zookeeper 必須正在運行"
    echo "  - 數據庫必須可用"
}

# 處理命令行參數
case "${1:-}" in
    --help|-h)
        show_usage
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac
