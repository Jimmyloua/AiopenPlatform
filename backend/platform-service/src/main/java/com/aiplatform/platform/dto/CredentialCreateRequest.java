package com.aiplatform.platform.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建凭证请求
 */
@Data
public class CredentialCreateRequest {

    @NotBlank(message = "凭证名称不能为空")
    private String name;

    @NotBlank(message = "凭证类型不能为空")
    private String type;

    private Long resourceId;

    private String permissions;

    private Integer rateLimit;

    private String ipWhitelist;

    private String description;

    private Integer expiresInDays;
}