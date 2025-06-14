# Multi-stage build
FROM openjdk:21-jdk-slim as build

WORKDIR /app

# Copy maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn clean package -DskipTests

# Production stage
FROM openjdk:21-jre-slim

WORKDIR /app

# Create non-root user
RUN useradd -r -s /bin/false appuser

# Copy the built jar
COPY --from=build /app/target/user-service-1.0.0.jar app.jar

# Change ownership
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Expose port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
