#!/bin/bash

# Kafka 主題管理腳本
# 用於創建和管理所有必要的 Kafka 主題

set -e

# Kafka 配置
KAFKA_CONTAINER="ecommerce-kafka"
KAFKA_BOOTSTRAP_SERVERS="localhost:9092"

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

# 定義所有需要的主題
TOPICS=(
    "user.registered:用戶註冊事件"
    "user.profile-updated:用戶資料更新事件"
    "user.password-reset:用戶密碼重置事件"
    "inventory.updated:庫存更新事件"
    "inventory.low-stock:低庫存預警事件"
    "inventory.out-of-stock:庫存不足事件"
    "inventory.restocked:庫存補充事件"
    "order.created:訂單創建事件"
    "order.confirmed:訂單確認事件"
    "order.shipped:訂單發貨事件"
    "order.delivered:訂單送達事件"
    "order.cancelled:訂單取消事件"
)

# 檢查 Kafka 是否運行
check_kafka() {
    print_step "檢查 Kafka 服務狀態..."
    
    if docker ps --format "table {{.Names}}" | grep -q "$KAFKA_CONTAINER"; then
        print_success "Kafka 容器運行中"
        return 0
    else
        print_error "Kafka 容器未運行，請先啟動基礎設施"
        return 1
    fi
}

# 創建主題
create_topic() {
    local topic_name=$1
    local description=$2
    
    print_step "創建主題: $topic_name ($description)"
    
    # 使用 docker exec 在 Kafka 容器中執行命令
    if docker exec "$KAFKA_CONTAINER" kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name" \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists > /dev/null 2>&1; then
        print_success "主題 $topic_name 創建成功"
        return 0
    else
        print_error "主題 $topic_name 創建失敗"
        return 1
    fi
}

# 列出所有主題
list_topics() {
    print_step "列出所有 Kafka 主題..."
    
    docker exec "$KAFKA_CONTAINER" kafka-topics --list --bootstrap-server localhost:9092
}

# 顯示主題詳情
describe_topic() {
    local topic_name=$1
    
    print_step "顯示主題詳情: $topic_name"
    
    docker exec "$KAFKA_CONTAINER" kafka-topics --describe \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name"
}

# 刪除主題
delete_topic() {
    local topic_name=$1
    
    print_step "刪除主題: $topic_name"
    
    if docker exec "$KAFKA_CONTAINER" kafka-topics --delete \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name" > /dev/null 2>&1; then
        print_success "主題 $topic_name 刪除成功"
        return 0
    else
        print_error "主題 $topic_name 刪除失敗"
        return 1
    fi
}

# 創建所有主題
create_all_topics() {
    print_step "創建所有必要的 Kafka 主題..."
    echo
    
    local created_count=0
    local total_count=${#TOPICS[@]}
    
    for topic_entry in "${TOPICS[@]}"; do
        IFS=':' read -r topic_name description <<< "$topic_entry"
        if create_topic "$topic_name" "$description"; then
            ((created_count++))
        fi
    done
    
    echo
    print_step "主題創建完成: $created_count/$total_count"
    
    if [ $created_count -eq $total_count ]; then
        print_success "所有主題創建成功！"
        return 0
    else
        print_error "部分主題創建失敗"
        return 1
    fi
}

# 刪除所有主題
delete_all_topics() {
    print_step "刪除所有主題..."
    echo
    
    local deleted_count=0
    local total_count=${#TOPICS[@]}
    
    for topic_entry in "${TOPICS[@]}"; do
        IFS=':' read -r topic_name description <<< "$topic_entry"
        if delete_topic "$topic_name"; then
            ((deleted_count++))
        fi
    done
    
    echo
    print_step "主題刪除完成: $deleted_count/$total_count"
}

# 監聽主題消息
consume_topic() {
    local topic_name=$1
    
    print_step "監聽主題消息: $topic_name"
    print_step "按 Ctrl+C 退出監聽"
    echo
    
    docker exec -it "$KAFKA_CONTAINER" kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name" \
        --from-beginning \
        --property print.key=true \
        --property key.separator=": "
}

# 發送測試消息
send_test_message() {
    local topic_name=$1
    local message=$2
    
    print_step "發送測試消息到主題: $topic_name"
    
    echo "$message" | docker exec -i "$KAFKA_CONTAINER" kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic "$topic_name"
    
    print_success "測試消息發送成功"
}

# 顯示使用說明
show_usage() {
    echo "Kafka 主題管理腳本"
    echo
    echo "使用方法:"
    echo "  $0 create-all          - 創建所有必要的主題"
    echo "  $0 list               - 列出所有主題"
    echo "  $0 describe <topic>   - 顯示主題詳情"
    echo "  $0 delete <topic>     - 刪除指定主題"
    echo "  $0 delete-all         - 刪除所有主題"
    echo "  $0 consume <topic>    - 監聽主題消息"
    echo "  $0 test-send <topic> <message> - 發送測試消息"
    echo "  $0 --help             - 顯示此幫助信息"
    echo
    echo "預定義的主題:"
    for topic_entry in "${TOPICS[@]}"; do
        IFS=':' read -r topic_name description <<< "$topic_entry"
        echo "  - $topic_name: $description"
    done
}

# 主函數
main() {
    case "${1:-}" in
        create-all)
            if check_kafka; then
                create_all_topics
            fi
            ;;
        list)
            if check_kafka; then
                list_topics
            fi
            ;;
        describe)
            if [ -z "${2:-}" ]; then
                echo "錯誤: 請提供主題名稱"
                exit 1
            fi
            if check_kafka; then
                describe_topic "$2"
            fi
            ;;
        delete)
            if [ -z "${2:-}" ]; then
                echo "錯誤: 請提供主題名稱"
                exit 1
            fi
            if check_kafka; then
                delete_topic "$2"
            fi
            ;;
        delete-all)
            if check_kafka; then
                echo "警告: 這將刪除所有主題！"
                read -p "確定要繼續嗎？(y/N): " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    delete_all_topics
                else
                    echo "操作已取消"
                fi
            fi
            ;;
        consume)
            if [ -z "${2:-}" ]; then
                echo "錯誤: 請提供主題名稱"
                exit 1
            fi
            if check_kafka; then
                consume_topic "$2"
            fi
            ;;
        test-send)
            if [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
                echo "錯誤: 請提供主題名稱和消息內容"
                exit 1
            fi
            if check_kafka; then
                send_test_message "$2" "$3"
            fi
            ;;
        --help|-h)
            show_usage
            ;;
        "")
            show_usage
            ;;
        *)
            echo "錯誤: 未知命令 '$1'"
            echo
            show_usage
            exit 1
            ;;
    esac
}

main "$@"
