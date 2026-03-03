package com.aiplatform.platform.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Authentication Manager
 * 统一认证管理器，协调多种认证方式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationManager {

    private final List<AuthenticationProvider> providers;
    private Map<String, AuthenticationProvider> providerMap;

    @PostConstruct
    public void init() {
        providerMap = providers.stream()
            .collect(Collectors.toMap(
                AuthenticationProvider::getType,
                Function.identity()
            ));
        log.info("Initialized {} authentication providers: {}",
            providers.size(), providerMap.keySet());
    }

    /**
     * 执行认证
     * 根据Authorization header自动选择认证提供者
     */
    public AuthenticationProvider.AuthenticationResult authenticate(
            String authHeader,
            AuthenticationProvider.AuthRequest request) {

        if (authHeader == null || authHeader.isEmpty()) {
            return AuthenticationProvider.AuthenticationResult.failure("Missing Authorization header");
        }

        // 找到支持的提供者
        AuthenticationProvider provider = findProvider(authHeader);
        if (provider == null) {
            return AuthenticationProvider.AuthenticationResult.failure(
                "Unsupported authentication method");
        }

        // 提取凭证
        String credential = extractCredential(authHeader);

        log.debug("Authenticating with provider: {} for path: {}",
            provider.getType(), request.path());

        return provider.authenticate(credential, request);
    }

    /**
     * 根据Authorization header找到合适的提供者
     */
    private AuthenticationProvider findProvider(String authHeader) {
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(authHeader)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 从Authorization header提取凭证
     */
    private String extractCredential(String authHeader) {
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        } else if (authHeader.startsWith("AK ")) {
            return authHeader.substring(3);
        }
        return authHeader;
    }

    /**
     * 获取指定类型的提供者
     */
    public AuthenticationProvider getProvider(String type) {
        return providerMap.get(type);
    }

    /**
     * 获取所有支持的认证类型
     */
    public java.util.Set<String> getSupportedTypes() {
        return providerMap.keySet();
    }
}