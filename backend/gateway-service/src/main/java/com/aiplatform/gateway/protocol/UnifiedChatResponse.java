package com.aiplatform.gateway.protocol;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Unified Chat Response
 * Standard internal format for all chat responses
 */
@Data
public class UnifiedChatResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<ChatChoice> choices;
    private Usage usage;
    private Map<String, Object> additionalProperties;

    @Data
    public static class ChatChoice {
        private Integer index;
        private ChatMessage message;
        private ChatMessage delta;
        private String finishReason;
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
        private List<ToolCall> toolCalls;
        private ThinkingContent thinking;
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
    public static class ThinkingContent {
        private String content;
        private String signature;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}