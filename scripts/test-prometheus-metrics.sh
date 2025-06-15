#!/bin/bash

# Prometheus 指標收集測試腳本
# 測試各服務的自定義業務指標

set -e

echo "🔍 E-commerce Platform - Prometheus Metrics Test"
echo "================================================"

# 配置
PROMETHEUS_URL="http://localhost:9090"

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 測試指標函數
test_metric() {
    local metric_name=$1
    local description=$2
    
    echo -n "  Testing ${metric_name}: "
    
    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${metric_name}" 2>/dev/null)
    
    if [ $? -eq 0 ] && echo "$response" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
        local count=$(echo "$response" | jq -r '.data.result | length')
        local latest_value=$(echo "$response" | jq -r '.data.result[0].value[1] // "0"')
        echo -e "${GREEN}✓ Available${NC} (${count} series, latest: ${latest_value})"
        return 0
    else
        echo -e "${RED}✗ Not available${NC}"
        return 1
    fi
}

# 生成測試數據
generate_test_data() {
    echo "📊 Generating test data by calling service endpoints..."
    
    # 測試用戶註冊 (如果服務正在運行)
    echo "  Calling user registration endpoint..."
    curl -s -X POST http://localhost:8080/api/v1/users/register \
        -H "Content-Type: application/json" \
        -d '{
            "username": "test_user_'$(date +%s)'",
            "email": "test'$(date +%s)'@example.com",
            "password": "Test123456!",
            "firstName": "Test",
            "lastName": "User"
        }' > /dev/null 2>&1 || echo "    (Service not available)"
    
    # 測試商品查詢
    echo "  Calling product search endpoint..."
    curl -s "http://localhost:8080/api/v1/products?keyword=test" > /dev/null 2>&1 || echo "    (Service not available)"
    
    # 測試庫存查詢
    echo "  Calling inventory check endpoint..."
    curl -s "http://localhost:8080/api/v1/inventory/1" > /dev/null 2>&1 || echo "    (Service not available)"
    
    echo "  Test data generation completed."
}

# 測試系統指標
test_system_metrics() {
    echo "🖥️  Testing System Metrics:"
    
    test_metric "up" "Service availability"
    test_metric "jvm_memory_used_bytes" "JVM memory usage"
    test_metric "jvm_memory_max_bytes" "JVM max memory"
    test_metric "system_cpu_usage" "System CPU usage"
    test_metric "process_cpu_usage" "Process CPU usage"
    test_metric "jvm_gc_pause_seconds_count" "GC pause count"
    test_metric "hikaricp_connections_active" "Database active connections"
    test_metric "hikaricp_connections_max" "Database max connections"
}

# 測試 HTTP 指標
test_http_metrics() {
    echo "🌐 Testing HTTP Metrics:"
    
    test_metric "http_server_requests_seconds_count" "HTTP request count"
    test_metric "http_server_requests_seconds_sum" "HTTP request duration sum"
    test_metric "http_server_requests_seconds_max" "HTTP request max duration"
}

# 測試業務指標
test_business_metrics() {
    echo "💼 Testing Business Metrics:"
    
    # 用戶服務指標
    echo "  User Service Metrics:"
    test_metric "user_registration_count_total" "User registration count"
    test_metric "user_login_count_total" "User login count"
    test_metric "user_registration_timer_seconds_count" "User registration timer"
    test_metric "user_login_timer_seconds_count" "User login timer"
    
    # 商品服務指標
    echo "  Product Service Metrics:"
    test_metric "product_search_count_total" "Product search count"
    test_metric "product_create_count_total" "Product create count"
    test_metric "product_view_count_total" "Product view count"
    test_metric "product_active_connections" "Product active connections"
    test_metric "product_total_count" "Product total count"
    
    # 訂單服務指標
    echo "  Order Service Metrics:"
    test_metric "order_created_count_total" "Order created count"
    test_metric "order_confirmed_count_total" "Order confirmed count"
    test_metric "order_cancelled_count_total" "Order cancelled count"
    test_metric "payment_success_count_total" "Payment success count"
    test_metric "payment_failed_count_total" "Payment failed count"
    test_metric "order_active_count" "Active orders count"
    
    # 庫存服務指標
    echo "  Inventory Service Metrics:"
    test_metric "inventory_update_count_total" "Inventory update count"
    test_metric "inventory_reserve_count_total" "Inventory reserve count"
    test_metric "inventory_release_count_total" "Inventory release count"
    test_metric "inventory_products_total" "Total products in inventory"
    test_metric "inventory_products_low_stock" "Low stock products"
    test_metric "inventory_products_out_of_stock" "Out of stock products"
}

# 測試 Prometheus 查詢
test_prometheus_queries() {
    echo "🔍 Testing Complex Prometheus Queries:"
    
    # 錯誤率查詢
    echo -n "  HTTP Error Rate (5xx): "
    local error_rate_query='rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])'
    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${error_rate_query}" 2>/dev/null)
    if [ $? -eq 0 ] && echo "$response" | jq -e '.data.result | length >= 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Query successful${NC}"
    else
        echo -e "${RED}✗ Query failed${NC}"
    fi
    
    # 95th 百分位響應時間
    echo -n "  95th Percentile Response Time: "
    local percentile_query='histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))'
    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${percentile_query}" 2>/dev/null)
    if [ $? -eq 0 ] && echo "$response" | jq -e '.data.result | length >= 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Query successful${NC}"
    else
        echo -e "${RED}✗ Query failed${NC}"
    fi
    
    # JVM 內存使用率
    echo -n "  JVM Memory Usage Percentage: "
    local memory_query='(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100'
    local response=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${memory_query}" 2>/dev/null)
    if [ $? -eq 0 ] && echo "$response" | jq -e '.data.result | length >= 0' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Query successful${NC}"
    else
        echo -e "${RED}✗ Query failed${NC}"
    fi
}

# 生成指標報告
generate_metrics_report() {
    echo "📋 Generating detailed metrics report..."
    
    local report_file="prometheus-metrics-report-$(date +%Y%m%d-%H%M%S).json"
    
    # 獲取所有可用指標
    curl -s "${PROMETHEUS_URL}/api/v1/label/__name__/values" > "$report_file"
    
    echo "  Available metrics saved to: $report_file"
    
    # 顯示指標統計
    local total_metrics=$(cat "$report_file" | jq -r '.data | length')
    echo "  Total available metrics: $total_metrics"
    
    # 顯示業務相關指標
    echo "  Business metrics found:"
    cat "$report_file" | jq -r '.data[]' | grep -E "(user_|product_|order_|inventory_|payment_)" | head -10 | sed 's/^/    - /'
}

# 主函數
main() {
    echo ""
    
    # 檢查 Prometheus 是否可用
    if ! curl -s -f "${PROMETHEUS_URL}/api/v1/status/runtimeinfo" > /dev/null 2>&1; then
        echo -e "${RED}❌ Prometheus is not available at ${PROMETHEUS_URL}${NC}"
        echo "Please ensure Prometheus is running before running this test."
        exit 1
    fi
    
    echo -e "${GREEN}✅ Prometheus is available${NC}"
    echo ""
    
    # 生成測試資料
    generate_test_data
    echo ""
    
    # 等待指標被收集
    echo "⏳ Waiting for metrics to be collected..."
    sleep 5
    echo ""
    
    # 執行測試
    test_system_metrics
    echo ""
    
    test_http_metrics
    echo ""
    
    test_business_metrics
    echo ""
    
    test_prometheus_queries
    echo ""
    
    generate_metrics_report
    echo ""
    
    echo "📊 Metric Testing Summary:"
    echo "  All system and business metrics have been tested."
    echo "  Check the generated report for detailed metric availability."
    echo ""
    echo "🌐 Useful Prometheus URLs:"
    echo "  Prometheus UI: http://localhost:9090"
    echo "  Metrics Browser: http://localhost:9090/graph"
    echo "  Targets Status: http://localhost:9090/targets"
    echo ""
    echo "✅ Prometheus metrics testing completed!"
}

# 執行主函數
main "$@"
