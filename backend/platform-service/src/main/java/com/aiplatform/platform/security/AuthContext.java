package com.aiplatform.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * Authentication Context
 * 认证上下文工具类，用于获取当前认证信息
 */
@Component
public class AuthContext {

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId != null ? (Long) userId : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username != null ? (String) username : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getCurrentRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? (String) role : null;
    }

    /**
     * 获取认证类型
     */
    public static String getCredentialType(HttpServletRequest request) {
        Object type = request.getAttribute("credentialType");
        return type != null ? (String) type : null;
    }

    /**
     * 获取Access Key
     */
    public static String getAccessKey(HttpServletRequest request) {
        Object ak = request.getAttribute("accessKey");
        return ak != null ? (String) ak : null;
    }

    /**
     * 获取当前用户权限
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getCurrentPermissions(HttpServletRequest request) {
        Object permissions = request.getAttribute("permissions");
        return permissions != null ? (Set<String>) permissions : Set.of();
    }

    /**
     * 检查是否有指定权限
     */
    public static boolean hasPermission(HttpServletRequest request, String permission) {
        Set<String> permissions = getCurrentPermissions(request);
        return permissions.contains(permission) || permissions.contains("*");
    }

    /**
     * 检查是否有任一指定权限
     */
    public static boolean hasAnyPermission(HttpServletRequest request, String... permissions) {
        Set<String> currentPermissions = getCurrentPermissions(request);
        for (String permission : permissions) {
            if (currentPermissions.contains(permission) || currentPermissions.contains("*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为AK/SK认证
     */
    public static boolean isAkSkAuth(HttpServletRequest request) {
        return "aksk".equals(getCredentialType(request));
    }

    /**
     * 检查是否为JWT认证
     */
    public static boolean isJwtAuth(HttpServletRequest request) {
        return "jwt".equals(getCredentialType(request));
    }

    /**
     * 检查是否已认证
     */
    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUserId(request) != null;
    }

    /**
     * 获取认证错误信息
     */
    public static String getAuthError(HttpServletRequest request) {
        Object error = request.getAttribute("authError");
        return error != null ? (String) error : null;
    }

    /**
     * 要求用户已认证，否则抛出异常
     */
    public static Long requireUserId(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            throw new SecurityException("Authentication required");
        }
        return userId;
    }

    /**
     * 要求有指定权限，否则抛出异常
     */
    public static void requirePermission(HttpServletRequest request, String permission) {
        if (!hasPermission(request, permission)) {
            throw new SecurityException("Permission denied: " + permission);
        }
    }
}