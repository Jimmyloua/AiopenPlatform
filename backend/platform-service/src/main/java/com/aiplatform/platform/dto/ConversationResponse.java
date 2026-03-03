package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation Response DTO
 */
@Data
public class ConversationResponse {

    private Long id;
    private Long userId;
    private Long assistantId;
    private String title;
    private String metadata;
    private AssistantResponse assistant;
    private List<MessageResponse> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}