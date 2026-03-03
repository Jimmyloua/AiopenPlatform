package com.aiplatform.platform.security;

import com.aiplatform.platform.model.Credential;
import com.aiplatform.platform.service.CredentialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

/**
 * AK/SK Authentication Provider
 * AK/SK认证提供者，用于会议、助手和CUI后端的服务间认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AkSkAuthenticationProvider implements AuthenticationProvider {

    private final CredentialService credentialService;
    private final ObjectMapper objectMapper;

    private static final long TIMESTAMP_TOLERANCE_MS = 300000; // 5分钟
    private static final String SIGNATURE_METHOD = "HMAC-SHA256";

    @Override
    public String getType() {
        return "aksk";
    }

    @Override
    public AuthenticationResult authenticate(String credential, AuthRequest request) {
        try {
            // 获取凭证
            Credential cred = credentialService.getCredentialByAccessKey(credential);
            if (cred == null) {
                return AuthenticationResult.failure("Invalid Access Key");
            }

            if (!Boolean.TRUE.equals(cred.getEnabled())) {
                return AuthenticationResult.failure("Credential is disabled");
            }

            // 验证时间戳（防止重放攻击）
            if (request.timestamp() != null) {
                try {
                    long requestTime = Long.parseLong(request.timestamp());
                    long currentTime = System.currentTimeMillis();
                    if (Math.abs(currentTime - requestTime) > TIMESTAMP_TOLERANCE_MS) {
                        return AuthenticationResult.failure("Request timestamp expired");
                    }
                } catch (NumberFormatException e) {
                    return AuthenticationResult.failure("Invalid timestamp format");
                }
            }

            // 验证签名（如果提供）
            if (request.signature() != null && !request.signature().isEmpty()) {
                // 签名验证逻辑
                // 注意：实际应用中需要存储加密的SK进行验证
                log.debug("Signature validation for AK: {}", credential);
            }

            // 验证IP白名单
            if (cred.getIpWhitelist() != null && !cred.getIpWhitelist().isEmpty()) {
                if (!isIpAllowed(cred.getIpWhitelist(), request.clientIp())) {
                    return AuthenticationResult.failure("IP not in whitelist");
                }
            }

            // 解析权限
            Set<String> permissions = parsePermissions(cred.getPermissions());

            log.info("AK/SK authentication successful: AK={}, type={}",
                credential, cred.getType());

            return AuthenticationResult.successWithAkSk(
                cred.getUserId(),
                cred.getAccessKey(),
                permissions
            );

        } catch (Exception e) {
            log.error("AK/SK authentication failed", e);
            return AuthenticationResult.failure("AK/SK authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String authHeader) {
        return authHeader != null && authHeader.startsWith("AK ");
    }

    /**
     * 验证签名
     */
    public boolean validateSignature(String accessKey, String secretKey,
            String timestamp, String nonce, String body, String signature) {
        try {
            String stringToSign = buildStringToSign(accessKey, timestamp, nonce, body);
            String expectedSignature = calculateSignature(secretKey, stringToSign);
            return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature validation failed", e);
            return false;
        }
    }

    /**
     * 构建签名字符串
     */
    private String buildStringToSign(String accessKey, String timestamp, String nonce, String body) {
        return String.join("\n",
            accessKey,
            SIGNATURE_METHOD,
            timestamp != null ? timestamp : "",
            nonce != null ? nonce : "",
            body != null ? digestBody(body) : ""
        );
    }

    /**
     * 计算签名
     */
    private String calculateSignature(String secretKey, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    /**
     * 对请求体进行摘要
     */
    private String digestBody(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 检查IP是否在白名单中
     */
    private boolean isIpAllowed(String ipWhitelistJson, String clientIp) {
        if (ipWhitelistJson == null || ipWhitelistJson.isEmpty() || clientIp == null) {
            return true;
        }
        try {
            java.util.List<String> whitelist = objectMapper.readValue(ipWhitelistJson,
                objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class));
            return whitelist.isEmpty() || whitelist.contains(clientIp) ||
                   whitelist.stream().anyMatch(pattern -> matchIpPattern(pattern, clientIp));
        } catch (Exception e) {
            log.warn("Failed to parse IP whitelist", e);
            return true;
        }
    }

    /**
     * IP模式匹配
     */
    private boolean matchIpPattern(String pattern, String ip) {
        if (pattern.endsWith("*")) {
            return ip.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equals(ip);
    }

    /**
     * 解析权限
     */
    @SuppressWarnings("unchecked")
    private Set<String> parsePermissions(String permissionsJson) {
        if (permissionsJson == null || permissionsJson.isEmpty()) {
            return Set.of();
        }
        try {
            return objectMapper.readValue(permissionsJson,
                objectMapper.getTypeFactory().constructCollectionType(Set.class, String.class));
        } catch (Exception e) {
            log.warn("Failed to parse permissions", e);
            return Set.of();
        }
    }
}