package com.aiplatform.platform.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Credential Entity
 */
@Data
@TableName("credentials")
public class Credential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String provider;

    private String encryptedValue;

    private String encryptionKeyId;

    private String metadata;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

}