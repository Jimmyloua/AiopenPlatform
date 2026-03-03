package com.aiplatform.platform.service;

import com.aiplatform.platform.dto.ConversationCreateRequest;
import com.aiplatform.platform.dto.ConversationResponse;
import com.aiplatform.platform.dto.MessageCreateRequest;
import com.aiplatform.platform.dto.MessageResponse;
import com.aiplatform.platform.model.Conversation;
import com.aiplatform.platform.model.Message;
import com.aiplatform.platform.repository.ConversationRepository;
import com.aiplatform.platform.repository.MessageRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Conversation Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AssistantService assistantService;

    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_TIMEOUT = 30; // minutes

    /**
     * Create a new conversation
     */
    public ConversationResponse createConversation(ConversationCreateRequest request, Long userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setAssistantId(request.getAssistantId());
        conversation.setTitle(request.getTitle() != null ? request.getTitle() : "New Conversation");

        conversationRepository.insert(conversation);

        // Create session in Redis
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                SESSION_PREFIX + sessionId,
                conversation.getId(),
                SESSION_TIMEOUT,
                TimeUnit.MINUTES
        );

        return toConversationResponse(conversation);
    }

    /**
     * Get conversation by ID
     */
    public ConversationResponse getConversationById(Long id, Long userId) {
        Conversation conversation = conversationRepository.selectById(id);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this conversation");
        }

        return toConversationResponse(conversation);
    }

    /**
     * List conversations for user
     */
    public Page<ConversationResponse> listConversations(int page, int size, Long userId) {
        Page<Conversation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdatedAt);

        Page<Conversation> result = conversationRepository.selectPage(pageParam, wrapper);

        Page<ConversationResponse> responsePage = new Page<>();
        responsePage.setCurrent(result.getCurrent());
        responsePage.setSize(result.getSize());
        responsePage.setTotal(result.getTotal());
        responsePage.setRecords(result.getRecords().stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList()));

        return responsePage;
    }

    /**
     * Delete conversation
     */
    public void deleteConversation(Long id, Long userId) {
        Conversation conversation = conversationRepository.selectById(id);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this conversation");
        }

        // Delete messages first
        messageRepository.delete(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, id));

        // Delete conversation
        conversationRepository.deleteById(id);
    }

    /**
     * Add message to conversation
     */
    public MessageResponse addMessage(MessageCreateRequest request, Long userId) {
        Conversation conversation = conversationRepository.selectById(request.getConversationId());
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this conversation");
        }

        Message message = new Message();
        message.setConversationId(request.getConversationId());
        message.setRole("user");
        message.setContent(request.getContent());
        message.setMetadata(request.getMetadata());

        messageRepository.insert(message);

        return toMessageResponse(message);
    }

    /**
     * Get messages for conversation
     */
    public List<MessageResponse> getMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.selectById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("Conversation not found");
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this conversation");
        }

        List<Message> messages = messageRepository.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt));

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert to ConversationResponse
     */
    private ConversationResponse toConversationResponse(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUserId(conversation.getUserId());
        response.setAssistantId(conversation.getAssistantId());
        response.setTitle(conversation.getTitle());
        response.setMetadata(conversation.getMetadata());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getAssistantId() != null) {
            try {
                response.setAssistant(assistantService.getAssistantById(conversation.getAssistantId()));
            } catch (Exception e) {
                log.warn("Failed to get assistant for conversation: {}", conversation.getId());
            }
        }

        return response;
    }

    /**
     * Convert to MessageResponse
     */
    private MessageResponse toMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversationId());
        response.setRole(message.getRole());
        response.setContent(message.getContent());
        response.setToolCalls(message.getToolCalls());
        response.setToolCallId(message.getToolCallId());
        response.setThinkingContent(message.getThinkingContent());
        response.setTokenCount(message.getTokenCount());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

}