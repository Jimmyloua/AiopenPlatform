package com.aiplatform.gateway.protocol;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Protocol Adapter Manager
 * Manages all protocol adapters and routes requests to the appropriate adapter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolAdapterManager {

    private final List<ProtocolAdapter> adapters;
    private final OpenAIAdapter openAIAdapter;
    private final Map<String, ProtocolAdapter> adapterMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register all adapters by provider name
        for (ProtocolAdapter adapter : adapters) {
            adapterMap.put(adapter.getProvider().toLowerCase(), adapter);
            log.info("Registered protocol adapter: {}", adapter.getProvider());
        }
    }

    /**
     * Get adapter by provider name
     */
    public ProtocolAdapter getAdapter(String provider) {
        if (provider == null) {
            return openAIAdapter; // Default to OpenAI
        }
        return adapterMap.get(provider.toLowerCase());
    }

    /**
     * Get adapter by model name
     * Auto-detects the provider based on model name
     */
    public ProtocolAdapter getAdapterByModel(String model) {
        if (model == null) {
            return openAIAdapter;
        }

        String lowerModel = model.toLowerCase();

        // Check each adapter if it supports this model
        for (ProtocolAdapter adapter : adapters) {
            if (adapter.supportsModel(model)) {
                log.debug("Using {} adapter for model: {}", adapter.getProvider(), model);
                return adapter;
            }
        }

        // Default to OpenAI for unknown models
        log.debug("Using default OpenAI adapter for model: {}", model);
        return openAIAdapter;
    }

    /**
     * Get all available adapters
     */
    public List<ProtocolAdapter> getAllAdapters() {
        return new ArrayList<>(adapters);
    }

    /**
     * Get all supported models across all adapters
     */
    public List<String> getAllSupportedModels() {
        List<String> models = new ArrayList<>();

        // Add common model prefixes
        models.addAll(List.of(
            "gpt-4", "gpt-4o", "gpt-3.5-turbo",
            "claude-3-opus", "claude-3-sonnet", "claude-3-haiku",
            "claude-3-5-sonnet", "claude-3-5-haiku",
            "gemini-pro", "gemini-1.5-pro", "gemini-1.5-flash"
        ));

        return models;
    }

    /**
     * Detect provider from model name
     */
    public String detectProvider(String model) {
        if (model == null) return "openai";

        String lowerModel = model.toLowerCase();

        if (lowerModel.contains("gpt") || lowerModel.contains("o1") || lowerModel.contains("o3")) {
            return "openai";
        }
        if (lowerModel.contains("claude")) {
            return "claude";
        }
        if (lowerModel.contains("gemini")) {
            return "gemini";
        }

        return "openai";
    }
}