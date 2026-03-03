package com.aiplatform.platform.controller;

import com.aiplatform.platform.dto.*;
import com.aiplatform.platform.service.ConversationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Conversation Controller
 */
@Tag(name = "Conversation", description = "Conversation management APIs")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "Create a new conversation")
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody ConversationCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            ConversationResponse response = conversationService.createConversation(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Conversation created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get conversation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            ConversationResponse response = conversationService.getConversationById(id, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "List conversations")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        Page<ConversationResponse> response = conversationService.listConversations(page, size, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Delete conversation")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            conversationService.deleteConversation(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Add message to conversation")
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            request.setConversationId(id);
            MessageResponse response = conversationService.addMessage(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Message added successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get messages for conversation")
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            List<MessageResponse> response = conversationService.getMessages(id, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}