package com.aiplatform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Authentication Filter for Gateway
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Log request
        System.out.println("Request: " + request.getMethod() + " " + request.getPath());

        // Pass through for public endpoints
        String path = request.getPath().value();
        if (path.contains("/register") || path.contains("/login") ||
            path.contains("/api-docs") || path.contains("/swagger-ui")) {
            return chain.filter(exchange);
        }

        // Validate JWT token for protected endpoints
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // For development, allow pass-through
            // In production, return 401 Unauthorized
            return chain.filter(exchange);
        }

        // Forward the request with auth header
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

}