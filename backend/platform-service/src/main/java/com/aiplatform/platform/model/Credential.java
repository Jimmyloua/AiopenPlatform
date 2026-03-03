package com.aiplatform.platform.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Credential Entity - AK/SK Management
 * 用于会议、助手和CUI后端的访问凭证管理
 */
@Data
@TableName("credentials")
public class Credential {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 凭证名称
     */
    private String name;

    /**
     * 凭证类型: meeting(会议), assistant(助手), cui_backend(CUI后端)
     */
    private String type;

    /**
     * Access Key (AK) - 公开的访问标识
     */
    private String accessKey;

    /**
     * Secret Key (SK) - 加密存储的密钥
     */
    private String secretKeyHash;

    /**
     * SK加密密钥ID
     */
    private String encryptionKeyId;

    /**
     * 关联的资源ID (会议ID/助手ID/CUI后端ID)
     */
    private Long resourceId;

    /**
     * 权限范围 (JSON格式)
     */
    private String permissions;

    /**
     * 速率限制 (请求/分钟)
     */
    private Integer rateLimit;

    /**
     * IP白名单 (JSON数组)
     */
    private String ipWhitelist;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 使用次数
     */
    private Long usageCount;

    /**
     * 描述
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 凭证类型常量
     */
    public static final String TYPE_MEETING = "meeting";
    public static final String TYPE_ASSISTANT = "assistant";
    public static final String TYPE_CUI_BACKEND = "cui_backend";
}