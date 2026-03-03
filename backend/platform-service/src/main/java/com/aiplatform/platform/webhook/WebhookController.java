package com.aiplatform.platform.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Webhook Controller
 * REST endpoints for webhook management
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Register a new webhook
     */
    @PostMapping
    public ResponseEntity<WebhookConfig> registerWebhook(
            @RequestBody WebhookConfig config,
            @RequestHeader("X-User-Id") Long userId) {

        config.setUserId(userId);
        WebhookConfig registered = webhookService.registerWebhook(config);

        log.info("Webhook registered: id={}, userId={}", registered.getId(), userId);

        return ResponseEntity.ok(registered);
    }

    /**
     * List user's webhooks
     */
    @GetMapping
    public ResponseEntity<List<WebhookConfig>> listWebhooks(
            @RequestHeader("X-User-Id") Long userId) {

        List<WebhookConfig> webhooks = webhookService.getUserWebhooks(userId);
        return ResponseEntity.ok(webhooks);
    }

    /**
     * Unregister a webhook
     */
    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Void> unregisterWebhook(
            @PathVariable Long webhookId,
            @RequestHeader("X-User-Id") Long userId) {

        webhookService.unregisterWebhook(webhookId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Test webhook endpoint
     */
    @PostMapping("/{webhookId}/test")
    public ResponseEntity<Map<String, Object>> testWebhook(
            @PathVariable Long webhookId,
            @RequestHeader("X-User-Id") Long userId) {

        WebhookEvent testEvent = WebhookEvent.builder()
            .eventId("test-" + System.currentTimeMillis())
            .eventType(WebhookEvent.EventType.MESSAGE_CREATED)
            .timestamp(System.currentTimeMillis())
            .payload(Map.of(
                "test", true,
                "message", "This is a test webhook event"
            ))
            .source("platform-service")
            .build();

        webhookService.sendWebhook(testEvent);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Test webhook sent",
            "eventId", testEvent.getEventId()
        ));
    }

    /**
     * Handle incoming webhook (for external services to call)
     */
    @PostMapping("/receive")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestBody WebhookEvent event,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {

        log.info("Received webhook: type={}, eventId={}", event.getEventType(), event.getEventId());

        // TODO: Verify signature if provided

        // Process the webhook event
        // This would typically publish to an event bus or trigger actions

        return ResponseEntity.ok(Map.of(
            "success", true,
            "eventId", event.getEventId()
        ));
    }
}