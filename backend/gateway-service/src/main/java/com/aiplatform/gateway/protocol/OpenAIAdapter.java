package com.aiplatform.gateway.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * OpenAI Protocol Adapter
 * Handles OpenAI GPT models (gpt-4, gpt-3.5-turbo, etc.)
 */
@Slf4j
@Component
public class OpenAIAdapter extends ProtocolAdapter {

    private static final String PROVIDER = "openai";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "gpt-4", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini",
        "gpt-3.5-turbo", "gpt-3.5-turbo-16k"
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
            unified.setTemperature(getDouble(request, "temperature"));
            unified.setTopP(getDouble(request, "top_p"));
            unified.setMaxTokens(getInteger(request, "max_tokens"));
            unified.setStream(getBoolean(request, "stream"));

            // Parse messages
            List<Map<String, Object>> messages = (List<Map<String, Object>>) request.get("messages");
            if (messages != null) {
                List<UnifiedChatRequest.ChatMessage> unifiedMessages = new ArrayList<>();
                for (Map<String, Object> msg : messages) {
                    UnifiedChatRequest.ChatMessage unifiedMsg = new UnifiedChatRequest.ChatMessage();
                    unifiedMsg.setRole((String) msg.get("role"));
                    unifiedMsg.setContent((String) msg.get("content"));
                    unifiedMsg.setName((String) msg.get("name"));
                    unifiedMsg.setToolCallId((String) msg.get("tool_call_id"));

                    if (msg.get("tool_calls") != null) {
                        unifiedMsg.setToolCalls(parseToolCalls(msg.get("tool_calls")));
                    }
                    unifiedMessages.add(unifiedMsg);
                }
                unified.setMessages(unifiedMessages);
            }

            // Parse tools
            List<Map<String, Object>> tools = (List<Map<String, Object>>) request.get("tools");
            if (tools != null) {
                unified.setTools(parseTools(tools));
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI request", e);
            throw new RuntimeException("Invalid OpenAI request format", e);
        }
    }

    @Override
    public String fromUnified(UnifiedChatRequest request) {
        try {
            Map<String, Object> openaiRequest = new LinkedHashMap<>();
            openaiRequest.put("model", request.getModel());

            if (request.getMessages() != null) {
                List<Map<String, Object>> messages = new ArrayList<>();
                for (UnifiedChatRequest.ChatMessage msg : request.getMessages()) {
                    Map<String, Object> msgMap = new LinkedHashMap<>();
                    msgMap.put("role", msg.getRole());
                    msgMap.put("content", msg.getContent());
                    if (msg.getName() != null) msgMap.put("name", msg.getName());
                    if (msg.getToolCallId() != null) msgMap.put("tool_call_id", msg.getToolCallId());
                    if (msg.getToolCalls() != null) {
                        msgMap.put("tool_calls", convertToolCalls(msg.getToolCalls()));
                    }
                    messages.add(msgMap);
                }
                openaiRequest.put("messages", messages);
            }

            if (request.getTemperature() != null) openaiRequest.put("temperature", request.getTemperature());
            if (request.getTopP() != null) openaiRequest.put("top_p", request.getTopP());
            if (request.getMaxTokens() != null) openaiRequest.put("max_tokens", request.getMaxTokens());
            if (request.getStream() != null) openaiRequest.put("stream", request.getStream());
            if (request.getTools() != null) openaiRequest.put("tools", convertTools(request.getTools()));

            return objectMapper.writeValueAsString(openaiRequest);
        } catch (Exception e) {
            log.error("Failed to convert to OpenAI format", e);
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
            unified.setObject((String) response.get("object"));
            unified.setModel((String) response.get("model"));

            if (response.get("created") != null) {
                unified.setCreated(((Number) response.get("created")).longValue());
            }

            // Parse choices
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null) {
                unified.setChoices(parseChoices(choices));
            }

            // Parse usage
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            if (usage != null) {
                UnifiedChatResponse.Usage unifiedUsage = new UnifiedChatResponse.Usage();
                unifiedUsage.setPromptTokens(getInt(usage, "prompt_tokens"));
                unifiedUsage.setCompletionTokens(getInt(usage, "completion_tokens"));
                unifiedUsage.setTotalTokens(getInt(usage, "total_tokens"));
                unified.setUsage(unifiedUsage);
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new RuntimeException("Invalid OpenAI response format", e);
        }
    }

    @Override
    public String fromUnifiedResponse(UnifiedChatResponse response) {
        try {
            Map<String, Object> openaiResponse = new LinkedHashMap<>();
            openaiResponse.put("id", response.getId());
            openaiResponse.put("object", response.getObject() != null ? response.getObject() : "chat.completion");
            openaiResponse.put("created", response.getCreated() != null ? response.getCreated() : System.currentTimeMillis() / 1000);
            openaiResponse.put("model", response.getModel());

            if (response.getChoices() != null) {
                openaiResponse.put("choices", convertChoices(response.getChoices()));
            }

            if (response.getUsage() != null) {
                Map<String, Object> usage = new LinkedHashMap<>();
                usage.put("prompt_tokens", response.getUsage().getPromptTokens());
                usage.put("completion_tokens", response.getUsage().getCompletionTokens());
                usage.put("total_tokens", response.getUsage().getTotalTokens());
                openaiResponse.put("usage", usage);
            }

            return objectMapper.writeValueAsString(openaiResponse);
        } catch (Exception e) {
            log.error("Failed to convert to OpenAI response format", e);
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
                if ("[DONE]".equals(data)) {
                    continue;
                }

                try {
                    UnifiedChatResponse response = toUnifiedResponse(data);
                    response.setObject("chat.completion.chunk");
                    responses.add(response);
                } catch (Exception e) {
                    log.warn("Failed to parse stream chunk: {}", data);
                }
            }
        }

        return responses;
    }

    @Override
    public boolean supportsModel(String model) {
        if (model == null) return false;
        return SUPPORTED_MODELS.stream().anyMatch(model::startsWith);
    }

    // Helper methods
    private List<UnifiedChatRequest.ToolCall> parseToolCalls(Object toolCallsObj) {
        List<Map<String, Object>> toolCallsList = (List<Map<String, Object>>) toolCallsObj;
        List<UnifiedChatRequest.ToolCall> result = new ArrayList<>();

        for (Map<String, Object> tc : toolCallsList) {
            UnifiedChatRequest.ToolCall toolCall = new UnifiedChatRequest.ToolCall();
            toolCall.setId((String) tc.get("id"));
            toolCall.setType((String) tc.get("type"));

            Map<String, Object> function = (Map<String, Object>) tc.get("function");
            if (function != null) {
                UnifiedChatRequest.FunctionCall fc = new UnifiedChatRequest.FunctionCall();
                fc.setName((String) function.get("name"));
                fc.setArguments((String) function.get("arguments"));
                toolCall.setFunction(fc);
            }
            result.add(toolCall);
        }

        return result;
    }

    private List<UnifiedChatRequest.ToolDefinition> parseTools(List<Map<String, Object>> tools) {
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

    private List<Map<String, Object>> convertToolCalls(List<UnifiedChatRequest.ToolCall> toolCalls) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (UnifiedChatRequest.ToolCall tc : toolCalls) {
            Map<String, Object> tcMap = new LinkedHashMap<>();
            tcMap.put("id", tc.getId());
            tcMap.put("type", tc.getType() != null ? tc.getType() : "function");

            if (tc.getFunction() != null) {
                Map<String, Object> fcMap = new LinkedHashMap<>();
                fcMap.put("name", tc.getFunction().getName());
                fcMap.put("arguments", tc.getFunction().getArguments());
                tcMap.put("function", fcMap);
            }
            result.add(tcMap);
        }

        return result;
    }

    private List<Map<String, Object>> convertTools(List<UnifiedChatRequest.ToolDefinition> tools) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (UnifiedChatRequest.ToolDefinition td : tools) {
            Map<String, Object> tdMap = new LinkedHashMap<>();
            tdMap.put("type", td.getType() != null ? td.getType() : "function");

            if (td.getFunction() != null) {
                Map<String, Object> fdMap = new LinkedHashMap<>();
                fdMap.put("name", td.getFunction().getName());
                fdMap.put("description", td.getFunction().getDescription());
                fdMap.put("parameters", td.getFunction().getParameters());
                tdMap.put("function", fdMap);
            }
            result.add(tdMap);
        }

        return result;
    }

    private List<UnifiedChatResponse.ChatChoice> parseChoices(List<Map<String, Object>> choices) {
        List<UnifiedChatResponse.ChatChoice> result = new ArrayList<>();

        for (Map<String, Object> choice : choices) {
            UnifiedChatResponse.ChatChoice uc = new UnifiedChatResponse.ChatChoice();
            uc.setIndex(getInt(choice, "index"));
            uc.setFinishReason((String) choice.get("finish_reason"));

            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message != null) {
                uc.setMessage(parseMessage(message));
            }

            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
            if (delta != null) {
                uc.setDelta(parseMessage(delta));
            }

            result.add(uc);
        }

        return result;
    }

    private UnifiedChatResponse.ChatMessage parseMessage(Map<String, Object> message) {
        UnifiedChatResponse.ChatMessage msg = new UnifiedChatResponse.ChatMessage();
        msg.setRole((String) message.get("role"));
        msg.setContent((String) message.get("content"));

        if (message.get("tool_calls") != null) {
            msg.setToolCalls(parseResponseToolCalls(message.get("tool_calls")));
        }

        return msg;
    }

    private List<UnifiedChatResponse.ToolCall> parseResponseToolCalls(Object toolCallsObj) {
        List<Map<String, Object>> toolCallsList = (List<Map<String, Object>>) toolCallsObj;
        List<UnifiedChatResponse.ToolCall> result = new ArrayList<>();

        for (Map<String, Object> tc : toolCallsList) {
            UnifiedChatResponse.ToolCall toolCall = new UnifiedChatResponse.ToolCall();
            toolCall.setId((String) tc.get("id"));
            toolCall.setType((String) tc.get("type"));

            Map<String, Object> function = (Map<String, Object>) tc.get("function");
            if (function != null) {
                UnifiedChatResponse.FunctionCall fc = new UnifiedChatResponse.FunctionCall();
                fc.setName((String) function.get("name"));
                fc.setArguments((String) function.get("arguments"));
                toolCall.setFunction(fc);
            }
            result.add(toolCall);
        }

        return result;
    }

    private List<Map<String, Object>> convertChoices(List<UnifiedChatResponse.ChatChoice> choices) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (UnifiedChatResponse.ChatChoice choice : choices) {
            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("index", choice.getIndex());
            cMap.put("finish_reason", choice.getFinishReason());

            if (choice.getMessage() != null) {
                cMap.put("message", convertMessage(choice.getMessage()));
            }
            if (choice.getDelta() != null) {
                cMap.put("delta", convertMessage(choice.getDelta()));
            }

            result.add(cMap);
        }

        return result;
    }

    private Map<String, Object> convertMessage(UnifiedChatResponse.ChatMessage message) {
        Map<String, Object> msgMap = new LinkedHashMap<>();
        msgMap.put("role", message.getRole());
        msgMap.put("content", message.getContent());

        if (message.getToolCalls() != null) {
            msgMap.put("tool_calls", convertResponseToolCalls(message.getToolCalls()));
        }

        return msgMap;
    }

    private List<Map<String, Object>> convertResponseToolCalls(List<UnifiedChatResponse.ToolCall> toolCalls) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (UnifiedChatResponse.ToolCall tc : toolCalls) {
            Map<String, Object> tcMap = new LinkedHashMap<>();
            tcMap.put("id", tc.getId());
            tcMap.put("type", tc.getType() != null ? tc.getType() : "function");

            if (tc.getFunction() != null) {
                Map<String, Object> fcMap = new LinkedHashMap<>();
                fcMap.put("name", tc.getFunction().getName());
                fcMap.put("arguments", tc.getFunction().getArguments());
                tcMap.put("function", fcMap);
            }
            result.add(tcMap);
        }

        return result;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).doubleValue();
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