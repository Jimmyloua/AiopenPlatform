package com.aiplatform.gateway.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Gemini Protocol Adapter
 * Handles Google Gemini models (gemini-pro, gemini-1.5-pro, etc.)
 *
 * Gemini API format differences:
 * - Uses "contents" instead of "messages"
 * - Different role naming ("user" and "model")
 * - Uses "parts" array for content
 * - Different safety settings format
 */
@Slf4j
@Component
public class GeminiAdapter extends ProtocolAdapter {

    private static final String PROVIDER = "gemini";
    private static final Set<String> SUPPORTED_MODELS = Set.of(
        "gemini-pro", "gemini-1.5-pro", "gemini-1.5-flash",
        "gemini-2.0-flash", "gemini-2.0-pro"
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

            // Extract model from request or use default
            String model = (String) request.get("model");
            unified.setModel(model != null ? model : "gemini-pro");

            // Parse generation config
            Map<String, Object> generationConfig = (Map<String, Object>) request.get("generationConfig");
            if (generationConfig != null) {
                if (generationConfig.get("temperature") != null) {
                    unified.setTemperature(((Number) generationConfig.get("temperature")).doubleValue());
                }
                if (generationConfig.get("topP") != null) {
                    unified.setTopP(((Number) generationConfig.get("topP")).doubleValue());
                }
                if (generationConfig.get("maxOutputTokens") != null) {
                    unified.setMaxTokens(((Number) generationConfig.get("maxOutputTokens")).intValue());
                }
            }

            // Parse system instruction
            Map<String, Object> systemInstruction = (Map<String, Object>) request.get("systemInstruction");
            String systemPrompt = null;
            if (systemInstruction != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) systemInstruction.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    systemPrompt = (String) parts.get(0).get("text");
                }
            }

            // Parse contents (Gemini's message format)
            List<Map<String, Object>> contents = (List<Map<String, Object>>) request.get("contents");
            if (contents != null) {
                List<UnifiedChatRequest.ChatMessage> unifiedMessages = new ArrayList<>();

                // Add system prompt if present
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    UnifiedChatRequest.ChatMessage systemMsg = new UnifiedChatRequest.ChatMessage();
                    systemMsg.setRole("system");
                    systemMsg.setContent(systemPrompt);
                    unifiedMessages.add(systemMsg);
                }

                for (Map<String, Object> content : contents) {
                    String role = (String) content.get("role");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                    if (parts != null) {
                        StringBuilder textBuilder = new StringBuilder();
                        for (Map<String, Object> part : parts) {
                            if (part.get("text") != null) {
                                textBuilder.append(part.get("text"));
                            }
                        }

                        UnifiedChatRequest.ChatMessage msg = new UnifiedChatRequest.ChatMessage();
                        msg.setRole(convertFromGeminiRole(role));
                        msg.setContent(textBuilder.toString());
                        unifiedMessages.add(msg);
                    }
                }
                unified.setMessages(unifiedMessages);
            }

            // Parse tools
            List<Map<String, Object>> tools = (List<Map<String, Object>>) request.get("tools");
            if (tools != null) {
                unified.setTools(parseGeminiTools(tools));
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse Gemini request", e);
            throw new RuntimeException("Invalid Gemini request format", e);
        }
    }

    @Override
    public String fromUnified(UnifiedChatRequest request) {
        try {
            Map<String, Object> geminiRequest = new LinkedHashMap<>();

            // Build contents from messages
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> systemInstruction = null;

            if (request.getMessages() != null) {
                for (UnifiedChatRequest.ChatMessage msg : request.getMessages()) {
                    if ("system".equals(msg.getRole())) {
                        // System instruction goes separately
                        Map<String, Object> part = new LinkedHashMap<>();
                        part.put("text", msg.getContent());

                        systemInstruction = new LinkedHashMap<>();
                        systemInstruction.put("parts", List.of(part));
                    } else {
                        Map<String, Object> content = new LinkedHashMap<>();
                        content.put("role", convertToGeminiRole(msg.getRole()));

                        Map<String, Object> part = new LinkedHashMap<>();
                        part.put("text", msg.getContent());
                        content.put("parts", List.of(part));

                        contents.add(content);
                    }
                }
            }

            geminiRequest.put("contents", contents);

            if (systemInstruction != null) {
                geminiRequest.put("systemInstruction", systemInstruction);
            }

            // Build generation config
            Map<String, Object> generationConfig = new LinkedHashMap<>();
            if (request.getTemperature() != null) {
                generationConfig.put("temperature", request.getTemperature());
            }
            if (request.getTopP() != null) {
                generationConfig.put("topP", request.getTopP());
            }
            if (request.getMaxTokens() != null) {
                generationConfig.put("maxOutputTokens", request.getMaxTokens());
            }
            if (!generationConfig.isEmpty()) {
                geminiRequest.put("generationConfig", generationConfig);
            }

            // Convert tools
            if (request.getTools() != null) {
                geminiRequest.put("tools", convertToGeminiTools(request.getTools()));
            }

            return objectMapper.writeValueAsString(geminiRequest);
        } catch (Exception e) {
            log.error("Failed to convert to Gemini format", e);
            throw new RuntimeException("Failed to convert request", e);
        }
    }

    @Override
    public UnifiedChatResponse toUnifiedResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {});

            UnifiedChatResponse unified = new UnifiedChatResponse();

            // Gemini doesn't always return an id
            unified.setId(UUID.randomUUID().toString());
            unified.setObject("chat.completion");
            unified.setCreated(System.currentTimeMillis() / 1000);

            // Extract model version if present
            Map<String, Object> modelVersion = (Map<String, Object>) response.get("modelVersion");
            if (modelVersion != null) {
                unified.setModel((String) modelVersion.get("name"));
            }

            // Parse candidates
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                List<UnifiedChatResponse.ChatChoice> choices = new ArrayList<>();

                for (int i = 0; i < candidates.size(); i++) {
                    Map<String, Object> candidate = candidates.get(i);
                    UnifiedChatResponse.ChatChoice choice = new UnifiedChatResponse.ChatChoice();
                    choice.setIndex(i);

                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    if (content != null) {
                        UnifiedChatResponse.ChatMessage message = new UnifiedChatResponse.ChatMessage();
                        message.setRole("assistant");

                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null) {
                            StringBuilder textBuilder = new StringBuilder();
                            for (Map<String, Object> part : parts) {
                                if (part.get("text") != null) {
                                    textBuilder.append(part.get("text"));
                                }
                            }
                            message.setContent(textBuilder.toString());
                        }

                        choice.setMessage(message);
                    }

                    // Parse finish reason
                    String finishReason = (String) candidate.get("finishReason");
                    choice.setFinishReason(convertFinishReason(finishReason));

                    choices.add(choice);
                }

                unified.setChoices(choices);
            }

            // Parse usage metadata
            Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
            if (usageMetadata != null) {
                UnifiedChatResponse.Usage usage = new UnifiedChatResponse.Usage();
                usage.setPromptTokens(getInt(usageMetadata, "promptTokenCount"));
                usage.setCompletionTokens(getInt(usageMetadata, "candidatesTokenCount"));
                usage.setTotalTokens(getInt(usageMetadata, "totalTokenCount"));
                unified.setUsage(usage);
            }

            return unified;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            throw new RuntimeException("Invalid Gemini response format", e);
        }
    }

    @Override
    public String fromUnifiedResponse(UnifiedChatResponse response) {
        try {
            Map<String, Object> geminiResponse = new LinkedHashMap<>();

            // Build candidates
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                List<Map<String, Object>> candidates = new ArrayList<>();

                for (UnifiedChatResponse.ChatChoice choice : response.getChoices()) {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("index", choice.getIndex());

                    if (choice.getMessage() != null) {
                        Map<String, Object> content = new LinkedHashMap<>();
                        content.put("role", "model");

                        Map<String, Object> part = new LinkedHashMap<>();
                        part.put("text", choice.getMessage().getContent());
                        content.put("parts", List.of(part));

                        candidate.put("content", content);
                    }

                    candidate.put("finishReason", convertToGeminiFinishReason(choice.getFinishReason()));

                    candidates.add(candidate);
                }

                geminiResponse.put("candidates", candidates);
            }

            // Build usage metadata
            if (response.getUsage() != null) {
                Map<String, Object> usageMetadata = new LinkedHashMap<>();
                usageMetadata.put("promptTokenCount", response.getUsage().getPromptTokens());
                usageMetadata.put("candidatesTokenCount", response.getUsage().getCompletionTokens());
                usageMetadata.put("totalTokenCount", response.getUsage().getTotalTokens());
                geminiResponse.put("usageMetadata", usageMetadata);
            }

            return objectMapper.writeValueAsString(geminiResponse);
        } catch (Exception e) {
            log.error("Failed to convert to Gemini response format", e);
            throw new RuntimeException("Failed to convert response", e);
        }
    }

    @Override
    public List<UnifiedChatResponse> parseStreamChunk(String chunk) {
        List<UnifiedChatResponse> responses = new ArrayList<>();

        try {
            // Gemini streaming uses different format
            Map<String, Object> response = objectMapper.readValue(chunk,
                new TypeReference<Map<String, Object>>() {});

            UnifiedChatResponse unified = toUnifiedResponse(objectMapper.writeValueAsString(response));
            unified.setObject("chat.completion.chunk");
            responses.add(unified);
        } catch (Exception e) {
            log.warn("Failed to parse Gemini stream chunk: {}", chunk);
        }

        return responses;
    }

    @Override
    public boolean supportsModel(String model) {
        if (model == null) return false;
        return SUPPORTED_MODELS.stream().anyMatch(model::startsWith);
    }

    // Helper methods
    private String convertFromGeminiRole(String role) {
        if ("model".equals(role)) return "assistant";
        if ("user".equals(role)) return "user";
        return role;
    }

    private String convertToGeminiRole(String role) {
        if ("assistant".equals(role)) return "model";
        return "user";
    }

    private String convertFinishReason(String reason) {
        if (reason == null) return null;
        switch (reason) {
            case "STOP":
                return "stop";
            case "MAX_TOKENS":
                return "length";
            case "SAFETY":
                return "content_filter";
            case "RECITATION":
                return "content_filter";
            default:
                return reason.toLowerCase();
        }
    }

    private String convertToGeminiFinishReason(String reason) {
        if (reason == null) return "STOP";
        switch (reason) {
            case "stop":
                return "STOP";
            case "length":
                return "MAX_TOKENS";
            case "content_filter":
                return "SAFETY";
            default:
                return "STOP";
        }
    }

    private List<UnifiedChatRequest.ToolDefinition> parseGeminiTools(List<Map<String, Object>> tools) {
        List<UnifiedChatRequest.ToolDefinition> result = new ArrayList<>();

        for (Map<String, Object> tool : tools) {
            List<Map<String, Object>> functionDeclarations =
                (List<Map<String, Object>>) tool.get("functionDeclarations");

            if (functionDeclarations != null) {
                for (Map<String, Object> func : functionDeclarations) {
                    UnifiedChatRequest.ToolDefinition td = new UnifiedChatRequest.ToolDefinition();
                    td.setType("function");

                    UnifiedChatRequest.FunctionDef fd = new UnifiedChatRequest.FunctionDef();
                    fd.setName((String) func.get("name"));
                    fd.setDescription((String) func.get("description"));
                    fd.setParameters((Map<String, Object>) func.get("parameters"));
                    td.setFunction(fd);

                    result.add(td);
                }
            }
        }

        return result;
    }

    private List<Map<String, Object>> convertToGeminiTools(List<UnifiedChatRequest.ToolDefinition> tools) {
        List<Map<String, Object>> functionDeclarations = new ArrayList<>();

        for (UnifiedChatRequest.ToolDefinition td : tools) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", td.getFunction().getName());
            func.put("description", td.getFunction().getDescription());
            func.put("parameters", td.getFunction().getParameters());
            functionDeclarations.add(func);
        }

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("functionDeclarations", functionDeclarations);

        return List.of(tool);
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }
}