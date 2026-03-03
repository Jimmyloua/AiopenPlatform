package com.aiplatform.platform.security;

import com.aiplatform.platform.service.JwtService;
import com.aiplatform.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * JWT Authentication Provider
 * JWT认证提供者，用于用户登录认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public String getType() {
        return "jwt";
    }

    @Override
    public AuthenticationResult authenticate(String credential, AuthRequest request) {
        try {
            if (!jwtService.validateToken(credential)) {
                return AuthenticationResult.failure("Invalid JWT token");
            }

            String username = jwtService.extractUsername(credential);
            Long userId = jwtService.extractUserId(credential);

            if (username == null || userId == null) {
                return AuthenticationResult.failure("Invalid token claims");
            }

            var user = userService.getUserByUsername(username);
            if (user == null) {
                return AuthenticationResult.failure("User not found");
            }

            log.debug("JWT authentication successful for user: {}", username);

            return AuthenticationResult.success(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                getType()
            );

        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            return AuthenticationResult.failure("JWT authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }
}