package com.aiplatform.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * User Response DTO
 */
@Data
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private String status;
    private LocalDateTime createdAt;

}