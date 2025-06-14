#!/bin/bash

# Kafka 消息系統完整測試腳本
# 用途: 測試 E-commerce Platform 的 Kafka 消息系統功能

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}===================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# 檢查必要的服務
check_services() {
    print_header "檢查服務狀態"
    
    # 檢查 Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安裝"
        exit 1
    fi
    print_success "Docker 可用"
    
    # 檢查 Kafka 容器
    if ! docker ps | grep -q "ecommerce-kafka"; then
        print_error "Kafka 容器未運行"
        exit 1
    fi
    print_success "Kafka 容器運行中"
    
    # 檢查 ZooKeeper 容器
    if ! docker ps | grep -q "ecommerce-zookeeper"; then
        print_error "ZooKeeper 容器未運行"
        exit 1
    fi
    print_success "ZooKeeper 容器運行中"
    
    # 檢查庫存服務
    if curl -s http://localhost:8085/api/v1/inventory/health > /dev/null; then
        print_success "庫存服務運行中"
    else
        print_error "庫存服務未運行"
        print_info "請先啟動庫存服務: cd services/inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
        exit 1
    fi
}

# 測試主題管理
test_topic_management() {
    print_header "測試 Kafka 主題管理"
    
    # 創建主題
    print_info "創建 Kafka 主題..."
    if ./scripts/manage-kafka-topics.sh create-all; then
        print_success "主題創建成功"
    else
        print_error "主題創建失敗"
        return 1
    fi
    
    # 列出主題
    print_info "列出所有主題..."
    if ./scripts/manage-kafka-topics.sh list > /dev/null; then
        print_success "主題列表獲取成功"
    else
        print_error "無法獲取主題列表"
        return 1
    fi
}

# 測試消息發佈和消費
test_messaging() {
    print_header "測試消息發佈和消費"
    
    # 測試庫存服務消息發佈
    print_info "測試庫存服務消息發佈..."
    
    # 在後台啟動消費者
    ./scripts/manage-kafka-topics.sh consume inventory.updated > /tmp/kafka_consume_test.log 2>&1 &
    CONSUMER_PID=$!
    
    # 等待消費者啟動
    sleep 8
    
    # 創建庫存記錄觸發消息
    RESPONSE=$(curl -s -X POST http://localhost:8085/api/v1/inventory \
        -H "Content-Type: application/json" \
        -d '{"productId": 999, "quantity": 50, "lowStockThreshold": 5}')
    
    if echo "$RESPONSE" | grep -q "999"; then
        print_success "庫存創建 API 調用成功"
    else
        print_error "庫存創建 API 調用失敗"
        kill $CONSUMER_PID 2>/dev/null || true
        return 1
    fi
    
    # 等待消息處理
    sleep 8
    
    # 檢查是否接收到消息
    if grep -q "INVENTORY_UPDATED" /tmp/kafka_consume_test.log; then
        print_success "Kafka 消息發佈和消費成功"
        print_info "接收到的消息:"
        grep "INVENTORY_UPDATED" /tmp/kafka_consume_test.log | tail -1
    else
        print_error "未接收到 Kafka 消息"
        kill $CONSUMER_PID 2>/dev/null || true
        return 1
    fi
    
    # 清理
    kill $CONSUMER_PID 2>/dev/null || true
    rm -f /tmp/kafka_consume_test.log
}

# 測試手動消息發送
test_manual_messaging() {
    print_header "測試手動消息發送"
    
    # 在後台啟動消費者
    ./scripts/manage-kafka-topics.sh consume user.registered > /tmp/kafka_manual_test.log 2>&1 &
    CONSUMER_PID=$!
    
    # 等待消費者啟動
    sleep 3
    
    # 發送測試消息
    TEST_MESSAGE='{"userId":12345,"username":"testuser","email":"test@example.com","timestamp":"2025-06-15T01:40:00"}'
    
    if ./scripts/manage-kafka-topics.sh test-send user.registered "$TEST_MESSAGE"; then
        print_success "測試消息發送成功"
    else
        print_error "測試消息發送失敗"
        kill $CONSUMER_PID 2>/dev/null || true
        return 1
    fi
    
    # 等待消息處理
    sleep 3
    
    # 檢查是否接收到消息
    if grep -q "testuser" /tmp/kafka_manual_test.log; then
        print_success "手動消息測試成功"
        print_info "接收到的消息:"
        grep "testuser" /tmp/kafka_manual_test.log | tail -1
    else
        print_error "未接收到手動發送的消息"
        kill $CONSUMER_PID 2>/dev/null || true
        return 1
    fi
    
    # 清理
    kill $CONSUMER_PID 2>/dev/null || true
    rm -f /tmp/kafka_manual_test.log
}

# 性能測試
test_performance() {
    print_header "簡單性能測試"
    
    print_info "發送多條消息測試性能..."
    
    # 記錄開始時間
    START_TIME=$(date +%s%N)
    
    # 發送 10 條消息
    for i in {1..10}; do
        TEST_MESSAGE="{\"messageId\":$i,\"content\":\"Performance test message $i\",\"timestamp\":\"$(date -Iseconds)\"}"
        ./scripts/manage-kafka-topics.sh test-send user.registered "$TEST_MESSAGE" > /dev/null
    done
    
    # 記錄結束時間
    END_TIME=$(date +%s%N)
    
    # 計算耗時（毫秒）
    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    
    print_success "發送 10 條消息耗時: ${DURATION}ms"
    print_info "平均每條消息耗時: $((DURATION / 10))ms"
}

# 生成測試報告
generate_report() {
    print_header "生成測試報告"
    
    REPORT_FILE="kafka_test_results_$(date +%Y%m%d_%H%M%S).txt"
    
    cat > "$REPORT_FILE" << EOF
Kafka 消息系統測試結果
====================================
測試時間: $(date)
測試主機: $(hostname)

測試項目:
✓ 服務狀態檢查
✓ Kafka 主題管理
✓ 消息發佈和消費
✓ 手動消息發送
✓ 基本性能測試

詳細結果請查看控制台輸出。

可用的 Kafka 主題:
$(./scripts/manage-kafka-topics.sh list 2>/dev/null | grep -E "^[a-z]" || echo "無法獲取主題列表")

測試完成 ✓
EOF
    
    print_success "測試報告已生成: $REPORT_FILE"
}

# 主測試流程
main() {
    print_header "Kafka 消息系統完整測試"
    print_info "開始測試 E-commerce Platform 的 Kafka 消息系統..."
    echo
    
    # 執行測試
    check_services
    echo
    
    test_topic_management
    echo
    
    test_messaging
    echo
    
    test_manual_messaging
    echo
    
    test_performance
    echo
    
    generate_report
    echo
    
    print_header "測試完成"
    print_success "所有 Kafka 消息系統測試通過!"
    print_info "Kafka 消息系統已準備好支援微服務通信"
}

# 錯誤處理
handle_error() {
    print_error "測試過程中發生錯誤"
    print_info "請檢查以上輸出並修復問題後重新運行測試"
    exit 1
}

# 設置錯誤處理
trap handle_error ERR

# 檢查參數
if [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
    echo "用法: $0 [選項]"
    echo "選項:"
    echo "  -h, --help    顯示此幫助信息"
    echo ""
    echo "此腳本會完整測試 Kafka 消息系統的各項功能。"
    echo "請確保以下服務正在運行:"
    echo "  - Docker (Kafka, ZooKeeper 容器)"
    echo "  - 庫存服務 (端口 8085)"
    exit 0
fi

# 運行主測試
main
