package com.aiplatform.platform.service;

import com.aiplatform.platform.dto.AssistantCreateRequest;
import com.aiplatform.platform.dto.AssistantResponse;
import com.aiplatform.platform.dto.SkillResponse;
import com.aiplatform.platform.dto.UserResponse;
import com.aiplatform.platform.model.Assistant;
import com.aiplatform.platform.repository.AssistantRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Assistant Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantRepository assistantRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new assistant
     */
    public AssistantResponse createAssistant(AssistantCreateRequest request, Long userId) {
        Assistant assistant = new Assistant();
        assistant.setName(request.getName());
        assistant.setDescription(request.getDescription());
        assistant.setAvatar(request.getAvatar());
        assistant.setSystemPrompt(request.getSystemPrompt());
        assistant.setModelConfig(toJson(request.getModelConfig()));
        assistant.setCapabilities(toJson(request.getCapabilities()));
        assistant.setIsPublic(false);
        assistant.setStatus("draft");
        assistant.setCreatedBy(userId);

        assistantRepository.insert(assistant);

        return toAssistantResponse(assistant);
    }

    /**
     * Get assistant by ID
     */
    public AssistantResponse getAssistantById(Long id) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }
        return toAssistantResponse(assistant);
    }

    /**
     * List assistants with pagination
     */
    public Page<AssistantResponse> listAssistants(int page, int size, Long userId, Boolean publicOnly) {
        Page<Assistant> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Assistant> wrapper = new LambdaQueryWrapper<>();

        if (publicOnly != null && publicOnly) {
            wrapper.eq(Assistant::getIsPublic, true)
                    .eq(Assistant::getStatus, "published");
        } else if (userId != null) {
            wrapper.eq(Assistant::getCreatedBy, userId);
        }

        wrapper.orderByDesc(Assistant::getCreatedAt);

        Page<Assistant> result = assistantRepository.selectPage(pageParam, wrapper);

        Page<AssistantResponse> responsePage = new Page<>();
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setTotal(result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(this::toAssistantResponse)
                .collect(Collectors.toList()));

        return responsePage;
    }

    /**
     * Update assistant
     */
    public AssistantResponse updateAssistant(Long id, AssistantCreateRequest request, Long userId) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }

        if (!assistant.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Not authorized to update this assistant");
        }

        assistant.setName(request.getName());
        assistant.setDescription(request.getDescription());
        assistant.setAvatar(request.getAvatar());
        assistant.setSystemPrompt(request.getSystemPrompt());
        assistant.setModelConfig(toJson(request.getModelConfig()));
        assistant.setCapabilities(toJson(request.getCapabilities()));

        assistantRepository.updateById(assistant);

        return toAssistantResponse(assistant);
    }

    /**
     * Delete assistant
     */
    public void deleteAssistant(Long id, Long userId) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }

        if (!assistant.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this assistant");
        }

        assistantRepository.deleteById(id);
    }

    /**
     * Publish assistant to plaza (requires review)
     */
    public AssistantResponse publishAssistant(Long id, Long userId) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }

        if (!assistant.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Not authorized to publish this assistant");
        }

        assistant.setIsPublic(true);
        assistant.setStatus("pending_review");

        assistantRepository.updateById(assistant);

        return toAssistantResponse(assistant);
    }

    /**
     * Approve assistant (admin only)
     */
    public AssistantResponse approveAssistant(Long id, String comment) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }

        assistant.setStatus("published");
        assistant.setReviewComment(comment);

        assistantRepository.updateById(assistant);

        return toAssistantResponse(assistant);
    }

    /**
     * Reject assistant (admin only)
     */
    public AssistantResponse rejectAssistant(Long id, String comment) {
        Assistant assistant = assistantRepository.selectById(id);
        if (assistant == null) {
            throw new RuntimeException("Assistant not found");
        }

        assistant.setStatus("rejected");
        assistant.setReviewComment(comment);
        assistant.setIsPublic(false);

        assistantRepository.updateById(assistant);

        return toAssistantResponse(assistant);
    }

    /**
     * Convert to AssistantResponse
     */
    private AssistantResponse toAssistantResponse(Assistant assistant) {
        AssistantResponse response = new AssistantResponse();
        response.setId(assistant.getId());
        response.setName(assistant.getName());
        response.setDescription(assistant.getDescription());
        response.setAvatar(assistant.getAvatar());
        response.setSystemPrompt(assistant.getSystemPrompt());
        response.setModelConfig(assistant.getModelConfig());
        response.setCapabilities(fromJson(assistant.getCapabilities(), new TypeReference<List<String>>() {}));
        response.setIsPublic(assistant.getIsPublic());
        response.setStatus(assistant.getStatus());
        response.setReviewComment(assistant.getReviewComment());
        response.setCreatedAt(assistant.getCreatedAt());
        response.setUpdatedAt(assistant.getUpdatedAt());

        if (assistant.getCreatedBy() != null) {
            try {
                UserResponse userResponse = userService.getUserById(assistant.getCreatedBy());
                response.setCreatedBy(userResponse);
            } catch (Exception e) {
                log.warn("Failed to get user for assistant: {}", assistant.getId());
            }
        }

        return response;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}