package com.aiplatform.platform.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Create Assistant Request DTO
 */
@Data
public class AssistantCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String avatar;

    private String systemPrompt;

    private String modelConfig;

    private List<String> capabilities;

    private List<Long> skillIds;

}