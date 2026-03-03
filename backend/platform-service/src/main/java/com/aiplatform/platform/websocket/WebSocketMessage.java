package com.aiplatform.platform.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket Message
 * Standard message format for WebSocket communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Message type
     */
    private MessageType type;

    /**
     * Message payload
     */
    private Object payload;

    /**
     * Timestamp
     */
    private Long timestamp;

    /**
     * Optional correlation ID for request/response matching
     */
    private String correlationId;

    public WebSocketMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * Message types supported by the platform
     */
    public enum MessageType {
        // Connection lifecycle
        CONNECT,
        DISCONNECT,
        HEARTBEAT,

        // Chat operations
        CHAT_REQUEST,
        CHAT_RESPONSE,
        CHAT_STREAM,
        CHAT_ERROR,

        // Conversation operations
        CONVERSATION_CREATED,
        CONVERSATION_UPDATED,
        CONVERSATION_DELETED,

        // Message operations
        MESSAGE_CREATED,
        MESSAGE_UPDATED,

        // Agent operations
        AGENT_STATUS,
        AGENT_THINKING,
        AGENT_TOOL_CALL,

        // System events
        NOTIFICATION,
        ERROR,

        // OpenCode Plugin specific
        PLUGIN_EVENT,
        PLUGIN_COMMAND,
        PLUGIN_RESPONSE
    }

    // Factory methods
    public static WebSocketMessage chatRequest(Object payload) {
        return new WebSocketMessage(MessageType.CHAT_REQUEST, payload);
    }

    public static WebSocketMessage chatResponse(Object payload) {
        return new WebSocketMessage(MessageType.CHAT_RESPONSE, payload);
    }

    public static WebSocketMessage chatStream(Object payload) {
        return new WebSocketMessage(MessageType.CHAT_STREAM, payload);
    }

    public static WebSocketMessage error(String message) {
        return new WebSocketMessage(MessageType.ERROR, message);
    }

    public static WebSocketMessage heartbeat() {
        return new WebSocketMessage(MessageType.HEARTBEAT, null);
    }
}