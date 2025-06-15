# 日志管理系统使用指南

## 🚀 快速开始

### 1. 启动服务

```bash
# 启动基础设施（包含Zipkin）
cd infrastructure
docker-compose up -d

# 启动各个服务
./scripts/start-services.sh
```

### 2. 访问监控界面

- **Zipkin UI**: http://localhost:9411
- **服务日志**: `logs/` 目录下各服务子目录

## 📝 日志查看

### 实时日志监控

```bash
# 查看用户服务实时日志
tail -f logs/user-service/user-service.log

# 查看所有服务的错误日志
tail -f logs/*/\*-error.log

# 查看特定请求的追踪日志
grep "traceId=abc123" logs/*/*.log
```

### 日志文件结构

```
logs/
├── user-service/
│   ├── user-service.log          # 主日志文件
│   ├── user-service-error.log    # 错误日志
│   ├── user-service-business.log # 业务日志
│   └── user-service-json.log     # JSON格式日志（生产环境）
├── product-service/
├── cart-service/
├── order-service/
├── inventory-service/
├── notification-service/
├── api-gateway/
├── eureka-server/
└── config-server/
```

## 🔍 分散式追踪

### 如何查看追踪信息

1. **打开Zipkin UI**: http://localhost:9411
2. **选择服务**: 从下拉菜单选择要查看的服务
3. **查看追踪**: 点击追踪记录查看详细时间线
4. **分析性能**: 查看各服务调用的耗时

### 追踪ID在日志中的格式

```
2025-06-15 22:00:00.123 [http-nio-8081-exec-1] INFO [user-service,64af12bc89ab,64af12bc89ab] com.ecommerce.user.controller.UserController - User login attempt for: john@example.com
```

- `64af12bc89ab`: TraceId (追踪ID)
- `64af12bc89ab`: SpanId (跨度ID)

## 🧪 测试日志功能

### 运行自动化测试

```bash
# 运行日志管理测试脚本
./scripts/test-logging-management.sh

# 查看测试报告
cat logs/log_analysis_report.txt
```

### 手动测试追踪

```bash
# 发送带追踪ID的请求
curl -H "X-Request-ID: test123" http://localhost:8080/api/v1/users/health

# 查看该请求的日志
grep "test123" logs/*/*.log
```

## 📊 日志分析

### 常用日志分析命令

```bash
# 统计错误日志数量
grep -c "ERROR" logs/user-service/user-service.log

# 查看最近的错误日志
grep "ERROR" logs/user-service/user-service.log | tail -10

# 统计特定时间段的请求
grep "2025-06-15 22:" logs/user-service/user-service.log | wc -l

# 查看最耗时的请求
grep "Duration:" logs/user-service/user-service.log | sort -k10 -nr | head -10
```

### 业务日志查询

```bash
# 查看用户注册日志
grep "User registered" logs/user-service/user-service-business.log

# 查看订单创建日志
grep "Order created" logs/order-service/order-service.log

# 查看库存变更日志
grep "Stock updated" logs/inventory-service/inventory-service-stock.log
```

## ⚙️ 配置说明

### 日志级别配置

- **开发环境**: DEBUG级别，详细日志输出
- **测试环境**: INFO级别，关键信息输出
- **生产环境**: WARN级别，警告和错误输出

### 修改日志级别

运行时修改日志级别（无需重启）：

```bash
# 查看当前日志级别
curl http://localhost:8081/actuator/loggers/com.ecommerce.user

# 修改日志级别为DEBUG
curl -X POST \
  http://localhost:8081/actuator/loggers/com.ecommerce.user \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel": "DEBUG"}'
```

### 追踪采样率配置

在 `application.yml` 中调整采样率：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100%采样（开发环境）
                        # 0.1   # 10%采样（生产环境）
```

## 🚨 故障排除

### 常见问题

#### 1. 日志文件未生成

**检查步骤**:
- 验证 `logs/` 目录权限
- 检查 `logback-spring.xml` 语法
- 查看应用启动日志中的错误

**解决方案**:
```bash
# 创建日志目录
mkdir -p logs/{user-service,product-service,cart-service,order-service,inventory-service,notification-service}

# 设置权限
chmod 755 logs/
```

#### 2. 追踪ID显示为空

**检查步骤**:
- 确认Zipkin服务运行状态
- 验证追踪配置是否正确
- 检查网络连接

**解决方案**:
```bash
# 检查Zipkin状态
curl http://localhost:9411/api/v2/services

# 重启Zipkin服务
docker-compose restart zipkin
```

#### 3. 日志文件过大

**解决方案**:
- 日志文件会自动轮转（每100MB或每天）
- 历史日志会自动压缩
- 超过30天的日志会自动删除

### 调试命令

```bash
# 检查服务健康状态
curl http://localhost:8081/actuator/health

# 查看日志配置
curl http://localhost:8081/actuator/loggers

# 查看追踪指标
curl http://localhost:8081/actuator/metrics | grep tracing

# 查看Zipkin连接状态
curl http://localhost:9411/health
```

## 📈 性能监控

### 日志性能指标

通过Prometheus监控日志相关指标：

```bash
# 查看日志输出速率
curl http://localhost:8081/actuator/prometheus | grep log

# 查看追踪延迟
curl http://localhost:8081/actuator/prometheus | grep tracing
```

### 优化建议

1. **生产环境优化**:
   - 降低追踪采样率到10-30%
   - 调整日志级别为WARN或ERROR
   - 使用异步日志输出

2. **存储优化**:
   - 定期清理旧日志文件
   - 配置日志压缩
   - 使用远程日志存储

3. **网络优化**:
   - 批量发送追踪数据
   - 配置追踪数据缓冲
   - 使用本地Zipkin代理

## 📚 更多资源

- [Zipkin官方文档](https://zipkin.io/)
- [Logback配置指南](http://logback.qos.ch/manual/)
- [Spring Boot Actuator指南](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Tracing文档](https://micrometer.io/docs/tracing)

---

**提示**: 如果遇到问题，请先查看服务的健康检查端点，然后检查对应的日志文件。大多数问题都可以通过日志找到根本原因。
