package com.aiplatform.platform.dto;

import lombok.Data;

/**
 * Authentication Response DTO
 */
@Data
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse user;

    public AuthResponse(String token, Long expiresIn, UserResponse user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

}