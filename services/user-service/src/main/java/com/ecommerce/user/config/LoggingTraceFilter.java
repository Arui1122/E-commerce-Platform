package com.ecommerce.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 日志追踪过滤器
 * 为每个请求生成唯一的requestId，并将用户信息添加到MDC中
 */
@Component
@Order(1)
@Slf4j
public class LoggingTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String X_USER_ID = "X-User-ID";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 生成或获取请求ID
            String requestId = getOrGenerateRequestId(request);
            MDC.put(REQUEST_ID, requestId);
            
            // 添加用户ID到MDC（如果存在）
            String userId = request.getHeader(X_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                MDC.put(USER_ID, userId);
            }
            
            // 将请求ID添加到响应头
            response.setHeader(X_REQUEST_ID, requestId);
            
            // 记录请求开始
            long startTime = System.currentTimeMillis();
            log.info("Request started: {} {} from {}", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    request.getRemoteAddr());
            
            // 继续过滤链
            filterChain.doFilter(request, response);
            
            // 记录请求完成
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    response.getStatus(), 
                    duration);
            
        } finally {
            // 清理MDC
            MDC.clear();
        }
    }

    /**
     * 获取或生成请求ID
     */
    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(X_REQUEST_ID);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        return requestId;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // 跳过健康检查和监控端点
        return path.startsWith("/actuator/") || 
               path.startsWith("/health") || 
               path.startsWith("/metrics");
    }
}
