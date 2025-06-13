#!/bin/bash

# E-commerce Platform 服務停止腳本

echo "🛑 Stopping E-commerce Platform services..."

# 切換到基礎設施目錄
cd "$(dirname "$0")/../infrastructure" || exit 1

# 停止所有容器
echo "Stopping Docker containers..."
docker-compose down

echo "✅ All services stopped successfully!"

echo ""
echo "🧹 To clean up completely (remove volumes):"
echo "docker-compose down -v"
