package com.aiplatform.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket Configuration
 * Enables WebSocket for real-time communication with CUI frontend
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker
        // Messages with destinations starting with /topic are for pub/sub
        // Messages with destinations starting with /queue are for point-to-point
        registry.enableSimpleBroker("/topic", "/queue");

        // Set prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Set prefix for user-specific destinations
        registry.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(new WebSocketAuthInterceptor())
            .withSockJS();

        // Also register a raw WebSocket endpoint (no SockJS fallback)
        registry.addEndpoint("/ws/raw")
            .setAllowedOriginPatterns("*")
            .addInterceptors(new WebSocketAuthInterceptor());

        log.info("WebSocket STOMP endpoints registered at /ws and /ws/raw");
    }
}