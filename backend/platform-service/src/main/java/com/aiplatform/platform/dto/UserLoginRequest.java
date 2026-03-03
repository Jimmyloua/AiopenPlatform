package com.aiplatform.platform.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * User Login Request DTO
 */
@Data
public class UserLoginRequest {

    @NotBlank(message = "Username or email is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

}