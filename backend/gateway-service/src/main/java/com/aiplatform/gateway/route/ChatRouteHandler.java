package com.aiplatform.gateway.route;

import com.aiplatform.gateway.handler.ProtocolHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Chat Route Handler
 * Handles OpenAI-compatible chat completion endpoints
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChatRouteHandler {

    private final ProtocolHandler protocolHandler;
    private final ObjectMapper objectMapper;

    // Connection service URL (should be configurable)
    private static final String CONNECTION_SERVICE_URL = "http://localhost:8081";

    /**
     * Chat completions endpoint (OpenAI compatible)
     */
    @PostMapping("/chat/completions")
    public Mono<?> chatCompletions(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.debug("Received chat completion request");

        try {
            Map<String, Object> request = objectMapper.readValue(requestBody, Map.class);
            Boolean stream = (Boolean) request.get("stream");

            if (stream != null && stream) {
                // Return streaming response as SSE
                return Mono.just(objectMapper.writeValueAsString(Map.of(
                    "type", "stream",
                    "message", "Use /v1/chat/completions/stream for streaming"
                )));
            } else {
                // Return regular response
                return protocolHandler.handleChatRequest(requestBody, CONNECTION_SERVICE_URL);
            }
        } catch (Exception e) {
            log.error("Failed to process chat completion request", e);
            return Mono.just(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Streaming chat completions endpoint
     */
    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChatCompletions(
            @RequestBody String requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.debug("Received streaming chat completion request");

        return protocolHandler.handleStreamRequest(requestBody, CONNECTION_SERVICE_URL)
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build())
            .concatWith(Flux.just(ServerSentEvent.<String>builder()
                .data("[DONE]")
                .build()))
            .onErrorResume(e -> {
                log.error("Streaming error", e);
                return Flux.just(ServerSentEvent.<String>builder()
                    .data(createErrorResponse(e.getMessage()))
                    .build());
            });
    }

    /**
     * List models endpoint
     */
    @GetMapping("/models")
    public Mono<String> listModels(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.debug("Received list models request");
        return protocolHandler.listModels();
    }

    /**
     * Retrieve model endpoint
     */
    @GetMapping("/models/{model}")
    public Mono<String> retrieveModel(
            @PathVariable String model,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.debug("Received retrieve model request for: {}", model);

        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", model);
            response.put("object", "model");
            response.put("created", 1700000000L);
            response.put("owned_by", "openai");

            return Mono.just(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            return Mono.just(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Fallback endpoint for circuit breaker
     */
    @GetMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just(createErrorResponse("Service temporarily unavailable. Please try again later."));
    }

    private String createErrorResponse(String message) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", Map.of(
                "message", message,
                "type", "api_error",
                "code", "service_unavailable"
            ));
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\":{\"message\":\"" + message + "\"}}";
        }
    }
}