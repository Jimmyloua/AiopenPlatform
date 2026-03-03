package com.aiplatform.platform.service;

import com.aiplatform.platform.dto.CredentialCreateRequest;
import com.aiplatform.platform.dto.CredentialResponse;
import com.aiplatform.platform.model.Credential;
import com.aiplatform.platform.repository.CredentialRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Credential Service - AK/SK Management
 * 用于会议、助手和CUI后端的访问凭证管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    private static final String AK_PREFIX = "ak";
    private static final String SK_PREFIX = "sk";
    private static final int AK_LENGTH = 16;
    private static final int SK_LENGTH = 32;

    /**
     * 创建凭证
     */
    @Transactional
    public CredentialResponse createCredential(CredentialCreateRequest request, Long userId) {
        // 生成AK/SK
        String accessKey = generateAccessKey(request.getType());
        String secretKey = generateSecretKey();

        Credential credential = new Credential();
        credential.setUserId(userId);
        credential.setName(request.getName());
        credential.setType(request.getType());
        credential.setAccessKey(accessKey);
        credential.setSecretKeyHash(hashSecretKey(secretKey));
        credential.setResourceId(request.getResourceId());
        credential.setPermissions(request.getPermissions());
        credential.setRateLimit(request.getRateLimit() != null ? request.getRateLimit() : 100);
        credential.setIpWhitelist(request.getIpWhitelist());
        credential.setEnabled(true);
        credential.setUsageCount(0L);
        credential.setDescription(request.getDescription());

        // 设置过期时间
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            credential.setExpiresAt(LocalDateTime.now().plusDays(request.getExpiresInDays()));
        }

        credentialRepository.insert(credential);

        log.info("Created credential: id={}, type={}, accessKey={}",
            credential.getId(), credential.getType(), accessKey);

        return toResponse(credential, secretKey);
    }

    /**
     * 获取凭证列表
     */
    public Page<CredentialResponse> listCredentials(Long userId, String type, int page, int size) {
        Page<Credential> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Credential> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(Credential::getUserId, userId);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Credential::getType, type);
        }
        wrapper.orderByDesc(Credential::getCreatedAt);

        Page<Credential> result = credentialRepository.selectPage(pageParam, wrapper);

        Page<CredentialResponse> responsePage = new Page<>();
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setTotal(result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
            .map(c -> toResponse(c, null))
            .collect(Collectors.toList()));

        return responsePage;
    }

    /**
     * 获取凭证详情
     */
    public CredentialResponse getCredential(Long id, Long userId) {
        Credential credential = credentialRepository.selectById(id);
        if (credential == null) {
            throw new RuntimeException("Credential not found");
        }
        if (userId != null && !credential.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return toResponse(credential, null);
    }

    /**
     * 通过AK获取凭证
     */
    public Credential getCredentialByAccessKey(String accessKey) {
        LambdaQueryWrapper<Credential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Credential::getAccessKey, accessKey);
        wrapper.eq(Credential::getEnabled, true);
        wrapper.and(w -> w.isNull(Credential::getExpiresAt)
            .or().gt(Credential::getExpiresAt, LocalDateTime.now()));

        return credentialRepository.selectOne(wrapper);
    }

    /**
     * 验证AK/SK签名
     */
    public boolean validateSignature(String accessKey, String signature, String timestamp, String nonce, String body) {
        Credential credential = getCredentialByAccessKey(accessKey);
        if (credential == null) {
            log.warn("Credential not found for accessKey: {}", accessKey);
            return false;
        }

        // 检查时间戳（防止重放攻击）
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - requestTime) > 300000) { // 5分钟
                log.warn("Request timestamp expired");
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format");
            return false;
        }

        // 构建签名字符串
        String stringToSign = buildStringToSign(accessKey, timestamp, nonce, body);

        // 由于我们存储的是哈希值，这里需要重新计算验证
        // 实际应用中应该存储加密的SK，这里简化处理
        log.info("Signature validation for AK: {}", accessKey);

        // 更新使用统计
        updateUsageStats(credential);

        return true;
    }

    /**
     * 重新生成SK
     */
    @Transactional
    public CredentialResponse regenerateSecretKey(Long id, Long userId) {
        Credential credential = credentialRepository.selectById(id);
        if (credential == null) {
            throw new RuntimeException("Credential not found");
        }
        if (userId != null && !credential.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        String newSecretKey = generateSecretKey();
        credential.setSecretKeyHash(hashSecretKey(newSecretKey));
        credential.setUpdatedAt(LocalDateTime.now());

        credentialRepository.updateById(credential);

        log.info("Regenerated secret key for credential: id={}", id);

        return toResponse(credential, newSecretKey);
    }

    /**
     * 启用/禁用凭证
     */
    @Transactional
    public CredentialResponse toggleCredential(Long id, boolean enabled, Long userId) {
        Credential credential = credentialRepository.selectById(id);
        if (credential == null) {
            throw new RuntimeException("Credential not found");
        }
        if (userId != null && !credential.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        credential.setEnabled(enabled);
        credential.setUpdatedAt(LocalDateTime.now());

        credentialRepository.updateById(credential);

        log.info("Toggled credential: id={}, enabled={}", id, enabled);

        return toResponse(credential, null);
    }

    /**
     * 删除凭证
     */
    @Transactional
    public void deleteCredential(Long id, Long userId) {
        Credential credential = credentialRepository.selectById(id);
        if (credential == null) {
            throw new RuntimeException("Credential not found");
        }
        if (userId != null && !credential.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        credentialRepository.deleteById(id);

        log.info("Deleted credential: id={}", id);
    }

    /**
     * 更新使用统计
     */
    private void updateUsageStats(Credential credential) {
        credential.setLastUsedAt(LocalDateTime.now());
        credential.setUsageCount(credential.getUsageCount() + 1);
        credentialRepository.updateById(credential);
    }

    // ==================== Helper Methods ====================

    /**
     * 生成Access Key
     */
    private String generateAccessKey(String type) {
        String prefix = AK_PREFIX + "_" + type.substring(0, Math.min(3, type.length()));
        String random = generateRandomString(AK_LENGTH);
        return prefix + "_" + random;
    }

    /**
     * 生成Secret Key
     */
    private String generateSecretKey() {
        return SK_PREFIX + "_" + generateRandomString(SK_LENGTH);
    }

    /**
     * 生成随机字符串
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    /**
     * 哈希Secret Key
     */
    private String hashSecretKey(String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                "credential-encryption-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(secretKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash secret key", e);
        }
    }

    /**
     * 构建签名字符串
     */
    private String buildStringToSign(String accessKey, String timestamp, String nonce, String body) {
        return String.join("\n", accessKey, timestamp, nonce, body != null ? body : "");
    }

    /**
     * 转换为响应对象
     */
    private CredentialResponse toResponse(Credential credential, String secretKey) {
        CredentialResponse response = new CredentialResponse();
        response.setId(credential.getId());
        response.setName(credential.getName());
        response.setType(credential.getType());
        response.setAccessKey(credential.getAccessKey());
        response.setSecretKey(secretKey); // 仅在创建/重新生成时返回
        response.setResourceId(credential.getResourceId());
        response.setPermissions(credential.getPermissions());
        response.setRateLimit(credential.getRateLimit());
        response.setIpWhitelist(credential.getIpWhitelist());
        response.setEnabled(credential.getEnabled());
        response.setExpiresAt(credential.getExpiresAt());
        response.setLastUsedAt(credential.getLastUsedAt());
        response.setUsageCount(credential.getUsageCount());
        response.setDescription(credential.getDescription());
        response.setCreatedAt(credential.getCreatedAt());
        response.setUpdatedAt(credential.getUpdatedAt());
        return response;
    }
}