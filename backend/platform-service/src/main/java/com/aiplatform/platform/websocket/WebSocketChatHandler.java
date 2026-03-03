package com.aiplatform.platform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket Chat Handler
 * Handles real-time chat communication via WebSocket
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle chat request
     * Client sends to /app/chat.request
     * Response is sent to /topic/chat.response/{conversationId}
     */
    @MessageMapping("/chat.request")
    public void handleChatRequest(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("Received chat request: {}", message);

        try {
            // Extract user info
            Principal user = headerAccessor.getUser();
            String sessionId = headerAccessor.getSessionId();

            // Extract conversation ID from payload
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long conversationId = getConversationId(payload);

            // Forward to connection service via Kafka
            // This would typically publish to a Kafka topic
            // For now, we'll just acknowledge the request

            WebSocketMessage response = new WebSocketMessage(
                WebSocketMessage.MessageType.CHAT_RESPONSE,
                Map.of(
                    "status", "processing",
                    "conversationId", conversationId,
                    "correlationId", message.getCorrelationId()
                )
            );

            // Send acknowledgment to user
            messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/chat.status",
                response
            );

        } catch (Exception e) {
            log.error("Failed to process chat request", e);
            sendError(headerAccessor, e.getMessage());
        }
    }

    /**
     * Handle streaming chat request
     * Client sends to /app/chat.stream
     * Stream responses sent to /topic/chat.stream/{conversationId}
     */
    @MessageMapping("/chat.stream")
    public void handleStreamRequest(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("Received stream request: {}", message);

        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long conversationId = getConversationId(payload);

            // This would trigger a streaming request to the connection service
            // Stream chunks would be sent back via Kafka events

            WebSocketMessage response = new WebSocketMessage(
                WebSocketMessage.MessageType.CHAT_STREAM,
                Map.of("status", "streaming", "conversationId", conversationId)
            );

            messagingTemplate.convertAndSend(
                "/topic/chat.stream/" + conversationId,
                response
            );

        } catch (Exception e) {
            log.error("Failed to process stream request", e);
            sendError(headerAccessor, e.getMessage());
        }
    }

    /**
     * Handle heartbeat
     * Client sends to /app/heartbeat
     */
    @MessageMapping("/heartbeat")
    @SendTo("/topic/heartbeat")
    public WebSocketMessage handleHeartbeat(@Payload WebSocketMessage message) {
        return WebSocketMessage.heartbeat();
    }

    /**
     * Handle connection
     * Client sends to /app/connect
     */
    @MessageMapping("/connect")
    public void handleConnect(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        log.info("Client connected: {}", headerAccessor.getSessionId());

        WebSocketMessage response = new WebSocketMessage(
            WebSocketMessage.MessageType.CONNECT,
            Map.of("sessionId", headerAccessor.getSessionId())
        );

        messagingTemplate.convertAndSendToUser(
            headerAccessor.getSessionId(),
            "/queue/connection",
            response
        );
    }

    /**
     * Send streaming chunk to specific conversation
     */
    public void sendStreamChunk(Long conversationId, Object chunk) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessage.MessageType.CHAT_STREAM,
            chunk
        );

        messagingTemplate.convertAndSend(
            "/topic/chat.stream/" + conversationId,
            message
        );
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(String userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }

    /**
     * Broadcast to all subscribers
     */
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    private Long getConversationId(Map<String, Object> payload) {
        Object id = payload.get("conversationId");
        if (id == null) id = payload.get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return null;
    }

    private void sendError(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        WebSocketMessage error = WebSocketMessage.error(errorMessage);
        messagingTemplate.convertAndSendToUser(
            headerAccessor.getSessionId(),
            "/queue/errors",
            error
        );
    }
}