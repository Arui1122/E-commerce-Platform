# 使用多階段建構來減小鏡像大小
FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# 複製 Maven 配置文件
COPY pom.xml .
COPY src ./src

# 安裝 Maven
RUN apt-get update && apt-get install -y maven

# 構建應用
RUN mvn clean package -DskipTests

# 運行階段
FROM openjdk:17-jre-slim

WORKDIR /app

# 創建非 root 用戶
RUN groupadd -r spring && useradd -r -g spring spring

# 複製構建好的 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 設置文件權限
RUN chown spring:spring app.jar

USER spring

# 健康檢查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

# 暴露端口
EXPOSE 8761

# 啟動應用
ENTRYPOINT ["java", "-jar", "app.jar"]
