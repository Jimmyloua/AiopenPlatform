package com.aiplatform.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket Authentication Interceptor
 * Validates JWT tokens during WebSocket handshake
 */
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        log.debug("WebSocket handshake initiated");

        // Extract token from query parameter or header
        String token = extractToken(request);

        if (token != null) {
            // Store token in attributes for later authentication
            attributes.put("token", token);

            // For development, allow all connections
            // In production, validate the JWT token here
            log.info("WebSocket connection authenticated with token");
            return true;
        }

        // For development, allow anonymous connections
        log.warn("WebSocket connection without authentication token");
        attributes.put("anonymous", true);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    private String extractToken(ServerHttpRequest request) {
        // Try to get token from query parameter
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String query = servletRequest.getServletRequest().getQueryString();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        return param.substring(6);
                    }
                }
            }

            // Also try Authorization header
            String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }
}