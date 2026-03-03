package com.aiplatform.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Unified Authentication Filter
 * 统一认证过滤器，支持JWT和AK/SK等多种认证方式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String path = request.getRequestURI();

        // 跳过公开端点
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || authHeader.isEmpty()) {
            // 对于某些端点允许匿名访问
            if (isOptionalAuthEndpoint(path)) {
                filterChain.doFilter(request, response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 构建认证请求
            AuthenticationProvider.AuthRequest authRequest = buildAuthRequest(request);

            // 执行认证
            AuthenticationProvider.AuthenticationResult result =
                authenticationManager.authenticate(authHeader, authRequest);

            if (result.success()) {
                // 设置用户信息到请求属性
                request.setAttribute("userId", result.userId());
                request.setAttribute("username", result.username());
                request.setAttribute("role", result.role());
                request.setAttribute("credentialType", result.credentialType());
                request.setAttribute("accessKey", result.accessKey());
                request.setAttribute("permissions", result.permissions());

                // 设置SecurityContext
                String principal = result.username() != null ? result.username() : result.accessKey();
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    result.permissions() != null ?
                        result.permissions().stream()
                            .map(p -> new org.springframework.security.core.authority.SimpleGrantedAuthority(p))
                            .toList() :
                        new ArrayList<>()
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authentication successful: type={}, userId={}, path={}",
                    result.credentialType(), result.userId(), path);
            } else {
                log.warn("Authentication failed: {} for path: {}", result.errorMessage(), path);
                // 认证失败但不阻止请求，让具体控制器处理权限
                request.setAttribute("authError", result.errorMessage());
            }

        } catch (Exception e) {
            log.error("Authentication error", e);
            request.setAttribute("authError", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 构建认证请求
     */
    private AuthenticationProvider.AuthRequest buildAuthRequest(HttpServletRequest request) {
        return new AuthenticationProvider.AuthRequest(
            request.getMethod(),
            request.getRequestURI(),
            request.getHeader("X-Timestamp"),
            request.getHeader("X-Nonce"),
            request.getHeader("X-Signature"),
            getRequestBodyHash(request),
            getClientIp(request)
        );
    }

    /**
     * 获取请求体哈希（用于签名验证）
     */
    private String getRequestBodyHash(HttpServletRequest request) {
        // 请求体只能读取一次，这里使用简化的方式
        // 实际应用中可能需要ContentCachingRequestWrapper
        return null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个IP（如果有多个代理）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 判断是否为公开端点
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/users/register") ||
               path.startsWith("/api/users/login") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/actuator") ||
               path.startsWith("/v1/models") ||
               path.equals("/health");
    }

    /**
     * 判断是否为可选认证端点
     */
    private boolean isOptionalAuthEndpoint(String path) {
        // V1 API可以通过AK/SK认证，也可以匿名（有其他限制）
        return path.startsWith("/v1/");
    }
}