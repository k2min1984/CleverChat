package kr.co.cleverchat.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.common.security.FieldEncryptionService.EncryptedField;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerContext;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionService;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FeedbackResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.HistorySessionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.MessageResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.OptionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.RecommendationResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.ScenarioSummaryResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SessionResponse;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedback;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRuntimeService {

    private static final int SESSION_TTL_MINUTES = 30;
    private static final String ENCRYPTED_PLACEHOLDER = "[encrypted]";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatFailureRecorder failureRecorder;
    private final ChatRecommendationMapper recommendationMapper;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioVersionMapper versionMapper;
    private final ScenarioNodeMapper nodeMapper;
    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioMatchingService matchingService;
    private final ChatPiiGuard piiGuard;
    private final ChatRateLimiter rateLimiter;
    private final SearchService searchService;
    private final AiAnswerSuggestionService aiAnswerSuggestionService;
    private final FieldEncryptionService fieldEncryptionService;
    private final ObjectMapper objectMapper;

    public ChatRuntimeService(
            ChatSessionMapper sessionMapper,
            ChatMessageMapper messageMapper,
            ChatFeedbackMapper feedbackMapper,
            ChatFailureRecorder failureRecorder,
            ChatRecommendationMapper recommendationMapper,
            ScenarioMapper scenarioMapper,
            ScenarioVersionMapper versionMapper,
            ScenarioNodeMapper nodeMapper,
            ScenarioNodeOptionMapper optionMapper,
            ScenarioMatchingService matchingService,
            ChatPiiGuard piiGuard,
            ChatRateLimiter rateLimiter,
            SearchService searchService,
            AiAnswerSuggestionService aiAnswerSuggestionService,
            FieldEncryptionService fieldEncryptionService,
            ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.feedbackMapper = feedbackMapper;
        this.failureRecorder = failureRecorder;
        this.recommendationMapper = recommendationMapper;
        this.scenarioMapper = scenarioMapper;
        this.versionMapper = versionMapper;
        this.nodeMapper = nodeMapper;
        this.optionMapper = optionMapper;
        this.matchingService = matchingService;
        this.piiGuard = piiGuard;
        this.rateLimiter = rateLimiter;
        this.searchService = searchService;
        this.aiAnswerSuggestionService = aiAnswerSuggestionService;
        this.fieldEncryptionService = fieldEncryptionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionResponse start(Long scenarioId, ChatRequestContext context) {
        Scenario scenario =
                Optional.ofNullable(scenarioMapper.findById(scenarioId))
                        .filter(value -> "ACTIVE".equals(value.getStatus()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "활성 시나리오를 찾을 수 없습니다."));
        ScenarioVersion version =
                Optional.ofNullable(versionMapper.findPublishedByScenarioId(scenario.getScenarioNo()))
                        .filter(value -> value.getStartNodeNo() != null)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.STATE_CONFLICT, "게시된 시작 노드가 없습니다."));
        ScenarioNode startNode =
                Optional.ofNullable(nodeMapper.findById(version.getStartNodeNo()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.STATE_CONFLICT, "시작 노드를 찾을 수 없습니다."));

        ChatSession session = new ChatSession();
        session.setChatSessionNo(UUID.randomUUID().toString());
        session.setAnonymousId(context.anonymousId().toString());
        session.setScenarioNo(scenario.getScenarioNo());
        session.setVersionNo(version.getScenarioVersionNo());
        session.setCurrentNodeNo(startNode.getScenarioNodeNo());
        session.setState(stateFor(startNode));
        session.setExpiresAt(expiresAt());
        session.setIpHash(hash(context.ipAddress()));
        session.setUserAgentHash(hash(context.userAgent()));
        sessionMapper.insert(session);

        insertBotMessage(
                session.getChatSessionNo(),
                1,
                startNode,
                startNode.getContent() == null ? startNode.getTitle() : startNode.getContent());
        return response(session.getChatSessionNo());
    }

    @Transactional
    public SessionResponse get(UUID sessionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        ChatSession session = findSession(sid);
        validateOwner(session, context);
        ensureNotExpired(session);
        return response(sid);
    }

    @Transactional(readOnly = true)
    public SessionResponse history(UUID sessionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        ChatSession session = findSession(sid);
        validateOwner(session, context);
        return response(sid);
    }

    @Transactional(readOnly = true)
    public List<HistorySessionResponse> historyList(UUID anonymousId) {
        return sessionMapper
                .findHistoryByAnonymousId(
                        anonymousId.toString(), OffsetDateTime.now().minusDays(90), 50)
                .stream()
                .map(this::toHistorySessionResponse)
                .toList();
    }

    @Transactional
    public SessionResponse selectOption(UUID sessionId, Long optionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);
        enforceRateLimit(session, context);

        ScenarioNodeOption option =
                optionMapper.findEnabledByNodeId(session.getCurrentNodeNo()).stream()
                        .filter(value -> value.getScenarioNodeOptionNo().equals(optionId))
                        .findFirst()
                        .orElseThrow(
                                () -> {
                                    Map<String, Object> detail = new LinkedHashMap<>();
                                    detail.put("requestedOptionId", optionId);
                                    failureRecorder.recordFailure(
                                            sid, null, "INVALID_OPTION", detail);
                                    return new BusinessException(
                                            ErrorCode.VALIDATION_ERROR, "선택할 수 없는 옵션입니다.");
                                });

        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(
                sid,
                seq,
                session.getCurrentNodeNo(),
                option.getScenarioNodeOptionNo(),
                option.getLabel(),
                "{}",
                null);
        advance(session, option.getNextNodeNo(), seq + 1);
        return response(sid);
    }

    @Transactional
    public SessionResponse freeText(UUID sessionId, String text, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);
        enforceRateLimit(session, context);
        List<String> piiTypes = piiGuard.detectTypes(text);
        if (piiGuard.hasHighRiskTypes(piiTypes)) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("piiTypes", piiTypes);
            detail.put("inputLength", text == null ? 0 : text.length());
            detail.put("currentNodeId", session.getCurrentNodeNo());
            failureRecorder.recordFailure(sid, null, "PII_BLOCKED", detail);
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Personal information cannot be submitted.");
        }
        String safeText = piiTypes.isEmpty() ? text : piiGuard.maskLowRisk(text);

        long startedAt = System.nanoTime();
        Optional<ScenarioMatchingService.MatchResult> match =
                matchingService.match(
                        session.getScenarioNo(), session.getCurrentNodeNo(), safeText);
        int latencyMs = latencyMsSince(startedAt);
        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(
                sid, seq, session.getCurrentNodeNo(), null, safeText, matchingPayload(match), null);
        if (match.isEmpty()) {
            var fallback =
                    searchService.search(
                            safeText,
                            "CHAT_FALLBACK",
                            3,
                            null,
                            hash(context.anonymousId().toString()));
            if (fallback.resultCount() > 0) {
                var top = fallback.results().get(0);
                String prefix =
                        "CRAWL_DOCUMENT".equals(top.getMatchedField()) ? "관련 자료: " : "관련 상담: ";
                String fallbackMessage = prefix + top.getScenarioTitle();
                String botMessage =
                        aiAnswerSuggestionService
                                .suggest(
                                        safeText,
                                        fallback.results(),
                                        new AiAnswerContext(
                                                session.getScenarioNo(),
                                                session.getCurrentNodeNo(),
                                                "CHAT_FALLBACK"))
                                .map(response -> response.answer())
                                .orElse(fallbackMessage);
                insertBotMessage(sid, seq + 1, null, botMessage, latencyMs);
                return response(sid);
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("inputLength", text == null ? 0 : text.length());
            detail.put("currentNodeId", session.getCurrentNodeNo());
            detail.put("fallbackSource", "M4_SEARCH");
            detail.put("matchedScenarioIds", List.of());
            failureRecorder.recordFailure(sid, null, "NO_MATCH", detail);
            insertBotMessage(sid, seq + 1, null, "질문에 맞는 답변을 찾지 못했습니다.", latencyMs);
            return response(sid);
        }

        ScenarioMatchingService.MatchResult result = match.get();
        if (result.nextNodeId() != null) {
            advance(session, result.nextNodeId(), seq + 1, latencyMs);
            return response(sid);
        }

        Scenario scenario =
                Optional.ofNullable(scenarioMapper.findById(result.scenarioId()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        insertBotMessage(sid, seq + 1, null, scenario.getTitle(), latencyMs);
        return response(sid);
    }

    @Transactional(readOnly = true)
    public List<ScenarioSummaryResponse> activeScenarios() {
        return scenarioMapper.findAll("ACTIVE").stream()
                .map(
                        scenario ->
                                new ScenarioSummaryResponse(
                                        scenario.getScenarioNo(),
                                        scenario.getTitle(),
                                        scenario.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendations() {
        return recommendationMapper.findEnabledForActiveScenarios().stream()
                .map(this::toRecommendationResponse)
                .toList();
    }

    @Transactional
    public FeedbackResponse feedback(
            Long messageId, String rating, String comment, ChatRequestContext context) {
        ChatMessage message =
                messageMapper
                        .findById(messageId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "메시지를 찾을 수 없습니다."));
        ChatSession session = findSession(message.getSessionNo());
        validateOwner(session, context);
        if (!"BOT".equals(message.getDirection())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "BOT 메시지에만 만족도를 등록할 수 있습니다.");
        }
        String safeComment = safeFeedbackComment(comment);
        ChatFeedback feedback = new ChatFeedback();
        feedback.setMessageNo(messageId);
        feedback.setRating(rating);
        applyEncryptedComment(feedback, safeComment);
        feedback.setIpHash(hash(context.ipAddress()));
        feedbackMapper.upsert(feedback);
        return new FeedbackResponse(messageId, rating, safeComment);
    }

    private void advance(ChatSession session, Long nextNodeId, int botSeq) {
        advance(session, nextNodeId, botSeq, null);
    }

    private void advance(ChatSession session, Long nextNodeId, int botSeq, Integer latencyMs) {
        if (nextNodeId == null) {
            sessionMapper.updateCurrentNode(
                    session.getChatSessionNo(), session.getCurrentNodeNo(), "COMPLETED", expiresAt());
            insertBotMessage(session.getChatSessionNo(), botSeq, null, "대화가 완료되었습니다.", latencyMs);
            return;
        }
        ScenarioNode nextNode =
                Optional.ofNullable(nodeMapper.findById(nextNodeId))
                        .filter(node -> session.getVersionNo().equals(node.getVersionNo()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.STATE_CONFLICT, "같은 버전의 다음 노드가 아닙니다."));
        sessionMapper.updateCurrentNode(
                session.getChatSessionNo(), nextNode.getScenarioNodeNo(), stateFor(nextNode), expiresAt());
        insertBotMessage(
                session.getChatSessionNo(),
                botSeq,
                nextNode,
                nextNode.getContent() == null ? nextNode.getTitle() : nextNode.getContent(),
                latencyMs);
    }

    private ChatMessage insertUserMessage(
            String sessionId,
            int seq,
            Long nodeId,
            Long optionId,
            String content,
            String payload,
            Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionNo(sessionId);
        message.setSeq(seq);
        message.setDirection("USER");
        message.setNodeNo(nodeId);
        message.setOptionNo(optionId);
        applyEncryptedContent(message, content);
        message.setPayload(payload);
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private ChatMessage insertBotMessage(
            String sessionId, int seq, ScenarioNode node, String content) {
        return insertBotMessage(sessionId, seq, node, content, null);
    }

    private ChatMessage insertBotMessage(
            String sessionId, int seq, ScenarioNode node, String content, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionNo(sessionId);
        message.setSeq(seq);
        message.setDirection("BOT");
        message.setNodeNo(node == null ? null : node.getScenarioNodeNo());
        applyEncryptedContent(
                message, content == null || content.isBlank() ? "응답 내용이 없습니다." : content);
        message.setPayload("{}");
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private SessionResponse response(String sessionId) {
        ChatSession session = findSession(sessionId);
        List<MessageResponse> messages =
                messageMapper.findBySessionId(sessionId).stream()
                        .map(this::toMessageResponse)
                        .toList();
        List<OptionResponse> options =
                "ACTIVE".equals(session.getState())
                        ? optionMapper.findEnabledByNodeId(session.getCurrentNodeNo()).stream()
                                .map(this::toOptionResponse)
                                .toList()
                        : List.of();
        return new SessionResponse(
                UUID.fromString(session.getChatSessionNo()),
                session.getScenarioNo(),
                session.getVersionNo(),
                session.getCurrentNodeNo(),
                session.getState(),
                session.getExpiresAt(),
                messages,
                options);
    }

    private ChatSession findSession(String sessionId) {
        return sessionMapper
                .findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."));
    }

    private void validateOwnerAndActive(ChatSession session, ChatRequestContext context) {
        validateOwner(session, context);
        ensureNotExpired(session);
        if (!"ACTIVE".equals(session.getState())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "진행 가능한 세션이 아닙니다.");
        }
    }

    private void validateOwner(ChatSession session, ChatRequestContext context) {
        if (!session.getAnonymousId().equals(context.anonymousId().toString())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void ensureNotExpired(ChatSession session) {
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())
                || "EXPIRED".equals(session.getState())) {
            if (!"EXPIRED".equals(session.getState())) {
                sessionMapper.markExpired(session.getChatSessionNo());
                failureRecorder.recordFailure(session.getChatSessionNo(), null, "EXPIRED");
            }
            throw new BusinessException(ErrorCode.SESSION_EXPIRED, "Session expired.");
        }
    }

    private void enforceRateLimit(ChatSession session, ChatRequestContext context) {
        String key = context.anonymousId() + ":" + session.getChatSessionNo();
        if (!rateLimiter.isAllowed(key)) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("currentNodeId", session.getCurrentNodeNo());
            failureRecorder.recordFailure(session.getChatSessionNo(), null, "RATE_LIMITED", detail);
            throw new BusinessException(ErrorCode.RATE_LIMITED, "Too many requests.");
        }
    }

    private String stateFor(ScenarioNode node) {
        return "END".equals(node.getNodeType()) ? "COMPLETED" : "ACTIVE";
    }

    private OffsetDateTime expiresAt() {
        return OffsetDateTime.now().plusMinutes(SESSION_TTL_MINUTES);
    }

    private int latencyMsSince(long startedAt) {
        return (int) ((System.nanoTime() - startedAt) / 1_000_000);
    }

    private String matchingPayload(Optional<ScenarioMatchingService.MatchResult> match) {
        if (match.isEmpty()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("matched", false);
            payload.put("score", null);
            payload.put("matchType", "NONE");
            payload.put("scenarioId", null);
            return toJson(payload);
        }
        ScenarioMatchingService.MatchResult result = match.get();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matched", true);
        payload.put("score", result.score());
        payload.put("matchType", result.matchType().name());
        payload.put("scenarioId", result.scenarioId());
        return toJson(payload);
    }

    private String toJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private MessageResponse toMessageResponse(ChatMessage message) {
        return new MessageResponse(
                message.getChatMessageNo(),
                message.getSeq(),
                message.getDirection(),
                message.getNodeNo(),
                decryptContent(message),
                message.getFrstRegDt());
    }

    private OptionResponse toOptionResponse(ScenarioNodeOption option) {
        return new OptionResponse(
                option.getScenarioNodeOptionNo(), option.getLabel(), option.getSortOrder());
    }

    private RecommendationResponse toRecommendationResponse(ChatRecommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getChatRecommendationNo(),
                recommendation.getScenarioNo(),
                recommendation.getLabel());
    }

    private HistorySessionResponse toHistorySessionResponse(ChatSessionListItem session) {
        return new HistorySessionResponse(
                UUID.fromString(session.getChatSessionNo()),
                session.getScenarioNo(),
                session.getScenarioTitle(),
                session.getState(),
                session.getStartedAt(),
                session.getLastActivityAt(),
                session.getMessageCount(),
                fieldEncryptionService.decryptOrFallback(
                        session.getLastMessageCiphertext(),
                        session.getLastMessageKeyId(),
                        session.getLastMessage()));
    }

    private void applyEncryptedContent(ChatMessage message, String content) {
        EncryptedField encrypted = fieldEncryptionService.encrypt(content == null ? "" : content);
        message.setContent(ENCRYPTED_PLACEHOLDER);
        message.setContentCiphertext(encrypted.ciphertext());
        message.setContentKeyId(encrypted.keyId());
        message.setContentEncryptionVersion(encrypted.version());
    }

    private void applyEncryptedComment(ChatFeedback feedback, String comment) {
        EncryptedField encrypted = fieldEncryptionService.encryptNullable(comment);
        feedback.setComment(comment == null ? null : ENCRYPTED_PLACEHOLDER);
        feedback.setCommentCiphertext(encrypted.ciphertext());
        feedback.setCommentKeyId(encrypted.keyId());
        feedback.setCommentEncryptionVersion(encrypted.version());
    }

    private String decryptContent(ChatMessage message) {
        return fieldEncryptionService.decryptOrFallback(
                message.getContentCiphertext(), message.getContentKeyId(), message.getContent());
    }

    private String safeFeedbackComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        List<String> piiTypes = piiGuard.detectTypes(trimmed);
        if (piiGuard.hasHighRiskTypes(piiTypes)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Personal information cannot be submitted.");
        }
        return piiTypes.isEmpty() ? trimmed : piiGuard.maskLowRisk(trimmed);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(
                            digest.digest(
                                    (value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record ChatRequestContext(UUID anonymousId, String ipAddress, String userAgent) {}
}
