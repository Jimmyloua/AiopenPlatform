package com.aiplatform.gateway.handler;

import com.aiplatform.gateway.protocol.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Protocol Handler
 * Handles the actual protocol conversion and request forwarding
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolHandler {

    private final ProtocolAdapterManager adapterManager;
    private final ObjectMapper objectMapper;

    /**
     * Process incoming chat request and forward to appropriate backend
     */
    public Mono<String> handleChatRequest(String requestBody, String targetUrl) {
        try {
            // Parse to unified format
            Map<String, Object> request = objectMapper.readValue(requestBody,
                new TypeReference<Map<String, Object>>() {});

            String model = (String) request.get("model");
            ProtocolAdapter adapter = adapterManager.getAdapterByModel(model);

            log.info("Processing chat request for model: {} with adapter: {}", model, adapter.getProvider());

            // Convert to unified format
            UnifiedChatRequest unifiedRequest = adapter.toUnified(requestBody);

            // Convert to target provider format
            String providerRequest = adapter.fromUnified(unifiedRequest);

            // Forward to backend service
            return forwardRequest(providerRequest, targetUrl, adapter);

        } catch (Exception e) {
            log.error("Failed to process chat request", e);
            return Mono.error(new RuntimeException("Failed to process request: " + e.getMessage()));
        }
    }

    /**
     * Handle streaming chat request
     */
    public Flux<String> handleStreamRequest(String requestBody, String targetUrl) {
        try {
            // Parse to unified format
            Map<String, Object> request = objectMapper.readValue(requestBody,
                new TypeReference<Map<String, Object>>() {});

            String model = (String) request.get("model");
            ProtocolAdapter adapter = adapterManager.getAdapterByModel(model);

            log.info("Processing streaming request for model: {} with adapter: {}", model, adapter.getProvider());

            // Convert to target provider format
            UnifiedChatRequest unifiedRequest = adapter.toUnified(requestBody);
            unifiedRequest.setStream(true);
            String providerRequest = adapter.fromUnified(unifiedRequest);

            // Forward and convert streaming response
            return forwardStreamRequest(providerRequest, targetUrl, adapter);

        } catch (Exception e) {
            log.error("Failed to process streaming request", e);
            return Flux.error(new RuntimeException("Failed to process request: " + e.getMessage()));
        }
    }

    /**
     * Convert response from provider to OpenAI format
     */
    public String convertResponse(String responseBody, String model) {
        try {
            ProtocolAdapter adapter = adapterManager.getAdapterByModel(model);
            UnifiedChatResponse unified = adapter.toUnifiedResponse(responseBody);

            // Convert back to OpenAI format for client compatibility
            return openAIAdapter().fromUnifiedResponse(unified);

        } catch (Exception e) {
            log.error("Failed to convert response", e);
            throw new RuntimeException("Failed to convert response: " + e.getMessage());
        }
    }

    /**
     * List available models
     */
    public Mono<String> listModels() {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("object", "list");

            List<Map<String, Object>> models = new ArrayList<>();
            for (String model : adapterManager.getAllSupportedModels()) {
                Map<String, Object> modelInfo = new LinkedHashMap<>();
                modelInfo.put("id", model);
                modelInfo.put("object", "model");
                modelInfo.put("created", 1700000000L);
                modelInfo.put("owned_by", adapterManager.detectProvider(model));
                models.add(modelInfo);
            }

            response.put("data", models);

            return Mono.just(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to list models", e);
            return Mono.error(new RuntimeException("Failed to list models"));
        }
    }

    // Private helper methods

    private Mono<String> forwardRequest(String requestBody, String targetUrl, ProtocolAdapter adapter) {
        WebClient webClient = WebClient.builder()
            .baseUrl(targetUrl)
            .build();

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> convertToOpenAIFormat(response, adapter));
    }

    private Flux<String> forwardStreamRequest(String requestBody, String targetUrl, ProtocolAdapter adapter) {
        WebClient webClient = WebClient.builder()
            .baseUrl(targetUrl)
            .build();

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .map(chunk -> convertStreamChunk(chunk, adapter));
    }

    private String convertToOpenAIFormat(String response, ProtocolAdapter adapter) {
        try {
            UnifiedChatResponse unified = adapter.toUnifiedResponse(response);
            return openAIAdapter().fromUnifiedResponse(unified);
        } catch (Exception e) {
            log.warn("Failed to convert response, returning as-is");
            return response;
        }
    }

    private String convertStreamChunk(String chunk, ProtocolAdapter adapter) {
        try {
            List<UnifiedChatResponse> responses = adapter.parseStreamChunk(chunk);
            StringBuilder result = new StringBuilder();

            for (UnifiedChatResponse response : responses) {
                String openaiChunk = openAIAdapter().fromUnifiedResponse(response);
                result.append("data: ").append(openaiChunk).append("\n\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.debug("Failed to convert stream chunk, returning as-is");
            return chunk;
        }
    }

    private OpenAIAdapter openAIAdapter() {
        return (OpenAIAdapter) adapterManager.getAdapter("openai");
    }
}