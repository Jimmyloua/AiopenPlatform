package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Message Response DTO
 */
@Data
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String toolCalls;
    private String toolCallId;
    private String thinkingContent;
    private Integer tokenCount;
    private LocalDateTime createdAt;

}