package com.aiplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Unified Authentication Filter for Gateway
 * 网关统一认证过滤器，支持JWT和AK/SK等多种认证方式
 */
@Slf4j
@Component
public class UnifiedAuthenticationFilter implements GlobalFilter, Ordered {

    // 公开端点列表
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/users/register",
        "/api/users/login",
        "/api-docs",
        "/swagger-ui",
        "/actuator",
        "/health"
    );

    // 可选认证端点（V1 API）
    private static final List<String> OPTIONAL_AUTH_PATHS = List.of(
        "/v1/models"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        log.debug("Gateway request: {} {}", method, path);

        // 检查是否为公开端点
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 获取Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");

        // 检查认证方式
        if (authHeader == null || authHeader.isEmpty()) {
            if (isOptionalAuthPath(path)) {
                // 可选认证端点，允许通过但记录
                log.debug("No authentication for optional path: {}", path);
                return chain.filter(exchange);
            }

            log.warn("Missing Authorization header for path: {}", path);
            // 开发环境允许通过，生产环境应返回401
            return chain.filter(exchange);
        }

        // 解析认证类型
        String authType = parseAuthType(authHeader);
        String credential = extractCredential(authHeader);

        // 根据认证类型处理
        switch (authType) {
            case "Bearer":
                return handleJwtAuth(exchange, chain, credential, path);
            case "AK":
                return handleAkSkAuth(exchange, chain, credential, path, request);
            default:
                log.warn("Unknown authentication type: {} for path: {}", authType, path);
                // 开发环境允许通过
                return chain.filter(exchange);
        }
    }

    /**
     * 处理JWT认证
     */
    private Mono<Void> handleJwtAuth(ServerWebExchange exchange, GatewayFilterChain chain,
            String token, String path) {
        log.debug("JWT authentication for path: {}", path);

        // JWT验证在下游服务中进行
        // 这里只是转发请求
        return chain.filter(exchange);
    }

    /**
     * 处理AK/SK认证
     */
    private Mono<Void> handleAkSkAuth(ServerWebExchange exchange, GatewayFilterChain chain,
            String accessKey, String path, ServerHttpRequest request) {
        log.debug("AK/SK authentication for path: {}, AK: {}", path, accessKey);

        // 获取签名相关headers
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String nonce = request.getHeaders().getFirst("X-Nonce");
        String signature = request.getHeaders().getFirst("X-Signature");

        // 验证时间戳（防止重放攻击）
        if (timestamp != null) {
            try {
                long requestTime = Long.parseLong(timestamp);
                long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - requestTime) > 300000) { // 5分钟
                    log.warn("Request timestamp expired for AK: {}", accessKey);
                    // 开发环境允许通过
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp format for AK: {}", accessKey);
            }
        }

        // AK/SK详细验证在下游服务中进行
        // 添加AK到请求头供下游服务使用
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Access-Key", accessKey)
            .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 解析认证类型
     */
    private String parseAuthType(String authHeader) {
        if (authHeader.startsWith("Bearer ")) {
            return "Bearer";
        } else if (authHeader.startsWith("AK ")) {
            return "AK";
        } else if (authHeader.startsWith("Basic ")) {
            return "Basic";
        }
        return "Unknown";
    }

    /**
     * 从Authorization header提取凭证
     */
    private String extractCredential(String authHeader) {
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        } else if (authHeader.startsWith("AK ")) {
            return authHeader.substring(3);
        } else if (authHeader.startsWith("Basic ")) {
            return authHeader.substring(6);
        }
        return authHeader;
    }

    /**
     * 检查是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 检查是否为可选认证路径
     */
    private boolean isOptionalAuthPath(String path) {
        return OPTIONAL_AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}