#!/bin/bash

# E-commerce Platform - 健康檢查腳本（針對慢網路優化）
# 檢查所有服務是否正在運行並且健康

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🏥 E-commerce Platform Health Check (Slow Network Optimized)${NC}"
echo "=================================================================="

# 服務列表和對應的健康檢查端點
declare -A SERVICES=(
    ["PostgreSQL"]="direct:postgres:5432"
    ["Redis"]="direct:redis:6379"
    ["Eureka Server"]="http://localhost:8761/actuator/health"
    ["Config Server"]="http://localhost:8888/actuator/health"
    ["API Gateway"]="http://localhost:8080/actuator/health"
    ["User Service"]="http://localhost:8081/actuator/health"
    ["Product Service"]="http://localhost:8082/actuator/health"
    ["Cart Service"]="http://localhost:8083/actuator/health"
    ["Inventory Service"]="http://localhost:8085/actuator/health"
    ["Prometheus"]="http://localhost:9090/-/healthy"
    ["Grafana"]="http://localhost:3000/api/health"
)

# 檢查單個服務健康狀態
check_service() {
    local service_name="$1"
    local endpoint="$2"
    local timeout=30
    
    echo -n -e "Checking ${service_name}... "
    
    if [[ "$endpoint" == "direct:postgres:5432" ]]; then
        # 特殊處理 PostgreSQL
        if timeout $timeout bash -c "</dev/tcp/localhost/5432" 2>/dev/null; then
            echo -e "${GREEN}✅ HEALTHY${NC}"
            return 0
        else
            echo -e "${RED}❌ UNHEALTHY (Connection failed)${NC}"
            return 1
        fi
    elif [[ "$endpoint" == "direct:redis:6379" ]]; then
        # 特殊處理 Redis
        if timeout $timeout bash -c "</dev/tcp/localhost/6379" 2>/dev/null; then
            echo -e "${GREEN}✅ HEALTHY${NC}"
            return 0
        else
            echo -e "${RED}❌ UNHEALTHY (Connection failed)${NC}"
            return 1
        fi
    else
        # HTTP 健康檢查
        local response
        local http_code
        
        response=$(timeout $timeout curl -s -w "%{http_code}" "$endpoint" 2>/dev/null) || {
            echo -e "${RED}❌ UNHEALTHY (Timeout or connection failed)${NC}"
            return 1
        }
        
        http_code="${response: -3}"
        
        if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
            # 進一步檢查 Spring Boot Actuator 響應
            if [[ "$endpoint" == *"/actuator/health"* ]]; then
                local body="${response%???}"
                if echo "$body" | grep -q '"status":"UP"' 2>/dev/null; then
                    echo -e "${GREEN}✅ HEALTHY${NC}"
                    return 0
                else
                    echo -e "${YELLOW}⚠️  DEGRADED (Service up but status not UP)${NC}"
                    return 1
                fi
            else
                echo -e "${GREEN}✅ HEALTHY${NC}"
                return 0
            fi
        else
            echo -e "${RED}❌ UNHEALTHY (HTTP $http_code)${NC}"
            return 1
        fi
    fi
}

# 檢查 Docker 容器狀態
check_docker_containers() {
    echo -e "\n${BLUE}🐳 Docker Container Status:${NC}"
    echo "----------------------------------------------------------"
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker command not found${NC}"
        return 1
    fi
    
    # 檢查容器狀態
    docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=ecommerce-" | while IFS=$'\t' read -r name status; do
        if [[ "$name" == "NAMES" ]]; then
            continue
        fi
        
        if [[ "$status" == *"Up"* ]]; then
            echo -e "${name}: ${GREEN}✅ Running${NC}"
        else
            echo -e "${name}: ${RED}❌ $status${NC}"
        fi
    done
}

# 檢查端口佔用情況
check_ports() {
    echo -e "\n${BLUE}🔌 Port Status:${NC}"
    echo "----------------------------------------------------------"
    
    local ports=(5432 6379 2181 9092 8761 8888 8080 8081 8082 8083 8085 9090 3000)
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null; then
            echo -e "Port $port: ${GREEN}✅ In Use${NC}"
        else
            echo -e "Port $port: ${RED}❌ Not in use${NC}"
        fi
    done
}

# 主要檢查流程
main() {
    local failed_services=0
    local total_services=${#SERVICES[@]}
    
    echo -e "\n${BLUE}🏥 Service Health Status:${NC}"
    echo "----------------------------------------------------------"
    
    # 檢查各個服務
    for service in "${!SERVICES[@]}"; do
        if ! check_service "$service" "${SERVICES[$service]}"; then
            ((failed_services++))
        fi
        sleep 2  # 慢網路環境下給予緩衝時間
    done
    
    # 檢查 Docker 容器
    check_docker_containers
    
    # 檢查端口
    check_ports
    
    # 總結
    echo ""
    echo "=================================================================="
    
    local healthy_services=$((total_services - failed_services))
    echo -e "${BLUE}📊 Health Summary:${NC}"
    echo -e "Healthy Services: ${GREEN}$healthy_services${NC}/$total_services"
    echo -e "Failed Services: ${RED}$failed_services${NC}/$total_services"
    
    if [[ $failed_services -eq 0 ]]; then
        echo -e "\n${GREEN}🎉 All services are healthy!${NC}"
        echo -e "${BLUE}💡 Ready to process requests.${NC}"
        
        echo -e "\n${BLUE}🔗 Quick Access URLs:${NC}"
        echo "- Eureka Dashboard: http://localhost:8761"
        echo "- API Gateway: http://localhost:8080"
        echo "- Swagger UIs available at each service endpoint + /swagger-ui.html"
        echo "- Prometheus: http://localhost:9090"
        echo "- Grafana: http://localhost:3000 (admin/admin)"
        
        exit 0
    else
        echo -e "\n${YELLOW}⚠️  Some services are not healthy.${NC}"
        echo -e "${BLUE}💡 Troubleshooting tips:${NC}"
        echo "1. Check if all containers are running: docker-compose ps"
        echo "2. Check service logs: docker-compose logs -f [service-name]"
        echo "3. For slow networks, wait a few more minutes for services to initialize"
        echo "4. Restart unhealthy services: docker-compose restart [service-name]"
        
        exit 1
    fi
}

# 執行主函數
main "$@"