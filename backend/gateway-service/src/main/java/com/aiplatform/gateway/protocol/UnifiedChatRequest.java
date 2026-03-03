package com.aiplatform.gateway.protocol;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Unified Chat Request
 * Standard internal format for all chat requests
 */
@Data
public class UnifiedChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Boolean stream;
    private List<ToolDefinition> tools;
    private Map<String, Object> additionalProperties;

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
        private String name;
        private List<ToolCall> toolCalls;
        private String toolCallId;
    }

    @Data
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    @Data
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    @Data
    public static class ToolDefinition {
        private String type;
        private FunctionDef function;
    }

    @Data
    public static class FunctionDef {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}