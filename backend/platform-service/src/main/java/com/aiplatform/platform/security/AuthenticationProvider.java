package com.aiplatform.platform.security;

import java.util.Set;

/**
 * Authentication Provider Interface
 * 统一认证提供者接口，支持多种认证方式
 */
public interface AuthenticationProvider {

    /**
     * 获取认证类型
     */
    String getType();

    /**
     * 验证凭证
     * @param credential 凭证字符串（JWT token 或 AK）
     * @param request 请求信息
     * @return 认证结果
     */
    AuthenticationResult authenticate(String credential, AuthRequest request);

    /**
     * 检查是否支持该认证方式
     * @param authHeader Authorization header 值
     * @return 是否支持
     */
    boolean supports(String authHeader);

    /**
     * 认证结果
     */
    record AuthenticationResult(
        boolean success,
        Long userId,
        String username,
        String role,
        String credentialType,
        String accessKey,
        Set<String> permissions,
        String errorMessage
    ) {
        public static AuthenticationResult success(Long userId, String username, String role, String credentialType) {
            return new AuthenticationResult(true, userId, username, role, credentialType, null, null, null);
        }

        public static AuthenticationResult successWithAkSk(Long userId, String accessKey, Set<String> permissions) {
            return new AuthenticationResult(true, userId, null, "service", "aksk", accessKey, permissions, null);
        }

        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, null, null, null, null, null, null, errorMessage);
        }
    }

    /**
     * 认证请求信息
     */
    record AuthRequest(
        String method,
        String path,
        String timestamp,
        String nonce,
        String signature,
        String body,
        String clientIp
    ) {}
}