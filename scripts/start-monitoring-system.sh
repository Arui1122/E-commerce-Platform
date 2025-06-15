#!/bin/bash

# E-commerce Platform - Prometheus 監控系統啟動和測試腳本
# 一鍵啟動監控系統並進行全面測試

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/infrastructure/docker-compose.yml"
WAIT_TIME=30

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}🚀 E-commerce Platform - Prometheus Monitoring Setup${NC}"
echo "======================================================"

# 檢查必要文件
check_requirements() {
    echo "📋 Checking requirements..."
    
    local required_files=(
        "$DOCKER_COMPOSE_FILE"
        "$PROJECT_ROOT/monitoring/prometheus/prometheus.yml"
        "$PROJECT_ROOT/monitoring/prometheus/alert-rules.yml"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            echo -e "${RED}❌ Required file not found: $file${NC}"
            exit 1
        fi
    done
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed${NC}"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}❌ Docker Compose is not installed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All requirements satisfied${NC}"
}

# 停止現有服務
stop_existing_services() {
    echo "🛑 Stopping existing services..."
    
    cd "$PROJECT_ROOT/infrastructure"
    
    # 優雅停止服務
    docker-compose down --remove-orphans || true
    
    # 清理懸掛的網絡和容器
    docker network prune -f || true
    docker container prune -f || true
    
    echo -e "${GREEN}✅ Existing services stopped${NC}"
}

# 構建服務
build_services() {
    echo "🔨 Building services..."
    
    # 構建所有服務
    cd "$PROJECT_ROOT"
    
    echo "  Building infrastructure services..."
    cd infrastructure/eureka-server && mvn clean compile -q
    cd ../config-server && mvn clean compile -q
    cd ../api-gateway && mvn clean compile -q
    
    echo "  Building business services..."
    cd ../../services/user-service && mvn clean compile -q
    cd ../product-service && mvn clean compile -q
    cd ../cart-service && mvn clean compile -q
    cd ../order-service && mvn clean compile -q
    cd ../inventory-service && mvn clean compile -q
    cd ../notification-service && mvn clean compile -q
    
    echo -e "${GREEN}✅ All services built successfully${NC}"
}

# 啟動基礎設施
start_infrastructure() {
    echo "🏗️  Starting infrastructure services..."
    
    cd "$PROJECT_ROOT/infrastructure"
    
    # 啟動基礎服務
    docker-compose up -d postgres redis zookeeper kafka prometheus grafana
    
    echo "⏳ Waiting for infrastructure to be ready..."
    sleep $WAIT_TIME
    
    # 檢查基礎設施狀態
    echo "  Checking infrastructure health..."
    
    local services=("postgres" "redis" "kafka" "prometheus" "grafana")
    for service in "${services[@]}"; do
        if docker-compose ps "$service" | grep -q "Up"; then
            echo -e "    ${service}: ${GREEN}✓ Running${NC}"
        else
            echo -e "    ${service}: ${RED}✗ Failed${NC}"
        fi
    done
}

# 啟動應用服務
start_application_services() {
    echo "🚀 Starting application services..."
    
    cd "$PROJECT_ROOT/infrastructure"
    
    # 按照依賴順序啟動服務
    echo "  Starting service discovery..."
    docker-compose up -d eureka-server
    sleep 15
    
    echo "  Starting configuration server..."
    docker-compose up -d config-server
    sleep 10
    
    echo "  Starting API gateway..."
    docker-compose up -d api-gateway
    sleep 10
    
    echo "  Starting business services..."
    docker-compose up -d user-service product-service cart-service inventory-service order-service notification-service
    sleep 20
    
    echo -e "${GREEN}✅ All application services started${NC}"
}

# 驗証服務狀態
verify_services() {
    echo "🔍 Verifying service status..."
    
    cd "$PROJECT_ROOT/infrastructure"
    
    # 顯示所有服務狀態
    echo "  Service Status:"
    docker-compose ps --format "table {{.Name}}\t{{.State}}\t{{.Ports}}"
    
    # 檢查服務健康狀態
    echo ""
    echo "  Health Check Results:"
    
    local services=(
        "eureka-server:8761"
        "config-server:8888"
        "api-gateway:8080"
        "user-service:8081"
        "product-service:8082"
        "cart-service:8083"
        "order-service:8084"
        "inventory-service:8085"
        "notification-service:8086"
    )
    
    for service_port in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_port"
        
        echo -n "    $service: "
        if curl -s -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Healthy${NC}"
        else
            echo -e "${RED}✗ Unhealthy${NC}"
        fi
    done
}

# 測試 Prometheus 配置
test_prometheus_config() {
    echo "📊 Testing Prometheus configuration..."
    
    # 檢查 Prometheus 是否可訪問
    if curl -s -f "http://localhost:9090/api/v1/status/runtimeinfo" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Prometheus is accessible${NC}"
    else
        echo -e "${RED}❌ Prometheus is not accessible${NC}"
        return 1
    fi
    
    # 檢查配置載入狀態
    local config_status=$(curl -s "http://localhost:9090/api/v1/status/config" | jq -r '.status')
    if [ "$config_status" = "success" ]; then
        echo -e "${GREEN}✅ Prometheus configuration loaded successfully${NC}"
    else
        echo -e "${RED}❌ Prometheus configuration has errors${NC}"
        return 1
    fi
    
    # 檢查告警規則
    local rules_response=$(curl -s "http://localhost:9090/api/v1/rules")
    local rules_count=$(echo "$rules_response" | jq -r '.data.groups | length')
    echo -e "${GREEN}✅ Alert rules loaded: $rules_count groups${NC}"
    
    return 0
}

# 運行監控測試
run_monitoring_tests() {
    echo "🧪 Running monitoring tests..."
    
    # 等待指標收集
    echo "  Waiting for metrics collection..."
    sleep 10
    
    # 運行健康檢查
    if [ -f "$SCRIPT_DIR/check-monitoring-health.sh" ]; then
        echo "  Running health check..."
        bash "$SCRIPT_DIR/check-monitoring-health.sh"
    fi
    
    # 運行指標測試
    if [ -f "$SCRIPT_DIR/test-prometheus-metrics.sh" ]; then
        echo "  Running metrics test..."
        bash "$SCRIPT_DIR/test-prometheus-metrics.sh"
    fi
}

# 顯示訪問信息
show_access_info() {
    echo ""
    echo -e "${CYAN}🌐 Access Information${NC}"
    echo "===================="
    echo ""
    echo "Monitoring Dashboards:"
    echo "  📊 Prometheus: http://localhost:9090"
    echo "  📈 Grafana: http://localhost:3000 (admin/admin)"
    echo ""
    echo "Service Endpoints:"
    echo "  🌐 API Gateway: http://localhost:8080"
    echo "  🔍 Eureka Server: http://localhost:8761"
    echo "  ⚙️  Config Server: http://localhost:8888"
    echo ""
    echo "Actuator Endpoints (example - user-service):"
    echo "  Health: http://localhost:8081/actuator/health"
    echo "  Metrics: http://localhost:8081/actuator/metrics"
    echo "  Prometheus: http://localhost:8081/actuator/prometheus"
    echo ""
    echo "Useful Commands:"
    echo "  📋 Check all services: docker-compose ps"
    echo "  📊 View logs: docker-compose logs -f [service-name]"
    echo "  🛑 Stop all: docker-compose down"
    echo "  🔄 Restart service: docker-compose restart [service-name]"
}

# 主函數
main() {
    echo ""
    
    # 執行檢查和設置
    check_requirements
    echo ""
    
    stop_existing_services
    echo ""
    
    build_services
    echo ""
    
    start_infrastructure
    echo ""
    
    start_application_services
    echo ""
    
    verify_services
    echo ""
    
    # 測試 Prometheus
    if test_prometheus_config; then
        echo ""
        run_monitoring_tests
    else
        echo -e "${YELLOW}⚠️  Prometheus configuration issues detected${NC}"
        echo "  Please check the configuration and try again."
    fi
    
    show_access_info
    
    echo ""
    echo -e "${GREEN}🎉 E-commerce Platform monitoring system is ready!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Visit Prometheus at http://localhost:9090 to explore metrics"
    echo "  2. Visit Grafana at http://localhost:3000 to create dashboards"
    echo "  3. Test API endpoints through the gateway at http://localhost:8080"
    echo ""
}

# 處理中斷信號
trap 'echo -e "\n${YELLOW}⚠️  Setup interrupted by user${NC}"; exit 130' INT

# 執行主函數
main "$@"
