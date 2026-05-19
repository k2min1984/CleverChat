package kr.co.cleverchat.domain.chatbot.service;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.MessageResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.OptionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.RecommendationResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SessionResponse;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRuntimeService {

    private static final int SESSION_TTL_MINUTES = 30;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFailureRecorder failureRecorder;
    private final ChatRecommendationMapper recommendationMapper;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioVersionMapper versionMapper;
    private final ScenarioNodeMapper nodeMapper;
    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioMatchingService matchingService;
    private final ObjectMapper objectMapper;

    public ChatRuntimeService(
        ChatSessionMapper sessionMapper,
        ChatMessageMapper messageMapper,
        ChatFailureRecorder failureRecorder,
        ChatRecommendationMapper recommendationMapper,
        ScenarioMapper scenarioMapper,
        ScenarioVersionMapper versionMapper,
        ScenarioNodeMapper nodeMapper,
        ScenarioNodeOptionMapper optionMapper,
        ScenarioMatchingService matchingService,
        ObjectMapper objectMapper
    ) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.failureRecorder = failureRecorder;
        this.recommendationMapper = recommendationMapper;
        this.scenarioMapper = scenarioMapper;
        this.versionMapper = versionMapper;
        this.nodeMapper = nodeMapper;
        this.optionMapper = optionMapper;
        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionResponse start(Long scenarioId, ChatRequestContext context) {
        Scenario scenario = Optional.ofNullable(scenarioMapper.findById(scenarioId))
            .filter(value -> "ACTIVE".equals(value.getStatus()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "활성 시나리오를 찾을 수 없습니다."));
        ScenarioVersion version = Optional.ofNullable(versionMapper.findPublishedByScenarioId(scenario.getId()))
            .filter(value -> value.getStartNodeId() != null)
            .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT, "게시된 시작 노드가 없습니다."));
        ScenarioNode startNode = Optional.ofNullable(nodeMapper.findById(version.getStartNodeId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT, "시작 노드를 찾을 수 없습니다."));

        ChatSession session = new ChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setAnonymousId(context.anonymousId().toString());
        session.setScenarioId(scenario.getId());
        session.setVersionId(version.getId());
        session.setCurrentNodeId(startNode.getId());
        session.setState(stateFor(startNode));
        session.setExpiresAt(expiresAt());
        session.setIpHash(hash(context.ipAddress()));
        session.setUserAgentHash(hash(context.userAgent()));
        sessionMapper.insert(session);

        insertBotMessage(session.getId(), 1, startNode, startNode.getContent() == null ? startNode.getTitle() : startNode.getContent());
        return response(session.getId());
    }

    @Transactional(readOnly = true)
    public SessionResponse get(UUID sessionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        ChatSession session = findSession(sid);
        validateOwner(session, context);
        return response(sid);
    }

    @Transactional
    public SessionResponse selectOption(UUID sessionId, Long optionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);

        ScenarioNodeOption option = optionMapper.findEnabledByNodeId(session.getCurrentNodeId()).stream()
            .filter(value -> value.getId().equals(optionId))
            .findFirst()
            .orElseThrow(() -> {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("requestedOptionId", optionId);
                failureRecorder.recordFailure(sid, null, "INVALID_OPTION", detail);
                return new BusinessException(ErrorCode.VALIDATION_ERROR, "선택할 수 없는 옵션입니다.");
            });

        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(sid, seq, session.getCurrentNodeId(), option.getId(), option.getLabel(), "{}", null);
        advance(session, option.getNextNodeId(), seq + 1);
        return response(sid);
    }

    @Transactional
    public SessionResponse freeText(UUID sessionId, String text, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);

        long startedAt = System.nanoTime();
        Optional<ScenarioMatchingService.MatchResult> match = matchingService.match(session.getScenarioId(), session.getCurrentNodeId(), text);
        int latencyMs = latencyMsSince(startedAt);
        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(
            sid,
            seq,
            session.getCurrentNodeId(),
            null,
            text,
            matchingPayload(match),
            null
        );
        if (match.isEmpty()) {
            failureRecorder.recordFailure(sid, null, "NO_MATCH");
            insertBotMessage(sid, seq + 1, null, "질문에 맞는 답변을 찾지 못했습니다.", latencyMs);
            return response(sid);
        }

        ScenarioMatchingService.MatchResult result = match.get();
        if (result.nextNodeId() != null) {
            advance(session, result.nextNodeId(), seq + 1, latencyMs);
            return response(sid);
        }

        Scenario scenario = Optional.ofNullable(scenarioMapper.findById(result.scenarioId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        insertBotMessage(sid, seq + 1, null, scenario.getTitle(), latencyMs);
        return response(sid);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommendations() {
        return recommendationMapper.findEnabledForActiveScenarios().stream()
            .map(this::toRecommendationResponse)
            .toList();
    }

    private void advance(ChatSession session, Long nextNodeId, int botSeq) {
        advance(session, nextNodeId, botSeq, null);
    }

    private void advance(ChatSession session, Long nextNodeId, int botSeq, Integer latencyMs) {
        if (nextNodeId == null) {
            sessionMapper.updateCurrentNode(session.getId(), session.getCurrentNodeId(), "COMPLETED", expiresAt());
            insertBotMessage(session.getId(), botSeq, null, "대화가 완료되었습니다.", latencyMs);
            return;
        }
        ScenarioNode nextNode = Optional.ofNullable(nodeMapper.findById(nextNodeId))
            .filter(node -> session.getVersionId().equals(node.getVersionId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT, "같은 버전의 다음 노드가 아닙니다."));
        sessionMapper.updateCurrentNode(session.getId(), nextNode.getId(), stateFor(nextNode), expiresAt());
        insertBotMessage(session.getId(), botSeq, nextNode, nextNode.getContent() == null ? nextNode.getTitle() : nextNode.getContent(), latencyMs);
    }

    private ChatMessage insertUserMessage(String sessionId, int seq, Long nodeId, Long optionId, String content, String payload, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSeq(seq);
        message.setDirection("USER");
        message.setNodeId(nodeId);
        message.setOptionId(optionId);
        message.setContent(content);
        message.setPayload(payload);
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private ChatMessage insertBotMessage(String sessionId, int seq, ScenarioNode node, String content) {
        return insertBotMessage(sessionId, seq, node, content, null);
    }

    private ChatMessage insertBotMessage(String sessionId, int seq, ScenarioNode node, String content, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSeq(seq);
        message.setDirection("BOT");
        message.setNodeId(node == null ? null : node.getId());
        message.setContent(content == null || content.isBlank() ? "응답 내용이 없습니다." : content);
        message.setPayload("{}");
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private SessionResponse response(String sessionId) {
        ChatSession session = findSession(sessionId);
        List<MessageResponse> messages = messageMapper.findBySessionId(sessionId).stream()
            .map(this::toMessageResponse)
            .toList();
        List<OptionResponse> options = "ACTIVE".equals(session.getState())
            ? optionMapper.findEnabledByNodeId(session.getCurrentNodeId()).stream().map(this::toOptionResponse).toList()
            : List.of();
        return new SessionResponse(
            UUID.fromString(session.getId()),
            session.getScenarioId(),
            session.getVersionId(),
            session.getCurrentNodeId(),
            session.getState(),
            session.getExpiresAt(),
            messages,
            options
        );
    }

    private ChatSession findSession(String sessionId) {
        return sessionMapper.findById(sessionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."));
    }

    private void validateOwnerAndActive(ChatSession session, ChatRequestContext context) {
        validateOwner(session, context);
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            failureRecorder.recordFailure(session.getId(), null, "EXPIRED");
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "진행 가능한 세션이 아닙니다.");
        }
        if (!"ACTIVE".equals(session.getState())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "진행 가능한 세션이 아닙니다.");
        }
    }

    private void validateOwner(ChatSession session, ChatRequestContext context) {
        if (!session.getAnonymousId().equals(context.anonymousId().toString())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
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
            message.getId(),
            message.getSeq(),
            message.getDirection(),
            message.getNodeId(),
            message.getContent(),
            message.getCreatedAt()
        );
    }

    private OptionResponse toOptionResponse(ScenarioNodeOption option) {
        return new OptionResponse(option.getId(), option.getLabel(), option.getSortOrder());
    }

    private RecommendationResponse toRecommendationResponse(ChatRecommendation recommendation) {
        return new RecommendationResponse(recommendation.getId(), recommendation.getScenarioId(), recommendation.getLabel());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record ChatRequestContext(UUID anonymousId, String ipAddress, String userAgent) {
    }
}
