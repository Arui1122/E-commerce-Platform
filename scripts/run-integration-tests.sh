#!/bin/bash

# Phase 3.4 集成測試執行腳本
# Integration Tests Execution Script

echo "=== Phase 3.4: E-commerce Platform Integration Tests ==="
echo

# 設置顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 設置基本變量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
INTEGRATION_TESTS_DIR="$PROJECT_ROOT/integration-tests"

echo -e "${BLUE}🔧 Project Root: $PROJECT_ROOT${NC}"
echo -e "${BLUE}📁 Integration Tests Directory: $INTEGRATION_TESTS_DIR${NC}"
echo

# 檢查前置條件
check_prerequisites() {
    echo -e "${YELLOW}🔍 Checking Prerequisites...${NC}"
    
    # 檢查 Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker is not installed or not in PATH${NC}"
        exit 1
    fi
    
    # 檢查 Docker 是否運行
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker is not running${NC}"
        exit 1
    fi
    
    # 檢查 Maven
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven is not installed or not in PATH${NC}"
        exit 1
    fi
    
    # 檢查 Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All prerequisites are met${NC}"
    echo
}

# 啟動基礎設施服務
start_infrastructure() {
    echo -e "${YELLOW}🚀 Starting Infrastructure Services...${NC}"
    
    cd "$PROJECT_ROOT"
    
    # 啟動基礎設施
    if [ -f "docker-compose.yml" ]; then
        docker compose -f infrastructure/docker-compose.yml up -d
        
        echo -e "${BLUE}⏳ Waiting for infrastructure services to be ready...${NC}"
        sleep 30
    else
        echo -e "${YELLOW}⚠️ docker-compose.yml not found, assuming infrastructure is already running${NC}"
    fi
    
    echo -e "${GREEN}✅ Infrastructure services are ready${NC}"
    echo
}

# 編譯並啟動微服務
start_microservices() {
    echo -e "${YELLOW}🏗️ Building and Starting Microservices...${NC}"
    
    cd "$PROJECT_ROOT"
    
    # 編譯 common-events
    echo -e "${BLUE}📦 Building common-events...${NC}"
    cd "$PROJECT_ROOT/services/common-events"
    mvn clean install -DskipTests
    
    # 啟動各個微服務
    services=("eureka-server" "config-server" "api-gateway" "user-service" "product-service" "inventory-service" "cart-service" "order-service" "notification-service")
    
    for service in "${services[@]}"; do
        echo -e "${BLUE}🚀 Starting $service...${NC}"
        
        if [ -d "$PROJECT_ROOT/infrastructure/$service" ]; then
            service_dir="$PROJECT_ROOT/infrastructure/$service"
        else
            service_dir="$PROJECT_ROOT/services/$service"
        fi
        
        if [ -d "$service_dir" ]; then
            cd "$service_dir"
            # 後台啟動服務
            nohup mvn spring-boot:run > "$service.log" 2>&1 &
            echo $! > "$service.pid"
            sleep 10
        else
            echo -e "${YELLOW}⚠️ Service directory not found: $service_dir${NC}"
        fi
    done
    
    # 等待服務啟動
    echo -e "${BLUE}⏳ Waiting for microservices to be ready (60 seconds)...${NC}"
    sleep 60
    
    echo -e "${GREEN}✅ Microservices are ready${NC}"
    echo
}

# 運行集成測試
run_integration_tests() {
    echo -e "${YELLOW}🧪 Running Integration Tests...${NC}"
    
    cd "$INTEGRATION_TESTS_DIR"
    
    # 檢查是否存在 pom.xml
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}❌ Integration tests pom.xml not found in $INTEGRATION_TESTS_DIR${NC}"
        return 1
    fi
    
    # 運行測試
    echo -e "${BLUE}🔄 Executing integration tests...${NC}"
    mvn clean test -Dtest.profile=integration
    
    test_result=$?
    
    if [ $test_result -eq 0 ]; then
        echo -e "${GREEN}✅ All integration tests passed!${NC}"
    else
        echo -e "${RED}❌ Some integration tests failed${NC}"
    fi
    
    return $test_result
}

# 運行特定類型的測試
run_specific_tests() {
    local test_type=$1
    
    echo -e "${YELLOW}🧪 Running $test_type Tests...${NC}"
    
    cd "$INTEGRATION_TESTS_DIR"
    
    case $test_type in
        "e2e")
            mvn test -Dtest="*E2EIntegrationTest"
            ;;
        "performance")
            mvn test -Dtest="*PerformanceIntegrationTest"
            ;;
        "exception")
            mvn test -Dtest="*ExceptionScenarioIntegrationTest"
            ;;
        *)
            echo -e "${RED}❌ Unknown test type: $test_type${NC}"
            echo "Available types: e2e, performance, exception"
            return 1
            ;;
    esac
}

# 停止服務
stop_services() {
    echo -e "${YELLOW}🛑 Stopping Services...${NC}"
    
    cd "$PROJECT_ROOT"
    
    # 停止微服務
    services=("eureka-server" "config-server" "api-gateway" "user-service" "product-service" "inventory-service" "cart-service" "order-service" "notification-service")
    
    for service in "${services[@]}"; do
        if [ -f "$PROJECT_ROOT/infrastructure/$service/$service.pid" ]; then
            pid=$(cat "$PROJECT_ROOT/infrastructure/$service/$service.pid")
            kill $pid 2>/dev/null && echo -e "${GREEN}✅ Stopped $service (PID: $pid)${NC}"
            rm "$PROJECT_ROOT/infrastructure/$service/$service.pid" 2>/dev/null
        elif [ -f "$PROJECT_ROOT/services/$service/$service.pid" ]; then
            pid=$(cat "$PROJECT_ROOT/services/$service/$service.pid")
            kill $pid 2>/dev/null && echo -e "${GREEN}✅ Stopped $service (PID: $pid)${NC}"
            rm "$PROJECT_ROOT/services/$service/$service.pid" 2>/dev/null
        fi
    done
    
    # 停止基礎設施
    if [ -f "infrastructure/docker-compose.yml" ]; then
        docker compose -f infrastructure/docker-compose.yml down
    fi
    
    echo -e "${GREEN}✅ All services stopped${NC}"
}

# 生成測試報告
generate_report() {
    echo -e "${YELLOW}📊 Generating Test Report...${NC}"
    
    cd "$INTEGRATION_TESTS_DIR"
    
    # 生成 Surefire 報告
    mvn surefire-report:report
    
    # 查找報告文件
    report_file="$INTEGRATION_TESTS_DIR/target/site/surefire-report.html"
    
    if [ -f "$report_file" ]; then
        echo -e "${GREEN}✅ Test report generated: $report_file${NC}"
        
        # 如果在 macOS 上，嘗試打開報告
        if [[ "$OSTYPE" == "darwin"* ]]; then
            read -p "Open test report in browser? (y/n): " open_report
            if [[ $open_report == "y" || $open_report == "Y" ]]; then
                open "$report_file"
            fi
        fi
    else
        echo -e "${YELLOW}⚠️ Test report not found${NC}"
    fi
}

# 清理函數
cleanup() {
    echo -e "${YELLOW}🧹 Cleaning up...${NC}"
    stop_services
    exit 0
}

# 設置信號捕獲
trap cleanup SIGINT SIGTERM

# 主函數
main() {
    case "${1:-all}" in
        "all")
            check_prerequisites
            start_infrastructure
            start_microservices
            run_integration_tests
            generate_report
            stop_services
            ;;
        "e2e")
            check_prerequisites
            start_infrastructure
            start_microservices
            run_specific_tests "e2e"
            stop_services
            ;;
        "performance")
            check_prerequisites
            start_infrastructure
            start_microservices
            run_specific_tests "performance"
            stop_services
            ;;
        "exception")
            check_prerequisites
            start_infrastructure
            start_microservices
            run_specific_tests "exception"
            stop_services
            ;;
        "test-only")
            run_integration_tests
            ;;
        "stop")
            stop_services
            ;;
        "help")
            echo "Usage: $0 [command]"
            echo
            echo "Commands:"
            echo "  all           - Run complete integration test suite (default)"
            echo "  e2e           - Run end-to-end tests only"
            echo "  performance   - Run performance tests only"
            echo "  exception     - Run exception scenario tests only"
            echo "  test-only     - Run tests without starting/stopping services"
            echo "  stop          - Stop all services"
            echo "  help          - Show this help message"
            ;;
        *)
            echo -e "${RED}❌ Unknown command: $1${NC}"
            echo "Run '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# 執行主函數
main "$@"
