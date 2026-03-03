package com.aiplatform.platform.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * Create Skill Request DTO
 */
@Data
public class SkillCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String category;

    @NotBlank(message = "Schema is required")
    private String schemaJson;

    private String handlerConfig;

    private Boolean isPublic = true;

}