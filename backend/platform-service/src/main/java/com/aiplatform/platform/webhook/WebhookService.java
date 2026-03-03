package com.aiplatform.platform.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Webhook Service
 * Handles sending webhook notifications to external services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // In-memory webhook config storage (should be database in production)
    private final Map<Long, WebhookConfig> webhookConfigs = new ConcurrentHashMap<>();

    /**
     * Register a new webhook
     */
    public WebhookConfig registerWebhook(WebhookConfig config) {
        if (config.getId() == null) {
            config.setId(System.currentTimeMillis());
        }
        webhookConfigs.put(config.getId(), config);
        log.info("Registered webhook: id={}, url={}", config.getId(), config.getUrl());
        return config;
    }

    /**
     * Unregister a webhook
     */
    public void unregisterWebhook(Long webhookId) {
        webhookConfigs.remove(webhookId);
        log.info("Unregistered webhook: id={}", webhookId);
    }

    /**
     * Send webhook event
     */
    @Async
    public void sendWebhook(WebhookEvent event) {
        log.debug("Sending webhook event: type={}, eventId={}", event.getEventType(), event.getEventId());

        for (WebhookConfig config : webhookConfigs.values()) {
            if (!config.isActive()) {
                continue;
            }

            if (!config.getEvents().contains(event.getEventType())) {
                continue;
            }

            sendToWebhook(config, event);
        }
    }

    /**
     * Send webhook to specific user's webhooks
     */
    @Async
    public void sendUserWebhook(Long userId, WebhookEvent event) {
        log.debug("Sending user webhook: userId={}, eventType={}", userId, event.getEventType());

        webhookConfigs.values().stream()
            .filter(config -> config.getUserId().equals(userId))
            .filter(WebhookConfig::isActive)
            .filter(config -> config.getEvents().contains(event.getEventType()))
            .forEach(config -> sendToWebhook(config, event));
    }

    /**
     * Send event to a specific webhook endpoint
     */
    private void sendToWebhook(WebhookConfig config, WebhookEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            HttpHeaders headers = buildHeaders(config, payload);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            log.debug("Sending webhook to: {}", config.getUrl());

            ResponseEntity<String> response = restTemplate.exchange(
                config.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook delivered successfully: url={}, status={}",
                    config.getUrl(), response.getStatusCode());
            } else {
                log.warn("Webhook delivery failed: url={}, status={}",
                    config.getUrl(), response.getStatusCode());
                handleRetry(config, event);
            }

        } catch (RestClientException e) {
            log.error("Webhook delivery failed: url={}, error={}",
                config.getUrl(), e.getMessage());
            handleRetry(config, event);
        } catch (Exception e) {
            log.error("Failed to serialize webhook event", e);
        }
    }

    /**
     * Build HTTP headers for webhook request
     */
    private HttpHeaders buildHeaders(WebhookConfig config, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add custom headers
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::add);
        }

        // Add authentication
        switch (config.getAuthType()) {
            case BEARER:
                headers.setBearerAuth(config.getAuthCredentials());
                break;

            case BASIC:
                headers.set("Authorization", "Basic " + config.getAuthCredentials());
                break;

            case HMAC_SHA256:
                String signature = generateHmacSignature(config.getSecret(), payload);
                headers.set("X-Webhook-Signature", signature);
                break;

            default:
                break;
        }

        return headers;
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHmacSignature(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to generate HMAC signature", e);
            return "";
        }
    }

    /**
     * Handle retry logic for failed webhook deliveries
     */
    private void handleRetry(WebhookConfig config, WebhookEvent event) {
        WebhookConfig.RetryConfig retryConfig = config.getRetryConfig();
        if (retryConfig == null || retryConfig.getMaxRetries() <= 0) {
            return;
        }

        // TODO: Implement retry with exponential backoff
        // This would typically use a message queue or scheduler
        log.info("Webhook retry scheduled: webhookId={}, eventId={}", config.getId(), event.getEventId());
    }

    /**
     * Get all webhooks for a user
     */
    public java.util.List<WebhookConfig> getUserWebhooks(Long userId) {
        return webhookConfigs.values().stream()
            .filter(config -> config.getUserId().equals(userId))
            .toList();
    }
}