package com.aiplatform.platform.controller;

import com.aiplatform.platform.dto.*;
import com.aiplatform.platform.service.AssistantService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Assistant Controller
 */
@Tag(name = "Assistant", description = "Assistant management APIs")
@RestController
@RequestMapping("/api/assistants")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @Operation(summary = "Create a new assistant")
    @PostMapping
    public ResponseEntity<ApiResponse<AssistantResponse>> createAssistant(
            @Valid @RequestBody AssistantCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            AssistantResponse response = assistantService.createAssistant(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Assistant created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get assistant by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssistantResponse>> getAssistant(@PathVariable Long id) {
        try {
            AssistantResponse response = assistantService.getAssistantById(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "List assistants")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AssistantResponse>>> listAssistants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean publicOnly) {
        Page<AssistantResponse> response = assistantService.listAssistants(page, size, userId, publicOnly);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update assistant")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AssistantResponse>> updateAssistant(
            @PathVariable Long id,
            @Valid @RequestBody AssistantCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            AssistantResponse response = assistantService.updateAssistant(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Assistant updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete assistant")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssistant(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            assistantService.deleteAssistant(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Assistant deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Publish assistant to plaza")
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AssistantResponse>> publishAssistant(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            AssistantResponse response = assistantService.publishAssistant(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Assistant submitted for review", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Approve assistant (admin only)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AssistantResponse>> approveAssistant(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("role") String role) {
        try {
            if (!"admin".equals(role)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
            }
            AssistantResponse response = assistantService.approveAssistant(id, comment);
            return ResponseEntity.ok(ApiResponse.success("Assistant approved", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Reject assistant (admin only)")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AssistantResponse>> rejectAssistant(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("role") String role) {
        try {
            if (!"admin".equals(role)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Admin access required"));
            }
            AssistantResponse response = assistantService.rejectAssistant(id, comment);
            return ResponseEntity.ok(ApiResponse.success("Assistant rejected", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}