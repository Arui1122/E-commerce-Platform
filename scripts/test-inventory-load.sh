#!/bin/bash

# Inventory Service Load Test
# 庫存服務負載測試

BASE_URL="http://localhost:8085/api/v1/inventory"
PRODUCT_ID=888
DURATION=30  # seconds
CONCURRENCY=10

echo "📊 Inventory Service Load Test"
echo "============================="
echo "Target URL: $BASE_URL"
echo "Test Duration: ${DURATION}s"
echo "Concurrency: $CONCURRENCY"
echo ""

# Setup test product
echo "📦 Setting up test product..."
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"productId\": $PRODUCT_ID,
    \"quantity\": 10000
  }" > /dev/null

echo "✅ Test product created with 10,000 units"
echo ""

# Function to run continuous requests
run_load_test() {
    local test_type=$1
    local endpoint=$2
    local method=$3
    local data=$4
    
    echo "🔥 Starting $test_type load test..."
    
    if command -v ab > /dev/null; then
        # Using Apache Bench if available
        if [ "$method" = "GET" ]; then
            ab -t $DURATION -c $CONCURRENCY "$endpoint"
        else
            echo "POST load test with ab requires additional setup"
        fi
    elif command -v curl > /dev/null; then
        # Fallback to curl with manual timing
        echo "Running curl-based load test for ${DURATION}s..."
        
        start_time=$(date +%s)
        end_time=$((start_time + DURATION))
        request_count=0
        success_count=0
        
        # Run requests for specified duration
        while [ $(date +%s) -lt $end_time ]; do
            for i in $(seq 1 $CONCURRENCY); do
                if [ "$method" = "GET" ]; then
                    response=$(curl -s -w "%{http_code}" -o /dev/null "$endpoint")
                else
                    response=$(curl -s -w "%{http_code}" -o /dev/null -X "$method" \
                        -H "Content-Type: application/json" \
                        -d "$data" "$endpoint")
                fi
                
                ((request_count++))
                if [ "$response" = "200" ]; then
                    ((success_count++))
                fi
            done
        done
        
        actual_duration=$(($(date +%s) - start_time))
        rps=$((request_count / actual_duration))
        success_rate=$((success_count * 100 / request_count))
        
        echo "📈 Results for $test_type:"
        echo "  Total Requests: $request_count"
        echo "  Successful: $success_count"
        echo "  Success Rate: ${success_rate}%"
        echo "  Duration: ${actual_duration}s"
        echo "  RPS: $rps"
        echo ""
    else
        echo "❌ No load testing tool available (ab or curl)"
        return 1
    fi
}

# Test 1: Inventory Query Load Test
run_load_test "Inventory Query" "$BASE_URL/$PRODUCT_ID" "GET" ""

# Test 2: Stock Check Load Test  
run_load_test "Stock Check" "$BASE_URL/check/$PRODUCT_ID?quantity=1" "GET" ""

# Test 3: Stock Reservation Load Test
reservation_data="{\"productId\": $PRODUCT_ID, \"quantity\": 1, \"referenceId\": \"LOAD-TEST-\$(date +%s%N)\"}"
run_load_test "Stock Reservation" "$BASE_URL/reserve" "POST" "$reservation_data"

echo "🏁 Load test completed!"
echo ""

# Check final inventory state
echo "📊 Final inventory state:"
curl -s -X GET "$BASE_URL/$PRODUCT_ID" | jq .

echo ""
echo "💡 Tips for better load testing:"
echo "  - Install Apache Bench: brew install httpd (macOS) or apt-get install apache2-utils (Ubuntu)"
echo "  - Use specialized tools like wrk, hey, or JMeter for comprehensive testing"
echo "  - Monitor system resources during load tests"
echo "  - Test with realistic data patterns and user behavior"
