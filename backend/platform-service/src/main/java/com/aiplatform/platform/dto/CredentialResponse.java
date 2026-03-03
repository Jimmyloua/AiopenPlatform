package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 凭证响应
 */
@Data
public class CredentialResponse {

    private Long id;

    private String name;

    private String type;

    private String accessKey;

    private String secretKey; // 仅在创建时返回

    private Long resourceId;

    private String permissions;

    private Integer rateLimit;

    private String ipWhitelist;

    private Boolean enabled;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private Long usageCount;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}