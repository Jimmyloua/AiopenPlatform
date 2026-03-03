package com.aiplatform.gateway.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Claude Protocol Adapter
 * Handles Anthropic Claude models (claude-3-opus, claude-3-sonnet, etc.)
 *
 * Claude API format differences from OpenAI:
 * - Uses "content" as array of content blocks instead of string
 * - System prompt is separate from messages
 * - Different streaming format
 */
@Slf4j
@Component
public class ClaudeAdapter extends ProtocolAdapter {

    private static final String PROVIDER = "claude";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "claude-3-opus", "claude-3-sonnet", "claude-3-haiku",
        "claude-3-5-sonnet", "claude-3-5-haiku",
        "claude-opus-4", "claude-sonnet-4"
    );

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public UnifiedChatRequest toUnified(String requestBody) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestBody,
                new TypeReference<Map<String, Object>>() {});

            UnifiedChatRequest unified = new UnifiedChatRequest();
            unified.setModel((String) request.get("model"));
            unified.setMaxTokens(getInteger(request, "max_tokens"));

            // Claude uses temperature differently
            if (request.get("temperature") != null) {
                unified.setTemperature(((Number) request.get("temperature")).doubleValue());
            }

            // Claude uses top_k instead of top_p
            if (request.get("top_p") != null) {
                unified.setTopP(((Number) request.get("top_p")).doubleValue());
            }

            unified.setStream(getBoolean(request, "stream"));

            // Parse system prompt (Claude specific)
            String systemPrompt = (String) request.get("system");

            // Parse messages
            List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
            if (messages != null) {
                List<UnifiedChatRequest.ChatMessage> unifiedMessages = new ArrayList<>();

                // Add system prompt as first message if present
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    UnifiedChatRequest.ChatMessage systemMsg = new UnifiedChatRequest.ChatMessage();
                    systemMsg.setRole("system");
                    systemMsg.setContent(systemPrompt);
                    unifiedMessages.add(systemMsg);
                }

                for (Map<String, Object> msg : messages) {
                    UnifiedChatRequest.ChatMessage unifiedMsg = new UnifiedChatRequest.ChatMessage();
                    unifiedMsg.setRole(convertRole((String) msg.get("role")));

                    // Claude content can be string or array
                    Object content = msg.get("content");
                    if (content instanceof String) {
                        unifiedMsg.setContent((String) content);
                    } else if (content instanceof List) {
                        unifiedMsg.setContent(extractTextFromContent((List<Map<String, Object>>) content));
                    }

                    unifiedMessages.add(unifiedMsg);
                }
                unified.setMessages(unifiedMessages);
            }

            // Parse tools (Claude format)
            List<Map<String, Object>> tools = (List<Map<String, Object>>) request.get("tools");
            if (tools != null) {
                unified.setTools(parseClaudeTools(tools));
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse Claude request", e);
            throw new RuntimeException("Invalid Claude request format", e);
        }
    }

    @Override
    public String fromUnified(UnifiedChatRequest request) {
        try {
            Map<String, Object> claudeRequest = new LinkedHashMap<>();
            claudeRequest.put("model", request.getModel());

            // Extract system message
            String systemPrompt = null;
            List<UnifiedChatRequest.ChatMessage> filteredMessages = new ArrayList<>();

            if (request.getMessages() != null) {
                for (UnifiedChatRequest.ChatMessage msg : request.getMessages()) {
                    if ("system".equals(msg.getRole())) {
                        systemPrompt = msg.getContent();
                    } else {
                        filteredMessages.add(msg);
                    }
                }
            }

            if (systemPrompt != null) {
                claudeRequest.put("system", systemPrompt);
            }

            // Convert messages to Claude format
            if (!filteredMessages.isEmpty()) {
                List<Map<String, Object>> messages = new ArrayList<>();
                for (UnifiedChatRequest.ChatMessage msg : filteredMessages) {
                    Map<String, Object> msgMap = new LinkedHashMap<>();
                    msgMap.put("role", convertToClaudeRole(msg.getRole()));
                    msgMap.put("content", msg.getContent());
                    messages.add(msgMap);
                }
                claudeRequest.put("messages", messages);
            }

            if (request.getMaxTokens() != null) {
                claudeRequest.put("max_tokens", request.getMaxTokens());
            } else {
                claudeRequest.put("max_tokens", 4096); // Default for Claude
            }

            if (request.getTemperature() != null) {
                claudeRequest.put("temperature", request.getTemperature());
            }
            if (request.getTopP() != null) {
                claudeRequest.put("top_p", request.getTopP());
            }
            if (request.getStream() != null) {
                claudeRequest.put("stream", request.getStream());
            }
            if (request.getTools() != null) {
                claudeRequest.put("tools", convertToClaudeTools(request.getTools()));
            }

            return objectMapper.writeValueAsString(claudeRequest);
        } catch (Exception e) {
            log.error("Failed to convert to Claude format", e);
            throw new RuntimeException("Failed to convert request", e);
        }
    }

    @Override
    public UnifiedChatResponse toUnifiedResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {});

            UnifiedChatResponse unified = new UnifiedChatResponse();
            unified.setId((String) response.get("id"));
            unified.setModel((String) response.get("model"));

            // Claude uses different type
            unified.setObject("chat.completion");

            if (response.get("created") == null) {
                unified.setCreated(System.currentTimeMillis() / 1000);
            }

            // Parse content
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            String textContent = null;
            String thinkingContent = null;

            if (content != null) {
                for (Map<String, Object> block : content) {
                    String type = (String) block.get("type");
                    if ("text".equals(type)) {
                        textContent = (String) block.get("text");
                    } else if ("thinking".equals(type)) {
                        thinkingContent = (String) block.get("thinking");
                    }
                }
            }

            // Build choices
            UnifiedChatResponse.ChatChoice choice = new UnifiedChatResponse.ChatChoice();
            choice.setIndex(0);

            UnifiedChatResponse.ChatMessage message = new UnifiedChatResponse.ChatMessage();
            message.setRole("assistant");
            message.setContent(textContent);

            if (thinkingContent != null) {
                UnifiedChatResponse.ThinkingContent thinking = new UnifiedChatResponse.ThinkingContent();
                thinking.setContent(thinkingContent);
                message.setThinking(thinking);
            }

            choice.setMessage(message);

            String stopReason = (String) response.get("stop_reason");
            choice.setFinishReason(convertStopReason(stopReason));

            unified.setChoices(List.of(choice));

            // Parse usage
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage != null) {
                UnifiedChatResponse.Usage unifiedUsage = new UnifiedChatResponse.Usage();
                unifiedUsage.setPromptTokens(getInt(usage, "input_tokens"));
                unifiedUsage.setCompletionTokens(getInt(usage, "output_tokens"));
                unifiedUsage.setTotalTokens(
                    (unifiedUsage.getPromptTokens() != null ? unifiedUsage.getPromptTokens() : 0) +
                    (unifiedUsage.getCompletionTokens() != null ? unifiedUsage.getCompletionTokens() : 0)
                );
                unified.setUsage(unifiedUsage);
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse Claude response", e);
            throw new RuntimeException("Invalid Claude response format", e);
        }
    }

    @Override
    public String fromUnifiedResponse(UnifiedChatResponse response) {
        try {
            Map<String, Object> claudeResponse = new LinkedHashMap<>();
            claudeResponse.put("id", response.getId() != null ? response.getId() : UUID.randomUUID().toString());
            claudeResponse.put("type", "message");
            claudeResponse.put("role", "assistant");
            claudeResponse.put("model", response.getModel());

            // Build content blocks
            List<Map<String, Object>> content = new ArrayList<>();

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                UnifiedChatResponse.ChatMessage msg = response.getChoices().get(0).getMessage();
                if (msg != null) {
                    // Add thinking block if present
                    if (msg.getThinking() != null && msg.getThinking().getContent() != null) {
                        Map<String, Object> thinkingBlock = new LinkedHashMap<>();
                        thinkingBlock.put("type", "thinking");
                        thinkingBlock.put("thinking", msg.getThinking().getContent());
                        content.add(thinkingBlock);
                    }

                    // Add text block
                    if (msg.getContent() != null) {
                        Map<String, Object> textBlock = new LinkedHashMap<>();
                        textBlock.put("type", "text");
                        textBlock.put("text", msg.getContent());
                        content.add(textBlock);
                    }
                }

                String finishReason = response.getChoices().get(0).getFinishReason();
                claudeResponse.put("stop_reason", convertToClaudeStopReason(finishReason));
            }

            claudeResponse.put("content", content);

            if (response.getUsage() != null) {
                Map<String, Object> usage = new LinkedHashMap<>();
                usage.put("input_tokens", response.getUsage().getPromptTokens());
                usage.put("output_tokens", response.getUsage().getCompletionTokens());
                claudeResponse.put("usage", usage);
            }

            return objectMapper.writeValueAsString(claudeResponse);
        } catch (Exception e) {
            log.error("Failed to convert to Claude response format", e);
            throw new RuntimeException("Failed to convert response", e);
        }
    }

    @Override
    public List<UnifiedChatResponse> parseStreamChunk(String chunk) {
        List<UnifiedChatResponse> responses = new ArrayList<>();

        String[] lines = chunk.split("\n");
        for (String line : lines) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                try {
                    Map<String, Object> event = objectMapper.readValue(data,
                        new TypeReference<Map<String, Object>>() {});

                    String type = (String) event.get("type");
                    UnifiedChatResponse response = parseStreamEvent(type, event);
                    if (response != null) {
                        responses.add(response);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse Claude stream chunk: {}", data);
                }
            }
        }

        return responses;
    }

    @Override
    public boolean supportsModel(String model) {
        if (model == null) return false;
        String lowerModel = model.toLowerCase();
        return SUPPORTED_MODELS.stream().anyMatch(lowerModel::contains);
    }

    // Helper methods
    private UnifiedChatResponse parseStreamEvent(String type, Map<String, Object> event) {
        UnifiedChatResponse response = new UnifiedChatResponse();
        response.setId((String) event.get("message_id"));
        response.setModel((String) event.get("model"));
        response.setObject("chat.completion.chunk");
        response.setCreated(System.currentTimeMillis() / 1000);

        UnifiedChatResponse.ChatChoice choice = new UnifiedChatResponse.ChatChoice();
        choice.setIndex(0);

        UnifiedChatResponse.ChatMessage delta = new UnifiedChatResponse.ChatMessage();

        switch (type) {
            case "content_block_delta":
                Map<String, Object> delta = (Map<String, Object>) event.get("delta");
                if (delta != null) {
                    String text = (String) delta.get("text");
                    if (text != null) {
                        ChatMessage msg = new ChatMessage();
                        msg.setContent(text);
                        choice.setDelta(msg);
                    }
                }
                break;

            case "content_block_start":
                // Start of a content block
                break;

            case "content_block_stop":
                // End of a content block
                break;

            case "message_delta":
                Map<String, Object> msgDelta = (Map<String, Object>) event.get("delta");
                if (msgDelta != null) {
                    String stopReason = (String) msgDelta.get("stop_reason");
                    if (stopReason != null) {
                        choice.setFinishReason(convertStopReason(stopReason));
                    }
                }
                break;

            case "message_start":
            case "message_stop":
                // Message lifecycle events
                return null;

            default:
                return null;
        }

        response.setChoices(List.of(choice));
        return response;
    }

    private String convertRole(String role) {
        if ("assistant".equals(role)) return "assistant";
        if ("user".equals(role)) return "user";
        return role;
    }

    private String convertToClaudeRole(String role) {
        // Claude only supports "user" and "assistant"
        if ("assistant".equals(role)) return "assistant";
        return "user";
    }

    private String convertStopReason(String stopReason) {
        if (stopReason == null) return null;
        switch (stopReason) {
            case "end_turn":
                return "stop";
            case "max_tokens":
                return "length";
            case "stop_sequence":
                return "stop";
            case "tool_use":
                return "tool_calls";
            default:
                return stopReason;
        }
    }

    private String convertToClaudeStopReason(String finishReason) {
        if (finishReason == null) return null;
        switch (finishReason) {
            case "stop":
                return "end_turn";
            case "length":
                return "max_tokens";
            case "tool_calls":
                return "tool_use";
            default:
                return finishReason;
        }
    }

    private String extractTextFromContent(List<Map<String, Object>> content) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> block : content) {
            if ("text".equals(block.get("type"))) {
                sb.append(block.get("text"));
            }
        }
        return sb.toString();
    }

    private List<UnifiedChatRequest.ToolDefinition> parseClaudeTools(List<Map<String, Object>> tools) {
        List<UnifiedChatRequest.ToolDefinition> result = new ArrayList<>();

        for (Map<String, Object> tool : tools) {
            UnifiedChatRequest.ToolDefinition td = new UnifiedChatRequest.ToolDefinition();
            td.setType((String) tool.get("type"));

            Map<String, Object> function = (Map<String, Object>) tool.get("function");
            if (function != null) {
                UnifiedChatRequest.FunctionDef fd = new UnifiedChatRequest.FunctionDef();
                fd.setName((String) function.get("name"));
                fd.setDescription((String) function.get("description"));
                fd.setParameters((Map<String, Object>) function.get("parameters"));
                td.setFunction(fd);
            }
            result.add(td);
        }

        return result;
    }

    private List<Map<String, Object>> convertToClaudeTools(List<UnifiedChatRequest.ToolDefinition> tools) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (UnifiedChatRequest.ToolDefinition td : tools) {
            Map<String, Object> tdMap = new LinkedHashMap<>();
            tdMap.put("name", td.getFunction().getName());
            tdMap.put("description", td.getFunction().getDescription());
            tdMap.put("input_schema", td.getFunction().getParameters());
            result.add(tdMap);
        }

        return result;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return (Boolean) value;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }
}