package com.aiplatform.platform.controller;

import com.aiplatform.platform.dto.ApiResponse;
import com.aiplatform.platform.dto.CredentialCreateRequest;
import com.aiplatform.platform.dto.CredentialResponse;
import com.aiplatform.platform.service.CredentialService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Credential Controller - AK/SK Management API
 * 用于会议、助手和CUI后端的访问凭证管理
 */
@Slf4j
@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * 创建凭证
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CredentialResponse>> createCredential(
            @RequestBody CredentialCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Creating credential: name={}, type={}, userId={}",
            request.getName(), request.getType(), userId);

        try {
            CredentialResponse response = credentialService.createCredential(request, userId);
            return ResponseEntity.ok(ApiResponse.success("凭证创建成功", response));
        } catch (Exception e) {
            log.error("Failed to create credential", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("凭证创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取凭证列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CredentialResponse>>> listCredentials(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CredentialResponse> result = credentialService.listCredentials(userId, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取凭证详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CredentialResponse>> getCredential(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        try {
            CredentialResponse response = credentialService.getCredential(id, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get credential", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取凭证失败: " + e.getMessage()));
        }
    }

    /**
     * 重新生成Secret Key
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ApiResponse<CredentialResponse>> regenerateSecretKey(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Regenerating secret key for credential: id={}, userId={}", id, userId);

        try {
            CredentialResponse response = credentialService.regenerateSecretKey(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Secret Key已重新生成", response));
        } catch (Exception e) {
            log.error("Failed to regenerate secret key", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("重新生成失败: " + e.getMessage()));
        }
    }

    /**
     * 启用/禁用凭证
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<CredentialResponse>> toggleCredential(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Toggling credential: id={}, enabled={}, userId={}", id, enabled, userId);

        try {
            CredentialResponse response = credentialService.toggleCredential(id, enabled, userId);
            return ResponseEntity.ok(ApiResponse.success( enabled ? "凭证已启用" : "凭证已禁用", response
               ));
        } catch (Exception e) {
            log.error("Failed to toggle credential", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }

    /**
     * 删除凭证
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Deleting credential: id={}, userId={}", id, userId);

        try {
            credentialService.deleteCredential(id, userId);
            return ResponseEntity.ok(ApiResponse.success("凭证已删除", null));
        } catch (Exception e) {
            log.error("Failed to delete credential", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 验证AK/SK签名（供内部服务调用）
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSignature(
            @RequestBody Map<String, String> request) {

        String accessKey = request.get("accessKey");
        String signature = request.get("signature");
        String timestamp = request.get("timestamp");
        String nonce = request.get("nonce");
        String body = request.get("body");

        boolean valid = credentialService.validateSignature(
            accessKey, signature, timestamp, nonce, body);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "valid", valid,
            "accessKey", accessKey
        )));
    }

    /**
     * 获取凭证类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCredentialTypes() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "meeting", "会议凭证",
            "assistant", "助手凭证",
            "cui_backend", "CUI后端凭证"
        )));
    }
}