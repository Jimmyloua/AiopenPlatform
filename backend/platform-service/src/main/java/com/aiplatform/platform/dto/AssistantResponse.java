package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Assistant Response DTO
 */
@Data
public class AssistantResponse {

    private Long id;
    private String name;
    private String description;
    private String avatar;
    private String systemPrompt;
    private String modelConfig;
    private List<String> capabilities;
    private List<SkillResponse> skills;
    private Boolean isPublic;
    private String status;
    private String reviewComment;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}