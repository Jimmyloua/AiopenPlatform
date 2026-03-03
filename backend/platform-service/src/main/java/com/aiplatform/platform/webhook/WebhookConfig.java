package com.aiplatform.platform.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Webhook Configuration
 * Configuration for external webhook endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfig {

    /**
     * Webhook ID
     */
    private Long id;

    /**
     * User/tenant ID
     */
    private Long userId;

    /**
     * Webhook name
     */
    private String name;

    /**
     * Target URL
     */
    private String url;

    /**
     * Secret for signature verification
     */
    private String secret;

    /**
     * Event types to subscribe to
     */
    private java.util.Set<WebhookEvent.EventType> events;

    /**
     * Whether webhook is active
     */
    private boolean active;

    /**
     * Authentication type (none, bearer, basic, hmac)
     */
    private AuthType authType;

    /**
     * Authentication credentials
     */
    private String authCredentials;

    /**
     * Headers to include in webhook requests
     */
    private java.util.Map<String, String> headers;

    /**
     * Retry configuration
     */
    private RetryConfig retryConfig;

    public enum AuthType {
        NONE,
        BEARER,
        BASIC,
        HMAC_SHA256
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfig {
        private int maxRetries;
        private long initialDelayMs;
        private long maxDelayMs;
        private double backoffMultiplier;
    }
}