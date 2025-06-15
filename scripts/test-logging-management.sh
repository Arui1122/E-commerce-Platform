#!/bin/bash

# 日志管理测试脚本
# 测试结构化日志和分散式追踪功能

echo "=== E-commerce Platform 日志管理测试 ==="
echo "测试时间: $(date)"
echo

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务端口定义
EUREKA_PORT=8761
API_GATEWAY_PORT=8080
USER_SERVICE_PORT=8081
PRODUCT_SERVICE_PORT=8082
CART_SERVICE_PORT=8083
ORDER_SERVICE_PORT=8084
INVENTORY_SERVICE_PORT=8085
NOTIFICATION_SERVICE_PORT=8086
ZIPKIN_PORT=9411

# 检查服务健康状态
check_service_health() {
    local service_name=$1
    local port=$2
    local endpoint=$3
    
    echo -n "检查 $service_name 健康状态... "
    
    if curl -s "http://localhost:$port$endpoint" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 正常${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        return 1
    fi
}

# 测试日志输出
test_logging() {
    echo -e "\n${BLUE}=== 测试结构化日志输出 ===${NC}"
    
    # 创建日志目录
    mkdir -p logs/test
    
    # 测试用户服务日志
    echo "测试用户服务API调用..."
    REQUEST_ID=$(uuidgen | cut -d'-' -f1)
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:$API_GATEWAY_PORT/api/v1/users/health" > /dev/null
    
    # 测试产品服务日志
    echo "测试产品服务API调用..."
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:$API_GATEWAY_PORT/api/v1/products" > /dev/null
    
    # 测试购物车服务日志
    echo "测试购物车服务API调用..."
    curl -s -H "X-Request-ID: $REQUEST_ID" \
         "http://localhost:$API_GATEWAY_PORT/api/v1/cart/health" > /dev/null
    
    echo -e "${GREEN}✓ API调用完成，检查日志文件...${NC}"
}

# 测试分散式追踪
test_distributed_tracing() {
    echo -e "\n${BLUE}=== 测试分散式追踪 ===${NC}"
    
    echo "检查Zipkin服务..."
    if check_service_health "Zipkin" $ZIPKIN_PORT "/api/v2/services"; then
        echo -e "${GREEN}✓ Zipkin服务正常运行${NC}"
        
        # 生成追踪数据
        echo "生成追踪数据..."
        for i in {1..5}; do
            REQUEST_ID=$(uuidgen | cut -d'-' -f1)
            echo "发送请求 $i (Request ID: $REQUEST_ID)"
            
            # 模拟完整的业务流程
            curl -s -H "X-Request-ID: $REQUEST_ID" \
                 "http://localhost:$API_GATEWAY_PORT/api/v1/products" > /dev/null
            sleep 1
        done
        
        echo -e "${GREEN}✓ 追踪数据生成完成${NC}"
        echo "请访问 http://localhost:9411 查看Zipkin UI"
    else
        echo -e "${RED}✗ Zipkin服务未运行${NC}"
    fi
}

# 检查日志文件
check_log_files() {
    echo -e "\n${BLUE}=== 检查日志文件 ===${NC}"
    
    # 检查各服务的日志文件
    services=("user-service" "product-service" "cart-service" "order-service" "inventory-service" "notification-service" "api-gateway" "eureka-server" "config-server")
    
    for service in "${services[@]}"; do
        log_dir="logs/$service"
        if [ -d "$log_dir" ]; then
            echo -e "${GREEN}✓ $service 日志目录存在${NC}"
            
            # 检查主日志文件
            if [ -f "$log_dir/$service.log" ]; then
                echo -e "  - 主日志文件: ${GREEN}✓${NC}"
                lines=$(wc -l < "$log_dir/$service.log")
                echo -e "    日志行数: $lines"
            else
                echo -e "  - 主日志文件: ${YELLOW}未找到${NC}"
            fi
            
            # 检查错误日志文件
            if [ -f "$log_dir/$service-error.log" ]; then
                echo -e "  - 错误日志文件: ${GREEN}✓${NC}"
                error_lines=$(wc -l < "$log_dir/$service-error.log")
                if [ $error_lines -gt 0 ]; then
                    echo -e "    ${YELLOW}警告: 发现 $error_lines 行错误日志${NC}"
                fi
            fi
            
        else
            echo -e "${YELLOW}⚠ $service 日志目录不存在${NC}"
        fi
    done
}

# 检查日志格式
check_log_format() {
    echo -e "\n${BLUE}=== 检查日志格式 ===${NC}"
    
    # 检查是否包含追踪ID
    echo "检查日志中的追踪ID格式..."
    
    log_file="logs/user-service/user-service.log"
    if [ -f "$log_file" ]; then
        # 查找包含traceId的日志行
        trace_lines=$(grep -c "traceId" "$log_file" 2>/dev/null || echo "0")
        if [ $trace_lines -gt 0 ]; then
            echo -e "${GREEN}✓ 发现 $trace_lines 行包含追踪ID的日志${NC}"
            
            # 显示示例日志行
            echo "示例日志行:"
            grep "traceId" "$log_file" | head -3 | while read line; do
                echo -e "${BLUE}  $line${NC}"
            done
        else
            echo -e "${YELLOW}⚠ 未发现包含追踪ID的日志${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ 用户服务日志文件不存在${NC}"
    fi
}

# 生成日志统计报告
generate_log_report() {
    echo -e "\n${BLUE}=== 生成日志统计报告 ===${NC}"
    
    report_file="logs/log_analysis_report.txt"
    echo "生成日志分析报告: $report_file"
    
    cat > "$report_file" << EOF
E-commerce Platform 日志分析报告
生成时间: $(date)
=====================================

服务日志统计:
EOF

    # 统计各服务日志
    for service in user-service product-service cart-service order-service inventory-service notification-service; do
        log_dir="logs/$service"
        if [ -d "$log_dir" ]; then
            echo "  $service:" >> "$report_file"
            
            if [ -f "$log_dir/$service.log" ]; then
                lines=$(wc -l < "$log_dir/$service.log")
                size=$(du -h "$log_dir/$service.log" | cut -f1)
                echo "    - 主日志: $lines 行, $size" >> "$report_file"
            fi
            
            if [ -f "$log_dir/$service-error.log" ]; then
                error_lines=$(wc -l < "$log_dir/$service-error.log")
                echo "    - 错误日志: $error_lines 行" >> "$report_file"
            fi
        fi
    done
    
    echo "" >> "$report_file"
    echo "日志级别分布:" >> "$report_file"
    
    # 统计日志级别
    if [ -f "logs/user-service/user-service.log" ]; then
        echo "  INFO:  $(grep -c "INFO" logs/user-service/user-service.log 2>/dev/null || echo 0)" >> "$report_file"
        echo "  DEBUG: $(grep -c "DEBUG" logs/user-service/user-service.log 2>/dev/null || echo 0)" >> "$report_file"
        echo "  WARN:  $(grep -c "WARN" logs/user-service/user-service.log 2>/dev/null || echo 0)" >> "$report_file"
        echo "  ERROR: $(grep -c "ERROR" logs/user-service/user-service.log 2>/dev/null || echo 0)" >> "$report_file"
    fi
    
    echo -e "${GREEN}✓ 报告生成完成${NC}"
}

# 主函数
main() {
    echo -e "${YELLOW}开始日志管理功能测试...${NC}"
    
    # 检查基础服务
    echo -e "\n${BLUE}=== 检查基础服务状态 ===${NC}"
    check_service_health "Eureka Server" $EUREKA_PORT "/actuator/health"
    check_service_health "API Gateway" $API_GATEWAY_PORT "/actuator/health"
    check_service_health "User Service" $USER_SERVICE_PORT "/actuator/health"
    check_service_health "Product Service" $PRODUCT_SERVICE_PORT "/actuator/health"
    
    # 测试日志功能
    test_logging
    
    # 测试分散式追踪
    test_distributed_tracing
    
    # 检查日志文件
    check_log_files
    
    # 检查日志格式
    check_log_format
    
    # 生成报告
    generate_log_report
    
    echo -e "\n${GREEN}=== 日志管理测试完成 ===${NC}"
    echo
    echo "测试结果:"
    echo "1. 日志文件位置: ./logs/"
    echo "2. Zipkin UI: http://localhost:9411"
    echo "3. 分析报告: ./logs/log_analysis_report.txt"
    echo
    echo "建议的后续操作:"
    echo "- 检查各服务的日志输出格式"
    echo "- 验证追踪ID在服务间的传递"
    echo "- 测试错误日志的记录"
    echo "- 监控日志文件的轮转"
}

# 运行主函数
main "$@"
