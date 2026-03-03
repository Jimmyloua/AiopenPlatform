package com.aiplatform.platform.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Connection Entity
 */
@Data
@TableName("connections")
public class Connection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String provider;

    private String endpoint;

    private Long credentialId;

    private String config;

    private String healthStatus;

    private LocalDateTime lastCheckAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

}