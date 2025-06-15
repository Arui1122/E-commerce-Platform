#!/bin/bash

# 日志管理功能演示脚本
# 展示结构化日志和分散式追踪的核心功能

echo "=== E-commerce Platform 日志管理功能演示 ==="
echo "演示时间: $(date)"
echo

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 演示步骤
demo_step=1

print_step() {
    echo -e "\n${BLUE}=== 步骤 $demo_step: $1 ===${NC}"
    ((demo_step++))
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 等待用户按键
wait_for_key() {
    echo -e "\n${YELLOW}按任意键继续...${NC}"
    read -n 1 -s
}

# 检查服务状态
check_services() {
    print_step "检查服务状态"
    
    services=(
        "Eureka Server:8761:/actuator/health"
        "API Gateway:8080:/actuator/health"
        "User Service:8081:/actuator/health"
        "Product Service:8082:/actuator/health"
        "Zipkin:9411:/health"
    )
    
    all_running=true
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r name port endpoint <<< "$service_info"
        
        if curl -s "http://localhost:$port$endpoint" > /dev/null 2>&1; then
            print_success "$name 运行正常"
        else
            print_warning "$name 未运行"
            all_running=false
        fi
    done
    
    if [ "$all_running" = false ]; then
        print_warning "部分服务未运行，演示可能不完整"
        echo "请确保先启动必要的服务："
        echo "  cd infrastructure && docker-compose up -d"
        echo "  ./scripts/start-services.sh"
    fi
    
    wait_for_key
}

# 创建测试日志目录
setup_demo_logs() {
    print_step "准备演示环境"
    
    print_info "创建日志目录..."
    mkdir -p logs/demo
    
    print_info "设置日志监控..."
    # 在后台启动日志监控
    (tail -f logs/user-service/user-service.log 2>/dev/null | while read line; do
        echo "[USER-SERVICE] $line" >> logs/demo/combined.log
    done) &
    TAIL_PID_USER=$!
    
    (tail -f logs/product-service/product-service.log 2>/dev/null | while read line; do
        echo "[PRODUCT-SERVICE] $line" >> logs/demo/combined.log
    done) &
    TAIL_PID_PRODUCT=$!
    
    print_success "演示环境准备完成"
    wait_for_key
}

# 演示结构化日志
demo_structured_logging() {
    print_step "演示结构化日志"
    
    print_info "发送API请求生成日志..."
    
    # 生成唯一的请求ID
    REQUEST_ID="demo-$(date +%s)"
    
    echo "请求ID: $REQUEST_ID"
    echo
    
    # 发送用户服务请求
    print_info "调用用户服务API..."
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         -H "X-User-ID: demo-user" \
         "http://localhost:8080/api/v1/users" > /dev/null 2>&1 || \
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:8081/actuator/health" > /dev/null 2>&1
    
    sleep 1
    
    # 发送产品服务请求
    print_info "调用产品服务API..."
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:8080/api/v1/products" > /dev/null 2>&1 || \
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:8082/actuator/health" > /dev/null 2>&1
    
    sleep 2
    
    print_success "API请求完成"
    
    # 显示生成的日志
    print_info "查看生成的结构化日志..."
    
    if [ -f "logs/demo/combined.log" ]; then
        echo -e "\n${CYAN}最近生成的日志:${NC}"
        tail -10 logs/demo/combined.log | while read line; do
            echo -e "${GREEN}  $line${NC}"
        done
    else
        print_warning "演示日志文件未找到，请检查服务状态"
        
        # 尝试直接查看服务日志
        if [ -f "logs/user-service/user-service.log" ]; then
            echo -e "\n${CYAN}用户服务最近日志:${NC}"
            tail -5 logs/user-service/user-service.log | while read line; do
                echo -e "${GREEN}  $line${NC}"
            done
        fi
    fi
    
    wait_for_key
}

# 演示分散式追踪
demo_distributed_tracing() {
    print_step "演示分散式追踪"
    
    print_info "生成复杂的服务调用链..."
    
    # 创建多个相关请求
    for i in {1..3}; do
        REQUEST_ID="trace-demo-$i-$(date +%s)"
        echo "生成追踪 $i: $REQUEST_ID"
        
        # 模拟用户->产品->库存的调用链
        curl -s -H "X-Request-ID: $REQUEST_ID" \
             "http://localhost:8080/api/v1/products" > /dev/null 2>&1 &
        
        # 模拟用户->购物车的调用链  
        curl -s -H "X-Request-ID: $REQUEST_ID" \
             "http://localhost:8080/api/v1/cart" > /dev/null 2>&1 &
        
        sleep 0.5
    done
    
    wait # 等待所有后台请求完成
    
    print_success "追踪数据生成完成"
    
    print_info "请访问 Zipkin UI 查看追踪信息:"
    echo -e "${CYAN}  URL: http://localhost:9411${NC}"
    echo -e "${CYAN}  步骤:${NC}"
    echo -e "${CYAN}    1. 选择服务名称${NC}"
    echo -e "${CYAN}    2. 点击 'Find Traces'${NC}"
    echo -e "${CYAN}    3. 点击追踪记录查看详情${NC}"
    
    wait_for_key
}

# 演示日志搜索和分析
demo_log_analysis() {
    print_step "演示日志搜索和分析"
    
    print_info "生成分析数据..."
    
    # 生成一些测试日志
    REQUEST_ID="analysis-$(date +%s)"
    
    for i in {1..5}; do
        curl -s -H "X-Request-ID: $REQUEST_ID-$i" \
             "http://localhost:8081/actuator/health" > /dev/null 2>&1
        sleep 0.2
    done
    
    sleep 1
    
    print_info "执行日志分析..."
    
    # 基本统计
    echo -e "\n${CYAN}1. 日志文件统计:${NC}"
    for service in user-service product-service cart-service; do
        if [ -f "logs/$service/$service.log" ]; then
            lines=$(wc -l < "logs/$service/$service.log" 2>/dev/null || echo "0")
            size=$(du -h "logs/$service/$service.log" 2>/dev/null | cut -f1 || echo "0B")
            echo -e "${GREEN}  $service: $lines 行, $size${NC}"
        fi
    done
    
    # 搜索特定请求
    echo -e "\n${CYAN}2. 搜索最近的请求ID:${NC}"
    if [ -f "logs/user-service/user-service.log" ]; then
        recent_requests=$(grep -o "requestId=[a-zA-Z0-9-]*" logs/user-service/user-service.log 2>/dev/null | tail -3 || echo "未找到")
        echo -e "${GREEN}  $recent_requests${NC}"
    else
        echo -e "${YELLOW}  用户服务日志文件不存在${NC}"
    fi
    
    # 错误统计
    echo -e "\n${CYAN}3. 错误日志统计:${NC}"
    for service in user-service product-service cart-service; do
        if [ -f "logs/$service/$service-error.log" ]; then
            error_count=$(wc -l < "logs/$service/$service-error.log" 2>/dev/null || echo "0")
            echo -e "${GREEN}  $service 错误数: $error_count${NC}"
        fi
    done
    
    wait_for_key
}

# 演示实时日志监控
demo_realtime_monitoring() {
    print_step "演示实时日志监控"
    
    print_info "启动实时日志监控 (5秒)..."
    echo -e "${CYAN}监控用户服务日志变化...${NC}"
    echo -e "${YELLOW}(将在5秒后自动停止)${NC}"
    echo
    
    # 在后台生成一些请求
    (
        sleep 1
        for i in {1..3}; do
            curl -s "http://localhost:8081/actuator/health" > /dev/null 2>&1
            sleep 1
        done
    ) &
    
    # 实时显示日志
    if [ -f "logs/user-service/user-service.log" ]; then
        timeout 5 tail -f logs/user-service/user-service.log 2>/dev/null | while read line; do
            echo -e "${GREEN}[REALTIME] $line${NC}"
        done
    else
        echo -e "${YELLOW}用户服务日志文件不存在，跳过实时监控演示${NC}"
        sleep 5
    fi
    
    print_success "实时监控演示完成"
    wait_for_key
}

# 清理演示环境
cleanup_demo() {
    print_step "清理演示环境"
    
    print_info "停止后台进程..."
    
    # 停止日志监控进程
    if [ ! -z "$TAIL_PID_USER" ]; then
        kill $TAIL_PID_USER 2>/dev/null || true
    fi
    if [ ! -z "$TAIL_PID_PRODUCT" ]; then
        kill $TAIL_PID_PRODUCT 2>/dev/null || true
    fi
    
    print_info "保留演示日志文件..."
    if [ -f "logs/demo/combined.log" ]; then
        echo -e "${CYAN}演示日志保存在: logs/demo/combined.log${NC}"
    fi
    
    print_success "清理完成"
}

# 显示总结
show_summary() {
    print_step "演示总结"
    
    echo -e "${CYAN}🎉 日志管理功能演示完成！${NC}"
    echo
    echo -e "${BLUE}已演示的功能:${NC}"
    echo -e "${GREEN}  ✅ 结构化日志输出${NC}"
    echo -e "${GREEN}  ✅ 分散式追踪${NC}"
    echo -e "${GREEN}  ✅ 日志搜索和分析${NC}"
    echo -e "${GREEN}  ✅ 实时日志监控${NC}"
    echo
    echo -e "${BLUE}有用的资源:${NC}"
    echo -e "${CYAN}  📁 日志文件: ./logs/${NC}"
    echo -e "${CYAN}  🔍 Zipkin UI: http://localhost:9411${NC}"
    echo -e "${CYAN}  📊 服务健康: http://localhost:8081/actuator/health${NC}"
    echo -e "${CYAN}  📖 使用指南: ./docs/LOGGING_USAGE_GUIDE.md${NC}"
    echo
    echo -e "${BLUE}下一步建议:${NC}"
    echo -e "${YELLOW}  1. 查看各服务的日志文件${NC}"
    echo -e "${YELLOW}  2. 在Zipkin UI中探索追踪数据${NC}"
    echo -e "${YELLOW}  3. 运行完整的测试脚本: ./scripts/test-logging-management.sh${NC}"
    echo -e "${YELLOW}  4. 阅读详细的实现报告: ./docs/LOGGING_MANAGEMENT_IMPLEMENTATION_REPORT.md${NC}"
    echo
}

# 主函数
main() {
    echo -e "${BLUE}欢迎使用日志管理功能演示！${NC}"
    echo
    echo "本演示将展示以下功能："
    echo "  1. 结构化日志"
    echo "  2. 分散式追踪"
    echo "  3. 日志分析"
    echo "  4. 实时监控"
    echo
    echo -e "${YELLOW}注意: 请确保相关服务已启动${NC}"
    
    wait_for_key
    
    # 设置清理函数
    trap cleanup_demo EXIT
    
    # 执行演示步骤
    check_services
    setup_demo_logs
    demo_structured_logging
    demo_distributed_tracing
    demo_log_analysis
    demo_realtime_monitoring
    show_summary
}

# 运行演示
main "$@"
