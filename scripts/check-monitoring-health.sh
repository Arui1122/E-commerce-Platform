#!/bin/bash

# E-commerce Platform 監控系統健康檢查腳本
# 檢查 Prometheus 和各服務的監控端點

set -e

echo "🔍 E-commerce Platform - Monitoring Health Check"
echo "================================================"

# 配置
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"

# 服務列表和端口
declare -A SERVICES=(
    ["eureka-server"]="8761"
    ["config-server"]="8888"
    ["api-gateway"]="8080"
    ["user-service"]="8081"
    ["product-service"]="8082"
    ["cart-service"]="8083"
    ["order-service"]="8084"
    ["inventory-service"]="8085"
    ["notification-service"]="8086"
)

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 健康檢查函數
check_service_health() {
    local service_name=$1
    local port=$2
    local url="http://localhost:${port}/actuator/health"
    
    echo -n "  ${service_name}: "
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ HEALTHY${NC}"
        return 0
    else
        echo -e "${RED}✗ UNHEALTHY${NC}"
        return 1
    fi
}

# 檢查 Prometheus 端點
check_prometheus_metrics() {
    local service_name=$1
    local port=$2
    local url="http://localhost:${port}/actuator/prometheus"
    
    echo -n "  ${service_name} metrics: "
    
    if curl -s -f "$url" | grep -q "jvm_memory_used_bytes"; then
        echo -e "${GREEN}✓ AVAILABLE${NC}"
        return 0
    else
        echo -e "${RED}✗ UNAVAILABLE${NC}"
        return 1
    fi
}

# 檢查 Prometheus 服務
check_prometheus_service() {
    echo -n "Prometheus Service: "
    
    if curl -s -f "${PROMETHEUS_URL}/api/v1/status/runtimeinfo" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ RUNNING${NC}"
        return 0
    else
        echo -e "${RED}✗ DOWN${NC}"
        return 1
    fi
}

# 檢查 Grafana 服務
check_grafana_service() {
    echo -n "Grafana Service: "
    
    if curl -s -f "${GRAFANA_URL}/api/health" | grep -q "ok"; then
        echo -e "${GREEN}✓ RUNNING${NC}"
        return 0
    else
        echo -e "${RED}✗ DOWN${NC}"
        return 1
    fi
}

# 檢查 Prometheus 目標狀態
check_prometheus_targets() {
    echo "📊 Checking Prometheus Targets Status:"
    
    local targets_response=$(curl -s "${PROMETHEUS_URL}/api/v1/targets" 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        local up_count=$(echo "$targets_response" | jq -r '.data.activeTargets[] | select(.health=="up") | .scrapeUrl' | wc -l)
        local total_count=$(echo "$targets_response" | jq -r '.data.activeTargets[] | .scrapeUrl' | wc -l)
        
        echo "  Active Targets: ${up_count}/${total_count}"
        
        if [ "$up_count" -eq "$total_count" ]; then
            echo -e "  Status: ${GREEN}All targets healthy${NC}"
        else
            echo -e "  Status: ${YELLOW}Some targets unhealthy${NC}"
            echo "  Unhealthy targets:"
            echo "$targets_response" | jq -r '.data.activeTargets[] | select(.health!="up") | "    - " + .labels.job + " (" + .scrapeUrl + "): " + .lastError'
        fi
    else
        echo -e "  ${RED}Failed to query Prometheus targets${NC}"
    fi
}

# 檢查關鍵指標
check_key_metrics() {
    echo "📈 Checking Key Metrics Availability:"
    
    local metrics=(
        "up"
        "jvm_memory_used_bytes"
        "http_server_requests_seconds_count"
        "user_registration_count_total"
        "order_created_count_total"
        "inventory_update_count_total"
    )
    
    for metric in "${metrics[@]}"; do
        echo -n "  ${metric}: "
        
        local query_response=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=${metric}" 2>/dev/null)
        
        if [ $? -eq 0 ] && echo "$query_response" | jq -e '.data.result | length > 0' > /dev/null 2>&1; then
            local result_count=$(echo "$query_response" | jq -r '.data.result | length')
            echo -e "${GREEN}✓ Available (${result_count} series)${NC}"
        else
            echo -e "${RED}✗ Not available${NC}"
        fi
    done
}

# 生成監控報告
generate_monitoring_report() {
    echo "📋 Generating Monitoring Report..."
    
    local report_file="monitoring-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "E-commerce Platform - Monitoring Report"
        echo "Generated at: $(date)"
        echo "========================================"
        echo ""
        
        echo "Service Health Status:"
        for service in "${!SERVICES[@]}"; do
            port=${SERVICES[$service]}
            if check_service_health "$service" "$port" >/dev/null 2>&1; then
                echo "  $service: HEALTHY"
            else
                echo "  $service: UNHEALTHY"
            fi
        done
        
        echo ""
        echo "Monitoring Infrastructure:"
        if check_prometheus_service >/dev/null 2>&1; then
            echo "  Prometheus: RUNNING"
        else
            echo "  Prometheus: DOWN"
        fi
        
        if check_grafana_service >/dev/null 2>&1; then
            echo "  Grafana: RUNNING"
        else
            echo "  Grafana: DOWN"
        fi
        
        echo ""
        echo "Key Metrics Summary:"
        curl -s "${PROMETHEUS_URL}/api/v1/query?query=up" | jq -r '.data.result[] | "  " + .metric.job + ": " + .value[1]'
        
    } > "$report_file"
    
    echo "  Report saved to: $report_file"
}

# 主函數
main() {
    echo ""
    echo "🏥 Service Health Check:"
    
    local healthy_services=0
    local total_services=${#SERVICES[@]}
    
    for service in "${!SERVICES[@]}"; do
        port=${SERVICES[$service]}
        if check_service_health "$service" "$port"; then
            ((healthy_services++))
        fi
    done
    
    echo ""
    echo "📊 Metrics Endpoint Check:"
    
    for service in "${!SERVICES[@]}"; do
        port=${SERVICES[$service]}
        check_prometheus_metrics "$service" "$port"
    done
    
    echo ""
    echo "🎯 Monitoring Infrastructure:"
    check_prometheus_service
    check_grafana_service
    
    echo ""
    check_prometheus_targets
    
    echo ""
    check_key_metrics
    
    echo ""
    echo "📊 Summary:"
    echo "  Healthy Services: ${healthy_services}/${total_services}"
    
    if [ "$healthy_services" -eq "$total_services" ]; then
        echo -e "  Overall Status: ${GREEN}✓ ALL SYSTEMS OPERATIONAL${NC}"
    else
        echo -e "  Overall Status: ${YELLOW}⚠ SOME ISSUES DETECTED${NC}"
    fi
    
    echo ""
    generate_monitoring_report
    
    echo ""
    echo "🌐 Quick Access URLs:"
    echo "  Prometheus: http://localhost:9090"
    echo "  Grafana: http://localhost:3000"
    echo "  API Gateway: http://localhost:8080"
    echo ""
    echo "✅ Monitoring health check completed!"
}

# 執行主函數
main "$@"
