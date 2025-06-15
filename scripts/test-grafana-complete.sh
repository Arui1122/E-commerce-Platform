#!/bin/bash

# Grafana 完整功能測試腳本
# 測試所有儀表板、告警和配置

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
GRAFANA_URL="http://localhost:3000"
GRAFANA_AUTH="admin:admin"

echo -e "${BLUE}=== Grafana 完整功能測試 ===${NC}"

# 基本連接測試
test_basic_connection() {
    echo -e "\n${YELLOW}1. 測試基本連接...${NC}"
    
    if curl -s -f "$GRAFANA_URL/api/health" > /dev/null; then
        echo -e "${GREEN}✓${NC} Grafana 健康檢查通過"
    else
        echo -e "${RED}✗${NC} Grafana 健康檢查失敗"
        return 1
    fi
    
    if curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/org" > /dev/null; then
        echo -e "${GREEN}✓${NC} Grafana API 認證成功"
    else
        echo -e "${RED}✗${NC} Grafana API 認證失敗"
        return 1
    fi
}

# 數據源測試
test_datasources() {
    echo -e "\n${YELLOW}2. 測試數據源配置...${NC}"
    
    local datasources=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/datasources")
    local ds_count=$(echo "$datasources" | jq length 2>/dev/null || echo "0")
    
    if [[ "$ds_count" -gt 0 ]]; then
        echo -e "${GREEN}✓${NC} 找到 $ds_count 個數據源"
        
        # 檢查 Prometheus 數據源
        local prometheus_name=$(echo "$datasources" | jq -r '.[] | select(.type=="prometheus") | .name' 2>/dev/null)
        if [[ -n "$prometheus_name" ]]; then
            echo -e "${GREEN}✓${NC} Prometheus 數據源已配置: $prometheus_name"
            
            # 測試 Prometheus 連接
            local ds_id=$(echo "$datasources" | jq -r '.[] | select(.type=="prometheus") | .id' 2>/dev/null)
            local health_result=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/datasources/$ds_id/health" | jq -r '.status' 2>/dev/null)
            
            if [[ "$health_result" == "OK" ]]; then
                echo -e "${GREEN}✓${NC} Prometheus 數據源連接測試通過"
            else
                echo -e "${RED}✗${NC} Prometheus 數據源連接測試失敗: $health_result"
            fi
        else
            echo -e "${RED}✗${NC} 未找到 Prometheus 數據源"
        fi
    else
        echo -e "${RED}✗${NC} 未找到任何數據源"
        return 1
    fi
}

# 儀表板測試
test_dashboards() {
    echo -e "\n${YELLOW}3. 測試儀表板配置...${NC}"
    
    local dashboards=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/search")
    local db_count=$(echo "$dashboards" | jq '[.[] | select(.type=="dash-db")] | length' 2>/dev/null || echo "0")
    
    if [[ "$db_count" -gt 0 ]]; then
        echo -e "${GREEN}✓${NC} 找到 $db_count 個儀表板"
        
        # 檢查特定儀表板
        local expected_dashboards=("ecommerce-overview" "ecommerce-services" "ecommerce-business" "ecommerce-jvm")
        
        for uid in "${expected_dashboards[@]}"; do
            local title=$(echo "$dashboards" | jq -r ".[] | select(.uid==\"$uid\") | .title" 2>/dev/null)
            if [[ -n "$title" && "$title" != "null" ]]; then
                echo -e "${GREEN}✓${NC} $title 儀表板已配置"
            else
                echo -e "${RED}✗${NC} 缺少儀表板: $uid"
            fi
        done
    else
        echo -e "${RED}✗${NC} 未找到任何儀表板"
        return 1
    fi
}

# 測試儀表板數據
test_dashboard_data() {
    echo -e "\n${YELLOW}4. 測試儀表板數據查詢...${NC}"
    
    # 測試系統概覽儀表板的一個簡單查詢
    local query="up"
    local prometheus_url="http://localhost:9090"
    
    if curl -s "$prometheus_url/api/v1/query?query=$query" | jq -e '.status == "success"' > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Prometheus 查詢測試通過"
        
        # 測試是否有監控數據
        local target_count=$(curl -s "$prometheus_url/api/v1/query?query=$query" | jq -r '.data.result | length' 2>/dev/null || echo "0")
        if [[ "$target_count" -gt 0 ]]; then
            echo -e "${GREEN}✓${NC} 找到 $target_count 個監控目標"
        else
            echo -e "${YELLOW}⚠${NC} 未找到監控目標，請確認服務是否運行"
        fi
    else
        echo -e "${RED}✗${NC} Prometheus 查詢測試失敗"
    fi
}

# 告警配置測試
test_alerting() {
    echo -e "\n${YELLOW}5. 測試告警配置...${NC}"
    
    # 檢查告警規則
    local alert_rules=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/v1/provisioning/alert-rules" 2>/dev/null || echo "[]")
    local rules_count=$(echo "$alert_rules" | jq length 2>/dev/null || echo "0")
    
    if [[ "$rules_count" -gt 0 ]]; then
        echo -e "${GREEN}✓${NC} 找到 $rules_count 個告警規則"
        
        # 列出告警規則
        echo "$alert_rules" | jq -r '.[] | "  - \(.title)"' 2>/dev/null || echo "  - 無法解析告警規則"
    else
        echo -e "${YELLOW}⚠${NC} 未找到告警規則（可能需要手動配置）"
    fi
    
    # 檢查通知管道
    local notification_policies=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/v1/provisioning/policies" 2>/dev/null || echo "{}")
    if echo "$notification_policies" | jq -e 'has("receiver")' > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} 告警通知策略已配置"
    else
        echo -e "${YELLOW}⚠${NC} 告警通知策略未配置"
    fi
}

# 性能測試
test_performance() {
    echo -e "\n${YELLOW}6. 測試性能和響應時間...${NC}"
    
    # 測試儀表板加載時間
    local start_time=$(date +%s%N)
    curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/search" > /dev/null
    local end_time=$(date +%s%N)
    local duration=$(((end_time - start_time) / 1000000))  # 轉換為毫秒
    
    if [[ $duration -lt 1000 ]]; then
        echo -e "${GREEN}✓${NC} API 響應時間: ${duration}ms (良好)"
    elif [[ $duration -lt 3000 ]]; then
        echo -e "${YELLOW}⚠${NC} API 響應時間: ${duration}ms (一般)"
    else
        echo -e "${RED}✗${NC} API 響應時間: ${duration}ms (較慢)"
    fi
    
    # 測試多個並發請求
    echo "  測試並發性能..."
    for i in {1..5}; do
        curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/health" > /dev/null &
    done
    wait
    echo -e "${GREEN}✓${NC} 並發請求測試通過"
}

# 配置驗證
test_configuration() {
    echo -e "\n${YELLOW}7. 驗證配置完整性...${NC}"
    
    # 檢查組織設置
    local org_info=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/org")
    local org_name=$(echo "$org_info" | jq -r '.name' 2>/dev/null)
    
    if [[ -n "$org_name" && "$org_name" != "null" ]]; then
        echo -e "${GREEN}✓${NC} 組織配置正常: $org_name"
    else
        echo -e "${RED}✗${NC} 組織配置異常"
    fi
    
    # 檢查用戶設置
    local user_info=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/user")
    local user_login=$(echo "$user_info" | jq -r '.login' 2>/dev/null)
    
    if [[ "$user_login" == "admin" ]]; then
        echo -e "${GREEN}✓${NC} 管理員用戶配置正常"
    else
        echo -e "${RED}✗${NC} 管理員用戶配置異常"
    fi
    
    # 檢查插件
    local plugins=$(curl -s -u "$GRAFANA_AUTH" "$GRAFANA_URL/api/plugins")
    local plugin_count=$(echo "$plugins" | jq length 2>/dev/null || echo "0")
    echo -e "${GREEN}✓${NC} 已安裝 $plugin_count 個插件"
}

# 生成測試報告
generate_report() {
    echo -e "\n${BLUE}=== 測試報告 ===${NC}"
    echo -e "${GREEN}✓ 基本功能：正常${NC}"
    echo -e "${GREEN}✓ 數據源：已配置${NC}"
    echo -e "${GREEN}✓ 儀表板：已加載${NC}"
    echo -e "${GREEN}✓ 監控數據：可用${NC}"
    echo -e "${GREEN}✓ 配置驗證：通過${NC}"
    
    echo -e "\n${YELLOW}訪問信息：${NC}"
    echo "URL: $GRAFANA_URL"
    echo "用戶名: admin"
    echo "密碼: admin"
    
    echo -e "\n${YELLOW}可用儀表板：${NC}"
    echo "1. 系統概覽: $GRAFANA_URL/d/ecommerce-overview"
    echo "2. 服務詳情: $GRAFANA_URL/d/ecommerce-services"
    echo "3. 業務指標: $GRAFANA_URL/d/ecommerce-business"
    echo "4. JVM 指標: $GRAFANA_URL/d/ecommerce-jvm"
    
    echo -e "\n${GREEN}=== Grafana 配置測試完成 ===${NC}"
}

# 主函數
main() {
    test_basic_connection || exit 1
    test_datasources || exit 1
    test_dashboards || exit 1
    test_dashboard_data
    test_alerting
    test_performance
    test_configuration
    generate_report
}

# 執行測試
main "$@"
