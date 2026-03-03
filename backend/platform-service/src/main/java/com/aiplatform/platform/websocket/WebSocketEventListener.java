package com.aiplatform.platform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Event Listener
 * Tracks WebSocket sessions and subscriptions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    // Track active sessions
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    /**
     * Handle new WebSocket connection
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setConnectedAt(System.currentTimeMillis());

        // Extract user info if available
        if (headerAccessor.getUser() != null) {
            sessionInfo.setUserId(headerAccessor.getUser().getName());
        }

        activeSessions.put(sessionId, sessionInfo);

        log.info("WebSocket connected: sessionId={}, activeSessions={}",
            sessionId, activeSessions.size());
    }

    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        SessionInfo removed = activeSessions.remove(sessionId);

        log.info("WebSocket disconnected: sessionId={}, duration={}ms",
            sessionId,
            removed != null ? System.currentTimeMillis() - removed.getConnectedAt() : 0);
    }

    /**
     * Handle subscription event
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo != null && destination != null) {
            sessionInfo.getSubscriptions().add(destination);
        }

        log.debug("WebSocket subscribed: sessionId={}, destination={}", sessionId, destination);
    }

    /**
     * Handle unsubscription event
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();

        log.debug("WebSocket unsubscribed: sessionId={}, subscriptionId={}", sessionId, subscriptionId);
    }

    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Get all active sessions
     */
    public Map<String, SessionInfo> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * Session information
     */
    @lombok.Data
    public static class SessionInfo {
        private String sessionId;
        private String userId;
        private Long connectedAt;
        private java.util.Set<String> subscriptions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }
}