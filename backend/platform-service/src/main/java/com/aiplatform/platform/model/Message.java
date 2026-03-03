package com.aiplatform.platform.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Message Entity
 */
@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String toolCalls;

    private String toolCallId;

    private String thinkingContent;

    private Integer tokenCount;

    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}