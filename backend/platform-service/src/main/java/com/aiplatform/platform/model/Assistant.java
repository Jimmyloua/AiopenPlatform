package com.aiplatform.platform.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Assistant Entity
 */
@Data
@TableName("assistants")
public class Assistant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String avatar;

    private String systemPrompt;

    private String modelConfig;

    private String capabilities;

    private Boolean isPublic;

    private String status;

    private String reviewComment;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

}