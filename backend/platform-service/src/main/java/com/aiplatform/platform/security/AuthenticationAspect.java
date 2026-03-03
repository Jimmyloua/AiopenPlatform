package com.aiplatform.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Set;

/**
 * Authentication Aspect
 * 认证切面，处理@RequireAuth和@AkSkAuth注解
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthenticationAspect {

    @Around("@annotation(requireAuth)")
    public Object checkRequireAuth(ProceedingJoinPoint joinPoint, RequireAuth requireAuth) throws Throwable {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new SecurityException("Cannot get request context");
        }

        // 检查是否允许匿名
        if (requireAuth.allowAnonymous()) {
            return joinPoint.proceed();
        }

        // 检查认证类型
        String credentialType = AuthContext.getCredentialType(request);
        if (credentialType == null) {
            throw new SecurityException("Authentication required");
        }

        // 验证认证类型是否允许
        String[] allowedTypes = requireAuth.types();
        if (allowedTypes.length > 0) {
            boolean typeMatch = Arrays.stream(allowedTypes)
                .anyMatch(type -> type.equalsIgnoreCase(credentialType));
            if (!typeMatch) {
                throw new SecurityException("Authentication type not allowed: " + credentialType);
            }
        }

        // 检查权限
        String[] requiredPermissions = requireAuth.permissions();
        if (requiredPermissions.length > 0) {
            Set<String> permissions = AuthContext.getCurrentPermissions(request);
            for (String permission : requiredPermissions) {
                if (!permissions.contains(permission) && !permissions.contains("*")) {
                    throw new SecurityException("Permission denied: " + permission);
                }
            }
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(akSkAuth)")
    public Object checkAkSkAuth(ProceedingJoinPoint joinPoint, AkSkAuth akSkAuth) throws Throwable {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new SecurityException("Cannot get request context");
        }

        // 检查是否为AK/SK认证
        if (!AuthContext.isAkSkAuth(request)) {
            throw new SecurityException("AK/SK authentication required");
        }

        // 检查凭证类型
        String accessKey = AuthContext.getAccessKey(request);
        if (accessKey == null) {
            throw new SecurityException("Access Key not found");
        }

        // 验证权限
        String[] requiredPermissions = akSkAuth.permissions();
        if (requiredPermissions.length > 0) {
            Set<String> permissions = AuthContext.getCurrentPermissions(request);
            for (String permission : requiredPermissions) {
                if (!permissions.contains(permission) && !permissions.contains("*")) {
                    throw new SecurityException("Permission denied: " + permission);
                }
            }
        }

        log.debug("AK/SK auth passed: AK={}", accessKey);

        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}