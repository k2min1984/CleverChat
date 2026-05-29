package kr.co.cleverchat.domain.chatbot.service;

import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.common.security.FieldEncryptionService.EncryptedField;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.RecommendationSaveRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.SessionDetailResponse;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedbackQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatMessageTraceItem;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendationAdminItem;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAdminService {

    private static final int MAX_QUEUE_LIMIT = 100;
    private static final String ENCRYPTED_PLACEHOLDER = "[encrypted]";

    private final ChatFailureMapper failureMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatRecommendationMapper recommendationMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final FieldEncryptionService fieldEncryptionService;

    public ChatAdminService(
            ChatFailureMapper failureMapper,
            ChatFeedbackMapper feedbackMapper,
            ChatRecommendationMapper recommendationMapper,
            ChatSessionMapper sessionMapper,
            ChatMessageMapper messageMapper,
            FieldEncryptionService fieldEncryptionService) {
        this.failureMapper = failureMapper;
        this.feedbackMapper = feedbackMapper;
        this.recommendationMapper = recommendationMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.fieldEncryptionService = fieldEncryptionService;
    }

    @Transactional(readOnly = true)
    public List<ChatSessionListItem> sessions(Integer limit) {
        int resolvedLimit = limit == null ? 50 : Math.max(1, Math.min(limit, MAX_QUEUE_LIMIT));
        return decryptSessions(sessionMapper.findAdminList(resolvedLimit));
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse sessionDetail(String sessionId) {
        ChatSessionListItem session = sessionMapper.findAdminDetail(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        decryptSession(session);
        return new SessionDetailResponse(
                session,
                decryptTrace(messageMapper.findTraceBySessionId(sessionId)),
                decryptFailures(failureMapper.findBySessionId(sessionId)));
    }

    @Transactional(readOnly = true)
    public List<ChatFailureQueueItem> failures(Boolean reviewed, Integer limit) {
        int resolvedLimit = limit == null ? 50 : Math.max(1, Math.min(limit, MAX_QUEUE_LIMIT));
        return decryptFailures(failureMapper.findQueue(reviewed, resolvedLimit));
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void reviewFailure(Long id, Long reviewerId, String comment) {
        String normalizedComment = comment == null || comment.isBlank() ? null : comment.trim();
        EncryptedField encrypted = fieldEncryptionService.encryptNullable(normalizedComment);
        int updated =
                failureMapper.markReviewed(
                        id,
                        reviewerId,
                        normalizedComment == null ? null : ENCRYPTED_PLACEHOLDER,
                        encrypted.ciphertext(),
                        encrypted.keyId(),
                        encrypted.version());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatFeedbackQueueItem> feedback(String rating, Integer limit) {
        int resolvedLimit = limit == null ? 50 : Math.max(1, Math.min(limit, MAX_QUEUE_LIMIT));
        return decryptFeedback(feedbackMapper.findQueue(rating, resolvedLimit));
    }

    @Transactional(readOnly = true)
    public List<ChatRecommendationAdminItem> recommendations(Boolean enabled) {
        return recommendationMapper.findAdminList(enabled);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ChatRecommendation createRecommendation(RecommendationSaveRequest request) {
        ChatRecommendation recommendation = toRecommendation(null, request);
        recommendationMapper.insert(recommendation);
        return recommendation;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public ChatRecommendation updateRecommendation(Long id, RecommendationSaveRequest request) {
        ChatRecommendation recommendation = toRecommendation(id, request);
        int updated = recommendationMapper.update(recommendation);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return recommendationMapper.findById(id);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public void deleteRecommendation(Long id) {
        int deleted = recommendationMapper.delete(id);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    private ChatRecommendation toRecommendation(Long id, RecommendationSaveRequest request) {
        ChatRecommendation recommendation = new ChatRecommendation();
        recommendation.setId(id);
        recommendation.setScenarioId(request.scenarioId());
        recommendation.setLabel(request.label());
        recommendation.setPriority(request.priority() == null ? 100 : request.priority());
        recommendation.setEnabled(request.enabled() == null || request.enabled());
        return recommendation;
    }

    private List<ChatMessageTraceItem> decryptTrace(List<ChatMessageTraceItem> messages) {
        messages.forEach(
                message -> {
                    message.setContent(
                            fieldEncryptionService.decryptOrFallback(
                                    message.getContentCiphertext(),
                                    message.getContentKeyId(),
                                    message.getContent()));
                    message.setFeedbackComment(
                            fieldEncryptionService.decryptOrFallback(
                                    message.getFeedbackCommentCiphertext(),
                                    message.getFeedbackCommentKeyId(),
                                    message.getFeedbackComment()));
                });
        return messages;
    }

    private List<ChatSessionListItem> decryptSessions(List<ChatSessionListItem> sessions) {
        sessions.forEach(this::decryptSession);
        return sessions;
    }

    private void decryptSession(ChatSessionListItem session) {
        session.setLastMessage(
                fieldEncryptionService.decryptOrFallback(
                        session.getLastMessageCiphertext(),
                        session.getLastMessageKeyId(),
                        session.getLastMessage()));
    }

    private List<ChatFailureQueueItem> decryptFailures(List<ChatFailureQueueItem> failures) {
        failures.forEach(
                failure ->
                        failure.setReviewComment(
                                fieldEncryptionService.decryptOrFallback(
                                        failure.getReviewCommentCiphertext(),
                                        failure.getReviewCommentKeyId(),
                                        failure.getReviewComment())));
        return failures;
    }

    private List<ChatFeedbackQueueItem> decryptFeedback(List<ChatFeedbackQueueItem> feedback) {
        feedback.forEach(
                item ->
                        item.setComment(
                                fieldEncryptionService.decryptOrFallback(
                                        item.getCommentCiphertext(),
                                        item.getCommentKeyId(),
                                        item.getComment())));
        return feedback;
    }
}
