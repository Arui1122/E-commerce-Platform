#!/bin/bash

# Grafana 測試腳本
# 用於測試 Grafana 配置和監控視覺化

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 項目根目錄
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRASTRUCTURE_DIR="${PROJECT_ROOT}/infrastructure"
MONITORING_DIR="${PROJECT_ROOT}/monitoring"

echo -e "${GREEN}=== Grafana 配置測試腳本 ===${NC}"
echo "項目根目錄: ${PROJECT_ROOT}"

# 檢查配置文件
check_grafana_config() {
    echo -e "\n${YELLOW}檢查 Grafana 配置文件...${NC}"
    
    local config_files=(
        "${MONITORING_DIR}/grafana/provisioning/datasources/datasource.yml"
        "${MONITORING_DIR}/grafana/provisioning/dashboards/dashboard.yml"
        "${MONITORING_DIR}/grafana/provisioning/alerting/rules.yml"
        "${MONITORING_DIR}/grafana/provisioning/alerting/notification-policies.yml"
    )
    
    for file in "${config_files[@]}"; do
        if [[ -f "$file" ]]; then
            echo -e "${GREEN}✓${NC} 配置文件存在: $(basename "$file")"
        else
            echo -e "${RED}✗${NC} 配置文件缺失: $file"
            return 1
        fi
    done
    
    # 檢查儀表板 JSON 文件
    local dashboard_dir="${MONITORING_DIR}/grafana/provisioning/dashboards/json"
    if [[ -d "$dashboard_dir" ]]; then
        local dashboard_count=$(find "$dashboard_dir" -name "*.json" | wc -l)
        echo -e "${GREEN}✓${NC} 找到 $dashboard_count 個儀表板配置文件"
        
        # 列出所有儀表板
        find "$dashboard_dir" -name "*.json" -exec basename {} \; | while read dashboard; do
            echo "  - $dashboard"
        done
    else
        echo -e "${RED}✗${NC} 儀表板目錄不存在: $dashboard_dir"
        return 1
    fi
}

# 驗證 JSON 格式
validate_json_files() {
    echo -e "\n${YELLOW}驗證 JSON 文件格式...${NC}"
    
    local dashboard_dir="${MONITORING_DIR}/grafana/provisioning/dashboards/json"
    local json_valid=true
    
    find "$dashboard_dir" -name "*.json" | while read file; do
        if python3 -m json.tool "$file" > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC} JSON 格式正確: $(basename "$file")"
        else
            echo -e "${RED}✗${NC} JSON 格式錯誤: $(basename "$file")"
            json_valid=false
        fi
    done
    
    if [[ "$json_valid" = false ]]; then
        echo -e "${RED}部分 JSON 文件格式有誤，請檢查${NC}"
        return 1
    fi
}

# 檢查 Docker Compose 配置
check_docker_compose() {
    echo -e "\n${YELLOW}檢查 Docker Compose 中的 Grafana 配置...${NC}"
    
    local compose_file="${INFRASTRUCTURE_DIR}/docker-compose.yml"
    
    if [[ -f "$compose_file" ]]; then
        if grep -q "grafana:" "$compose_file"; then
            echo -e "${GREEN}✓${NC} Docker Compose 中包含 Grafana 服務"
            
            # 檢查 volume 映射
            if grep -q "grafana/provisioning:/etc/grafana/provisioning" "$compose_file"; then
                echo -e "${GREEN}✓${NC} Grafana 配置目錄正確映射"
            else
                echo -e "${RED}✗${NC} Grafana 配置目錄映射缺失或錯誤"
                return 1
            fi
            
            # 檢查端口映射
            if grep -q "3000:3000" "$compose_file"; then
                echo -e "${GREEN}✓${NC} Grafana 端口映射正確"
            else
                echo -e "${YELLOW}⚠${NC} Grafana 端口映射可能不是標準的 3000:3000"
            fi
        else
            echo -e "${RED}✗${NC} Docker Compose 中未找到 Grafana 服務"
            return 1
        fi
    else
        echo -e "${RED}✗${NC} Docker Compose 文件不存在: $compose_file"
        return 1
    fi
}

# 啟動 Grafana
start_grafana() {
    echo -e "\n${YELLOW}啟動 Grafana 和相關服務...${NC}"
    
    cd "$INFRASTRUCTURE_DIR"
    
    # 停止現有服務
    echo "停止現有服務..."
    docker-compose down
    
    # 啟動必要的基礎服務
    echo "啟動基礎服務..."
    docker-compose up -d postgres redis prometheus
    
    # 等待服務啟動
    echo "等待基礎服務啟動..."
    sleep 10
    
    # 啟動 Grafana
    echo "啟動 Grafana..."
    docker-compose up -d grafana
    
    # 等待 Grafana 啟動
    echo "等待 Grafana 啟動..."
    sleep 15
    
    # 檢查服務狀態
    check_services_status
}

# 檢查服務狀態
check_services_status() {
    echo -e "\n${YELLOW}檢查服務狀態...${NC}"
    
    local services=("prometheus" "grafana")
    
    for service in "${services[@]}"; do
        if docker-compose ps "$service" | grep -q "Up"; then
            echo -e "${GREEN}✓${NC} $service 服務運行中"
        else
            echo -e "${RED}✗${NC} $service 服務未運行"
        fi
    done
    
    # 檢查 Grafana 健康狀態
    local grafana_url="http://localhost:3000/api/health"
    local max_attempts=30
    local attempt=1
    
    echo -e "\n${YELLOW}檢查 Grafana 健康狀態...${NC}"
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -s "$grafana_url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC} Grafana 健康檢查通過"
            break
        else
            echo "嘗試 $attempt/$max_attempts: 等待 Grafana 響應..."
            sleep 2
            ((attempt++))
        fi
    done
    
    if [[ $attempt -gt $max_attempts ]]; then
        echo -e "${RED}✗${NC} Grafana 健康檢查失敗"
        return 1
    fi
}

# 測試 Grafana API
test_grafana_api() {
    echo -e "\n${YELLOW}測試 Grafana API...${NC}"
    
    local base_url="http://localhost:3000"
    local auth="admin:admin"
    
    # 測試基本連接
    if curl -s -u "$auth" "$base_url/api/org" > /dev/null; then
        echo -e "${GREEN}✓${NC} Grafana API 連接成功"
    else
        echo -e "${RED}✗${NC} Grafana API 連接失敗"
        return 1
    fi
    
    # 檢查數據源
    local datasources=$(curl -s -u "$auth" "$base_url/api/datasources" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [[ "$datasources" -gt 0 ]]; then
        echo -e "${GREEN}✓${NC} 找到 $datasources 個數據源"
    else
        echo -e "${YELLOW}⚠${NC} 未找到配置的數據源"
    fi
    
    # 檢查儀表板
    local dashboards=$(curl -s -u "$auth" "$base_url/api/search" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [[ "$dashboards" -gt 0 ]]; then
        echo -e "${GREEN}✓${NC} 找到 $dashboards 個儀表板"
    else
        echo -e "${YELLOW}⚠${NC} 未找到配置的儀表板"
    fi
}

# 顯示訪問信息
show_access_info() {
    echo -e "\n${GREEN}=== Grafana 訪問信息 ===${NC}"
    echo "URL: http://localhost:3000"
    echo "用戶名: admin"
    echo "密碼: admin"
    echo ""
    echo "可用的儀表板:"
    echo "1. 系統概覽 (System Overview)"
    echo "2. 服務詳細監控 (Service Details)"
    echo "3. 業務指標 (Business Metrics)"
    echo "4. JVM 指標 (JVM Metrics)"
    echo ""
    echo -e "${YELLOW}注意: 首次登錄可能需要等待儀表板完全加載${NC}"
}

# 主函數
main() {
    echo -e "${GREEN}開始 Grafana 配置測試...${NC}"
    
    # 執行檢查
    check_grafana_config || exit 1
    validate_json_files || exit 1
    check_docker_compose || exit 1
    
    # 詢問是否啟動服務
    echo -e "\n${YELLOW}是否啟動 Grafana 服務進行測試? (y/n)${NC}"
    read -r response
    
    if [[ "$response" =~ ^[Yy]$ ]]; then
        start_grafana || exit 1
        test_grafana_api || exit 1
        show_access_info
    else
        echo -e "${GREEN}配置檢查完成，跳過服務啟動${NC}"
    fi
    
    echo -e "\n${GREEN}=== Grafana 配置測試完成 ===${NC}"
}

# 執行主函數
main "$@"
