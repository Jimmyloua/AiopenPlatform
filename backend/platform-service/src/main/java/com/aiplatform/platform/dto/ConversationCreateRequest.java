package com.aiplatform.platform.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * Create Conversation Request DTO
 */
@Data
public class ConversationCreateRequest {

    @NotNull(message = "Assistant ID is required")
    private Long assistantId;

    private String title;

}