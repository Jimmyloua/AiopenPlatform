package com.aiplatform.platform.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Webhook Event
 * Standard event format for webhook notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    /**
     * Unique event ID
     */
    private String eventId;

    /**
     * Event type
     */
    private EventType eventType;

    /**
     * Event timestamp
     */
    private Long timestamp;

    /**
     * Event payload
     */
    private Map<String, Object> payload;

    /**
     * Source service
     */
    private String source;

    /**
     * Target service (optional)
     */
    private String target;

    /**
     * Event types
     */
    public enum EventType {
        // Message events
        MESSAGE_CREATED,
        MESSAGE_UPDATED,
        MESSAGE_DELETED,

        // Conversation events
        CONVERSATION_CREATED,
        CONVERSATION_UPDATED,
        CONVERSATION_DELETED,

        // Agent events
        AGENT_STARTED,
        AGENT_COMPLETED,
        AGENT_ERROR,
        AGENT_TOOL_CALL,

        // Stream events
        STREAM_STARTED,
        STREAM_CHUNK,
        STREAM_COMPLETED,
        STREAM_ERROR,

        // System events
        USER_REGISTERED,
        ASSISTANT_PUBLISHED,
        ASSISTANT_APPROVED,

        // OpenCode Plugin events
        PLUGIN_REGISTERED,
        PLUGIN_EVENT,
        PLUGIN_COMMAND
    }

    // Factory methods
    public static WebhookEvent messageCreated(Long conversationId, Long messageId, String content) {
        return WebhookEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType(EventType.MESSAGE_CREATED)
            .timestamp(Instant.now().toEpochMilli())
            .payload(Map.of(
                "conversationId", conversationId,
                "messageId", messageId,
                "content", content
            ))
            .build();
    }

    public static WebhookEvent streamChunk(Long conversationId, String chunk) {
        return WebhookEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType(EventType.STREAM_CHUNK)
            .timestamp(Instant.now().toEpochMilli())
            .payload(Map.of(
                "conversationId", conversationId,
                "chunk", chunk
            ))
            .build();
    }

    public static WebhookEvent agentError(Long conversationId, String error) {
        return WebhookEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType(EventType.AGENT_ERROR)
            .timestamp(Instant.now().toEpochMilli())
            .payload(Map.of(
                "conversationId", conversationId,
                "error", error
            ))
            .build();
    }
}