package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Skill Response DTO
 */
@Data
public class SkillResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String schemaJson;
    private String handlerConfig;
    private Boolean isPublic;
    private LocalDateTime createdAt;

}