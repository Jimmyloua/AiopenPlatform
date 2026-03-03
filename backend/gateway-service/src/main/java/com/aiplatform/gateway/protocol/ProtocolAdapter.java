package com.aiplatform.gateway.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Protocol Adapter Interface
 * Defines contract for converting between different agent protocols
 */
@Slf4j
@Component
public abstract class ProtocolAdapter {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get the provider name this adapter handles
     */
    public abstract String getProvider();

    /**
     * Convert incoming request to unified format
     */
    public abstract UnifiedChatRequest toUnified(String requestBody);

    /**
     * Convert unified request to provider-specific format
     */
    public abstract String fromUnified(UnifiedChatRequest request);

    /**
     * Convert provider-specific response to unified format
     */
    public abstract UnifiedChatResponse toUnifiedResponse(String responseBody);

    /**
     * Convert unified response to provider-specific format
     */
    public abstract String fromUnifiedResponse(UnifiedChatResponse response);

    /**
     * Parse streaming chunk from provider
     */
    public abstract List<UnifiedChatResponse> parseStreamChunk(String chunk);

    /**
     * Check if this adapter supports the given model
     */
    public abstract boolean supportsModel(String model);
}