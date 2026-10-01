package kr.co.cleverchat.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.common.security.FieldEncryptionService.EncryptedField;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerContext;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionService;
import kr.co.cleverchat.domain.chatbot.config.ChatSearchProperties;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FeedbackResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.HistorySessionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.MessageLinkResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.MessageResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.OptionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.RecommendationResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.ScenarioSummaryResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SearchOptionResponse;
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
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeLinkMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeLink;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import kr.co.cleverchat.domain.search.service.SearchService;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatRuntimeService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private static final Logger log = LoggerFactory.getLogger(ChatRuntimeService.class);
    private static final int SESSION_TTL_MINUTES = 30;
    private static final String ENCRYPTED_PLACEHOLDER = "[encrypted]";
    private static final String NO_MATCH_MESSAGE =
            "\uC9C8\uBB38\uC5D0 \uB9DE\uB294 \uB2F5\uBCC0\uC744 \uCC3E\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String SEARCH_OPTIONS_PROMPT = "관련 자료를 찾았어요. 아래에서 선택해 주세요.";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatFailureRecorder failureRecorder;
    private final ChatRecommendationMapper recommendationMapper;
    private final ScenarioMapper scenarioMapper;
    private final ScenarioVersionMapper versionMapper;
    private final ScenarioNodeMapper nodeMapper;
    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioNodeLinkMapper linkMapper;
    private final ScenarioMatchingService matchingService;
    private final ChatPiiGuard piiGuard;
    private final ChatRateLimiter rateLimiter;
    private final SearchService searchService;
    private final ChatSearchProperties searchProperties;
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
            ScenarioNodeLinkMapper linkMapper,
            ScenarioMatchingService matchingService,
            ChatPiiGuard piiGuard,
            ChatRateLimiter rateLimiter,
            SearchService searchService,
            ChatSearchProperties searchProperties,
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
        this.linkMapper = linkMapper;
        this.matchingService = matchingService;
        this.piiGuard = piiGuard;
        this.rateLimiter = rateLimiter;
        this.searchService = searchService;
        this.searchProperties = searchProperties;
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
                Optional.ofNullable(
                                versionMapper.findPublishedByScenarioId(scenario.getScenarioNo()))
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
        session.setUserNo(context.userNo());
        session.setScenarioNo(scenario.getScenarioNo());
        session.setVersionNo(version.getScenarioVersionNo());
        session.setCurrentNodeNo(startNode.getScenarioNodeNo());
        session.setSessionType("SCENARIO");
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
    public SessionResponse startWithText(String text, ChatRequestContext context) {
        return startSearch(safeChatText(text), context);
    }

    @Transactional
    public SessionResponse startSearch(String text, ChatRequestContext context) {
        String safeText = safeChatText(text);
        ChatSession session = new ChatSession();
        session.setChatSessionNo(UUID.randomUUID().toString());
        session.setAnonymousId(context.anonymousId().toString());
        session.setUserNo(context.userNo());
        session.setScenarioNo(null);
        session.setVersionNo(null);
        session.setCurrentNodeNo(null);
        session.setSessionType("SEARCH");
        session.setState("ACTIVE");
        session.setExpiresAt(expiresAt());
        session.setIpHash(hash(context.ipAddress()));
        session.setUserAgentHash(hash(context.userAgent()));
        sessionMapper.insert(session);

        insertUserMessage(session.getChatSessionNo(), 1, null, null, safeText, "{}", null);
        return respondWithSearch(session.getChatSessionNo(), 2, safeText, session, context, null);
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
                        anonymousId.toString(),
                        OffsetDateTime.now()
                                .minusDays(
                                        runtimeSettings == null
                                                ? 90
                                                : runtimeSettings
                                                        .current()
                                                        .integer(RuntimeSetting.CHAT_HISTORY_DAYS)),
                        runtimeSettings == null
                                ? 50
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.CHAT_HISTORY_LIMIT))
                .stream()
                .map(this::toHistorySessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistorySessionResponse> userHistory(Long userNo) {
        return sessionMapper
                .findHistoryByUser(
                        userNo,
                        OffsetDateTime.now()
                                .minusDays(
                                        runtimeSettings == null
                                                ? 90
                                                : runtimeSettings
                                                        .current()
                                                        .integer(RuntimeSetting.CHAT_HISTORY_DAYS)),
                        runtimeSettings == null
                                ? 50
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.CHAT_HISTORY_LIMIT))
                .stream()
                .map(this::toHistorySessionResponse)
                .toList();
    }

    @Transactional
    public SessionResponse selectOption(UUID sessionId, Long optionId, ChatRequestContext context) {
        return selectOption(sessionId, optionId, null, context);
    }

    @Transactional
    public SessionResponse selectOption(
            UUID sessionId, Long optionId, Long sourceNodeId, ChatRequestContext context) {
        return selectOption(sessionId, optionId, sourceNodeId, null, context);
    }

    @Transactional
    public SessionResponse selectOption(
            UUID sessionId,
            Long optionId,
            Long sourceNodeId,
            Long sourceMessageId,
            ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        if (sourceNodeId == null) {
            validateOwnerAndActive(session, context);
        } else {
            validateOwner(session, context);
            ensureSelectableHistoryState(session);
        }
        enforceRateLimit(session, context);

        Long optionNodeId = sourceNodeId == null ? session.getCurrentNodeNo() : sourceNodeId;
        boolean nodeWasOffered = sourceNodeId == null;
        if (!nodeWasOffered && optionNodeId != null) {
            List<ChatMessage> sessionMessages = messageMapper.findBySessionId(sid);
            nodeWasOffered =
                    sessionMessages != null
                            && sessionMessages.stream()
                                    .anyMatch(
                                            message ->
                                                    "BOT".equals(message.getDirection())
                                                            && optionNodeId.equals(
                                                                    message.getNodeNo()));
        }
        if (!nodeWasOffered) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택할 수 없는 옵션입니다.");
        }
        if (sourceNodeId != null) {
            ScenarioNode optionNode = nodeMapper.findById(optionNodeId);
            if (optionNode == null || !session.getVersionNo().equals(optionNode.getVersionNo())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "현재 상담에서 선택할 수 없는 옵션입니다.");
            }
        }
        ScenarioNodeOption option =
                optionMapper.findEnabledByNodeId(optionNodeId).stream()
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

        List<ChatMessage> offeredMessages = messageMapper.findBySessionId(sid);
        ChatMessage sourceMessage =
                sourceMessageId == null
                        ? latestNodeMessage(offeredMessages, optionNodeId)
                        : offeredMessages.stream()
                                .filter(
                                        message ->
                                                "BOT".equals(message.getDirection())
                                                        && sourceMessageId.equals(
                                                                message.getChatMessageNo())
                                                        && Objects.equals(
                                                                optionNodeId, message.getNodeNo()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new BusinessException(
                                                        ErrorCode.VALIDATION_ERROR,
                                                        "선택한 메시지에서 제공된 옵션이 아닙니다."));
        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(
                sid,
                seq,
                optionNodeId,
                option.getScenarioNodeOptionNo(),
                option.getLabel(),
                "{}",
                null);
        advance(
                session,
                option.getNextNodeNo(),
                seq + 1,
                navigationPayload(
                        "{}", sourceMessage == null ? null : sourceMessage.getChatMessageNo()));
        return response(sid);
    }

    @Transactional
    public SessionResponse freeText(UUID sessionId, String text, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);
        enforceRateLimit(session, context);
        String safeText = safeChatText(text, sid, session.getCurrentNodeNo());

        if (session.getScenarioNo() == null) {
            int seq = messageMapper.selectNextSeq(sid);
            insertUserMessage(sid, seq, null, null, safeText, "{}", null);
            return respondWithSearch(sid, seq + 1, safeText, session, context, null);
        }

        long startedAt = System.nanoTime();
        Optional<ScenarioMatchingService.MatchResult> match =
                matchingService.match(
                        session.getScenarioNo(), session.getCurrentNodeNo(), safeText);
        int latencyMs = latencyMsSince(startedAt);
        int seq = messageMapper.selectNextSeq(sid);
        insertUserMessage(
                sid, seq, session.getCurrentNodeNo(), null, safeText, matchingPayload(match), null);
        return respondWithSearch(sid, seq + 1, safeText, session, context, latencyMs);
    }

    private SessionResponse respondWithSearch(
            String sid,
            int botSeq,
            String safeText,
            ChatSession session,
            ChatRequestContext context,
            Integer latencyMs) {
        var fallback =
                searchService.search(
                        safeText,
                        "CHAT_FALLBACK",
                        searchProperties.getPoolLimit(),
                        null,
                        hash(context.anonymousId().toString()));
        List<SearchResultItem> results = new ArrayList<>(fallback.results());
        SearchDisplaySelection selection = selectForDisplay(results);
        log.info(
                "chat search display query='{}' pool={} shown={} hidden={}",
                safeText,
                results.size(),
                selection.shown().size(),
                selection.hidden().size());
        if (selection.shown().size() + selection.hidden().size() > 1
                || (selection.shown().size() == 1 && !isCrawlDocument(selection.shown().get(0)))) {
            // Multiple candidates are presented as buttons; click handling reveals snippet/link.
            insertSearchOptionsMessage(
                    sid, botSeq, selection.shown(), selection.hidden(), latencyMs);
            return response(sid);
        }
        if (selection.shown().size() == 1) {
            SearchResultItem top = selection.shown().get(0);
            if ("CRAWL_DOCUMENT".equals(top.getMatchedField())) {
                insertCrawlAnswer(
                        sid,
                        botSeq,
                        top.getCrawlDocumentNo(),
                        top.getScenarioTitle(),
                        top.getSnippet(),
                        top.getCrawlUrl(),
                        latencyMs,
                        safeText);
                return response(sid);
            }
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("inputLength", safeText == null ? 0 : safeText.length());
        detail.put("currentNodeId", session.getCurrentNodeNo());
        detail.put("fallbackSource", "M4_SEARCH");
        detail.put("matchedScenarioIds", List.of());
        failureRecorder.recordFailure(sid, null, "NO_MATCH", detail);
        insertBotMessage(
                sid,
                botSeq,
                null,
                runtimeSettings == null
                        ? NO_MATCH_MESSAGE
                        : runtimeSettings.current().text(RuntimeSetting.CHAT_NO_MATCH),
                latencyMs);
        return response(sid);
    }

    /**
     * Resolves a click on a search-derived option that was offered by a previous free-text
     * fallback. A crawled document is answered with its snippet and a source link; a scenario
     * result switches the current session context to that scenario. Only options actually offered
     * in this session are selectable (guards against picking arbitrary document ids).
     */
    @Transactional
    public SessionResponse selectSearchResult(
            UUID sessionId, Long crawlDocumentNo, Long scenarioNo, ChatRequestContext context) {
        return selectSearchResult(sessionId, crawlDocumentNo, scenarioNo, null, context);
    }

    @Transactional
    public SessionResponse selectSearchResult(
            UUID sessionId,
            Long crawlDocumentNo,
            Long scenarioNo,
            Long scenarioNodeNo,
            ChatRequestContext context) {
        return selectSearchResult(
                sessionId, crawlDocumentNo, scenarioNo, scenarioNodeNo, null, context);
    }

    @Transactional
    public SessionResponse selectSearchResult(
            UUID sessionId,
            Long crawlDocumentNo,
            Long scenarioNo,
            Long scenarioNodeNo,
            Long sourceMessageId,
            ChatRequestContext context) {
        if ((crawlDocumentNo == null && scenarioNo == null)
                || (crawlDocumentNo != null && (scenarioNo != null || scenarioNodeNo != null))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "선택 항목이 없습니다.");
        }
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwner(session, context);
        ensureSelectableHistoryState(session);
        enforceRateLimit(session, context);

        OfferedSearchOption offered =
                findOfferedSearchOption(
                                sid, crawlDocumentNo, scenarioNo, scenarioNodeNo, sourceMessageId)
                        .orElseThrow(
                                () -> {
                                    Map<String, Object> detail = new LinkedHashMap<>();
                                    detail.put("requestedCrawlDocumentNo", crawlDocumentNo);
                                    detail.put("requestedScenarioNo", scenarioNo);
                                    detail.put("requestedScenarioNodeNo", scenarioNodeNo);
                                    detail.put("selectionType", "SEARCH_RESULT");
                                    failureRecorder.recordFailure(
                                            sid, null, "INVALID_OPTION", detail);
                                    return new BusinessException(
                                            ErrorCode.VALIDATION_ERROR, "선택할 수 없는 항목입니다.");
                                });

        JsonNode option = offered.option();
        String selectionPayload = navigationPayload("{}", offered.messageNo());
        String label = textOrNull(option.get("label"));
        int seq = messageMapper.selectNextSeq(sid);
        ChatMessage triggerMessage =
                insertUserMessage(
                        sid,
                        seq,
                        session.getCurrentNodeNo(),
                        null,
                        label == null ? "" : label,
                        "{}",
                        null);

        Long optCrawlNo = longOrNull(option.get("crawlDocumentNo"));
        if (optCrawlNo != null) {
            insertCrawlAnswer(
                    sid,
                    seq + 1,
                    optCrawlNo,
                    label,
                    textOrNull(option.get("snippet")),
                    textOrNull(option.get("url")),
                    null,
                    "선택한 자료의 내용을 설명해 주세요: " + label,
                    selectionPayload);
            return response(sid);
        }
        return switchScenarioInCurrentSession(
                sid,
                session,
                longOrNull(option.get("scenarioNo")),
                longOrNull(option.get("scenarioNodeNo")),
                triggerMessage,
                label,
                selectionPayload);
    }

    @Transactional
    public SessionResponse searchMore(UUID sessionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwnerAndActive(session, context);
        enforceRateLimit(session, context);

        JsonNode overflow = latestBotOverflowOptions(messageMapper.findBySessionId(sid));
        if (overflow == null || overflow.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "추가 검색 결과가 없습니다.");
        }

        List<Map<String, Object>> shown = new ArrayList<>();
        for (JsonNode node : overflow) {
            shown.add(searchOptionNodeToMap(node));
        }

        int seq = messageMapper.selectNextSeq(sid);
        insertSearchOptionsPayloadMessage(sid, seq, shown, List.of(), null);
        return response(sid);
    }

    @Transactional
    public SessionResponse goBack(UUID sessionId, ChatRequestContext context) {
        String sid = sessionId.toString();
        sessionMapper.lockSessionByAdvisoryKey(sid);
        ChatSession session = findSession(sid);
        validateOwner(session, context);
        ensureNotExpired(session);
        if (!"ACTIVE".equals(session.getState()) && !"COMPLETED".equals(session.getState())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "되돌릴 수 없는 세션 상태입니다.");
        }
        enforceRateLimit(session, context);

        List<ChatMessage> messages = messageMapper.findBySessionId(sid);
        ChatMessage latest = latestBotMessage(messages);
        if (hasNavigation(latest)) {
            ChatMessage target =
                    navigationTarget(messages, latest)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.VALIDATION_ERROR, "이전 단계가 없습니다."));
            return restoreNavigationMessage(sid, session, messages, target);
        }
        // Existing conversations may lack the pointer. A document shown over a scenario still
        // returns to its search list, never to the scenario's previous node.
        if (isSearchAnswer(latest)) {
            Optional<SearchPayloadSnapshot> snapshot =
                    latestSearchPayloadBefore(messages, latest, null);
            if (snapshot.isPresent()) {
                return restoreSearchOptions(sid, session, snapshot.get());
            }
        }
        if (session.getScenarioNo() != null && session.getCurrentNodeNo() != null) {
            Optional<Long> previousNodeNo = previousNodeNo(messages, session.getCurrentNodeNo());
            if (previousNodeNo.isPresent()) {
                return goBackToNode(sid, session, previousNodeNo.get());
            }
            Optional<SearchPayloadSnapshot> searchPayload =
                    searchOptionsBeforeLatestScenarioSwitch(sid, session, messages);
            if (searchPayload.isPresent()) {
                return restoreSearchOptions(sid, session, searchPayload.get());
            }
        } else if (session.getScenarioNo() == null) {
            Optional<SearchPayloadSnapshot> searchPayload =
                    previousSearchOptionsBeforeLatest(sid, messages);
            if (searchPayload.isPresent()) {
                return restoreSearchOptions(sid, session, searchPayload.get());
            }
        }

        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이전 단계가 없습니다.");
    }

    private SessionResponse goBackToNode(String sid, ChatSession session, Long previousNodeNo) {
        ScenarioNode previousNode =
                Optional.ofNullable(nodeMapper.findById(previousNodeNo))
                        .filter(node -> session.getVersionNo().equals(node.getVersionNo()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT));

        Long fromNodeNo = session.getCurrentNodeNo();
        sessionMapper.updateCurrentNode(sid, previousNodeNo, stateFor(previousNode), expiresAt());
        int seq = messageMapper.selectNextSeq(sid);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("source", "NODE_BACK");
        detail.put("fromNodeNo", fromNodeNo);
        detail.put("toNodeNo", previousNodeNo);
        List<ChatMessage> messages = messageMapper.findBySessionId(sid);
        ChatMessage target = latestNodeMessage(messages, previousNodeNo);
        Long parentId =
                target == null
                        ? null
                        : (hasNavigation(target)
                                        ? navigationTarget(messages, target)
                                        : legacyParent(messages, target))
                                .map(ChatMessage::getChatMessageNo)
                                .orElse(null);
        ChatMessage backMessage =
                insertBotMessageWithNodePayload(
                        sid,
                        seq,
                        previousNode,
                        "이전 단계로 돌아갑니다.\n\n"
                                + (previousNode.getContent() == null
                                        ? previousNode.getTitle()
                                        : previousNode.getContent()),
                        navigationPayload(toJson(detail), parentId));
        sessionMapper.insertNodeBackEvent(
                sid, fromNodeNo, previousNodeNo, backMessage.getChatMessageNo(), toJson(detail));
        return response(sid);
    }

    private void ensureSelectableHistoryState(ChatSession session) {
        ensureNotExpired(session);
        if (!"ACTIVE".equals(session.getState()) && !"COMPLETED".equals(session.getState())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "선택지를 다시 선택할 수 없는 세션 상태입니다.");
        }
    }

    private SessionResponse restoreSearchOptions(
            String sid, ChatSession session, SearchPayloadSnapshot snapshot) {
        List<ChatMessage> messages = messageMapper.findBySessionId(sid);
        Optional<ChatMessage> original =
                messages.stream()
                        .filter(
                                message ->
                                        snapshot.messageNo() != null
                                                && snapshot.messageNo()
                                                        .equals(message.getChatMessageNo()))
                        .findFirst();
        if (original.isPresent()) {
            return restoreNavigationMessage(sid, session, messages, original.get());
        }
        sessionMapper.restoreSearchSession(sid, expiresAt());
        int seq = messageMapper.selectNextSeq(sid);
        ChatMessage botMessage =
                insertSearchOptionsPayloadMessage(
                        sid, seq, snapshot.searchOptions(), snapshot.overflowOptions(), null);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("source", "SEARCH_BACK");
        detail.put("restoredFromMessageNo", snapshot.messageNo());
        detail.put("searchOptionCount", snapshot.searchOptions().size());
        detail.put("overflowOptionCount", snapshot.overflowOptions().size());
        sessionMapper.insertSearchBackEvent(
                sid, session.getScenarioNo(), botMessage.getChatMessageNo(), toJson(detail));
        return response(sid);
    }

    private SessionResponse switchScenarioInCurrentSession(
            String sid,
            ChatSession session,
            Long scenarioNo,
            Long scenarioNodeNo,
            ChatMessage triggerMessage,
            String selectedLabel,
            String selectionPayload) {
        Scenario scenario =
                Optional.ofNullable(scenarioMapper.findById(scenarioNo))
                        .filter(value -> "ACTIVE".equals(value.getStatus()))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.NOT_FOUND, "활성 시나리오를 찾을 수 없습니다."));
        ScenarioVersion version =
                Optional.ofNullable(
                                versionMapper.findPublishedByScenarioId(scenario.getScenarioNo()))
                        .filter(value -> value.getStartNodeNo() != null)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.STATE_CONFLICT, "게시된 시작 노드가 없습니다."));
        ScenarioNode startNode =
                Optional.ofNullable(
                                nodeMapper.findById(
                                        scenarioNodeNo == null
                                                ? version.getStartNodeNo()
                                                : scenarioNodeNo))
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.STATE_CONFLICT, "시작 노드를 찾을 수 없습니다."));

        if (scenarioNodeNo != null
                && (!version.getScenarioVersionNo().equals(startNode.getVersionNo())
                        || !version.getScenarioVersionNo().equals(scenario.getActiveVersionNo())
                        || !("QUESTION".equals(startNode.getNodeType())
                                || "ANSWER".equals(startNode.getNodeType())))) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "검색 결과가 변경되었습니다. 다시 검색해 주세요.");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("selectedLabel", selectedLabel);
        detail.put("fromSessionType", session.getSessionType());
        Long fromScenarioNo = session.getScenarioNo();
        sessionMapper.updateScenarioContext(
                sid,
                scenario.getScenarioNo(),
                version.getScenarioVersionNo(),
                startNode.getScenarioNodeNo(),
                stateFor(startNode),
                "SCENARIO",
                expiresAt());
        sessionMapper.insertScenarioSwitchEvent(
                sid,
                fromScenarioNo,
                scenario.getScenarioNo(),
                triggerMessage.getChatMessageNo(),
                toJson(detail));
        insertBotMessageWithNodePayload(
                sid,
                triggerMessage.getSeq() + 1,
                startNode,
                startNode.getContent() == null ? startNode.getTitle() : startNode.getContent(),
                selectionPayload);
        return response(sid);
    }

    @Transactional(readOnly = true)
    public List<ScenarioSummaryResponse> activeScenarios() {
        return scenarioMapper.findActiveForMatching().stream()
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

    private void advance(ChatSession session, Long nextNodeId, int botSeq, String payload) {
        if (nextNodeId == null) {
            sessionMapper.updateCurrentNode(
                    session.getChatSessionNo(),
                    session.getCurrentNodeNo(),
                    "COMPLETED",
                    expiresAt());
            insertBotMessageWithNodePayload(
                    session.getChatSessionNo(), botSeq, null, "대화가 완료되었습니다.", payload);
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
                session.getChatSessionNo(),
                nextNode.getScenarioNodeNo(),
                stateFor(nextNode),
                expiresAt());
        insertBotMessageWithNodePayload(
                session.getChatSessionNo(),
                botSeq,
                nextNode,
                nextNode.getContent() == null ? nextNode.getTitle() : nextNode.getContent(),
                payload);
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
        return insertBotMessageWithNodePayload(sessionId, seq, node, content, "{}", latencyMs);
    }

    private ChatMessage insertBotMessageWithNodePayload(
            String sessionId, int seq, ScenarioNode node, String content, String payload) {
        return insertBotMessageWithNodePayload(sessionId, seq, node, content, payload, null);
    }

    private ChatMessage insertBotMessageWithNodePayload(
            String sessionId,
            int seq,
            ScenarioNode node,
            String content,
            String payload,
            Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionNo(sessionId);
        message.setSeq(seq);
        message.setDirection("BOT");
        message.setNodeNo(node == null ? null : node.getScenarioNodeNo());
        applyEncryptedContent(
                message, content == null || content.isBlank() ? "응답 내용이 없습니다." : content);
        message.setPayload(attachNavigation(sessionId, payload));
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private SessionResponse response(String sessionId) {
        ChatSession session = findSession(sessionId);
        List<ChatMessage> rawMessages = messageMapper.findBySessionId(sessionId);
        List<MessageResponse> messages =
                rawMessages.stream().map(message -> toMessageResponse(message, session)).toList();
        List<SearchOptionResponse> searchOptions = extractSearchOptions(rawMessages, session);
        int searchMoreCount = searchMoreCount(rawMessages, session);
        boolean canGoBack = canGoBack(rawMessages, session);
        boolean hideNodeOptions = shouldHideNodeOptions(rawMessages, searchOptions);
        List<OptionResponse> options =
                !hideNodeOptions
                                && "ACTIVE".equals(session.getState())
                                && session.getCurrentNodeNo() != null
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
                options,
                searchOptions,
                searchMoreCount,
                canGoBack,
                navigationTarget(rawMessages, latestBotMessage(rawMessages))
                        .map(this::backTargetType)
                        .orElse(null));
    }

    /**
     * Search-derived buttons live in the payload of the most recent bot message; they are surfaced
     * only while the session is active and disappear once any newer bot message is appended.
     */
    private List<SearchOptionResponse> extractSearchOptions(
            List<ChatMessage> messages, ChatSession session) {
        if (!"ACTIVE".equals(session.getState())) {
            return List.of();
        }
        JsonNode array = latestBotSearchOptions(messages);
        if (array == null) {
            return List.of();
        }
        List<SearchOptionResponse> options = new ArrayList<>();
        for (JsonNode node : array) {
            options.add(
                    new SearchOptionResponse(
                            longOrNull(node.get("crawlDocumentNo")),
                            longOrNull(node.get("scenarioNo")),
                            longOrNull(node.get("scenarioNodeNo")),
                            textOrNull(node.get("label")),
                            textOrNull(node.get("matchedField")),
                            optionType(textOrNull(node.get("matchedField")))));
        }
        return options;
    }

    private int searchMoreCount(List<ChatMessage> messages, ChatSession session) {
        if (!"ACTIVE".equals(session.getState())) {
            return 0;
        }
        JsonNode array = latestBotOverflowOptions(messages);
        return array == null || !searchProperties.isMoreEnabled() ? 0 : array.size();
    }

    private ObjectNode payloadNode(String payload) {
        if (payload != null && !payload.isBlank()) {
            try {
                JsonNode parsed = objectMapper.readTree(payload);
                if (parsed instanceof ObjectNode object) {
                    return object;
                }
            } catch (JsonProcessingException ignored) {
                // Older messages may not have structured payloads.
            }
        }
        return objectMapper.createObjectNode();
    }

    private boolean hasNavigation(ChatMessage message) {
        return message != null && payloadNode(message.getPayload()).path("navigation").isObject();
    }

    private boolean isSearchAnswer(ChatMessage message) {
        return message != null
                && "SEARCH_RESULT"
                        .equals(payloadNode(message.getPayload()).path("source").asText());
    }

    private String navigationPayload(String payload, Long parentMessageNo) {
        ObjectNode result = payloadNode(payload);
        ObjectNode navigation = result.putObject("navigation");
        if (parentMessageNo == null) {
            navigation.putNull("parentMessageNo");
        } else {
            navigation.put("parentMessageNo", parentMessageNo);
        }
        return result.toString();
    }

    private String attachNavigation(String sessionId, String payload) {
        if (payloadNode(payload).path("navigation").isObject()) {
            return payload;
        }
        ChatMessage parent = latestBotMessage(messageMapper.findBySessionId(sessionId));
        return navigationPayload(payload, parent == null ? null : parent.getChatMessageNo());
    }

    private Optional<ChatMessage> navigationTarget(
            List<ChatMessage> messages, ChatMessage current) {
        if (!hasNavigation(current)) {
            return Optional.empty();
        }
        Long parentId =
                longOrNull(
                        payloadNode(current.getPayload())
                                .path("navigation")
                                .get("parentMessageNo"));
        // Resolve only an earlier bot message owned by this session. The browser never supplies
        // a return target, and null explicitly means the beginning of this navigation branch.
        return messages.stream()
                .filter(
                        message ->
                                "BOT".equals(message.getDirection())
                                        && parentId != null
                                        && parentId.equals(message.getChatMessageNo())
                                        && message.getSeq() < current.getSeq())
                .findFirst();
    }

    private ChatMessage latestNodeMessage(List<ChatMessage> messages, Long nodeId) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if ("BOT".equals(message.getDirection())
                    && nodeId != null
                    && nodeId.equals(message.getNodeNo())) {
                return message;
            }
        }
        return null;
    }

    private String backTargetType(ChatMessage message) {
        if (searchOptions(message) != null) {
            return "SEARCH_RESULTS";
        }
        return message.getNodeNo() == null ? "ANSWER" : "SCENARIO";
    }

    private Optional<ChatMessage> legacyParent(List<ChatMessage> messages, ChatMessage current) {
        List<ChatMessage> earlier =
                messages.stream().filter(message -> message.getSeq() < current.getSeq()).toList();
        if (nodeBackTarget(current.getPayload()) != null) {
            List<ChatMessage> throughCurrent = new ArrayList<>(earlier);
            throughCurrent.add(current);
            return previousNodeNo(throughCurrent, current.getNodeNo())
                    .map(id -> latestNodeMessage(earlier, id));
        }
        if (searchOptions(current) != null) {
            Long restoredFrom =
                    sessionMapper.findSearchBackRestoredFromMessageNo(
                            current.getSessionNo(), current.getChatMessageNo());
            if (restoredFrom != null) {
                return earlier.stream()
                        .filter(message -> restoredFrom.equals(message.getChatMessageNo()))
                        .findFirst()
                        .flatMap(
                                message ->
                                        hasNavigation(message)
                                                ? navigationTarget(earlier, message)
                                                : legacyParent(earlier, message));
            }
        }
        return Optional.ofNullable(latestBotMessage(earlier));
    }

    private SessionResponse restoreNavigationMessage(
            String sid, ChatSession session, List<ChatMessage> messages, ChatMessage target) {
        String payload = target.getPayload();
        if (!hasNavigation(target)) {
            payload =
                    navigationPayload(
                            payload,
                            legacyParent(messages, target)
                                    .map(ChatMessage::getChatMessageNo)
                                    .orElse(null));
        }
        ScenarioNode node = null;
        if (target.getNodeNo() != null) {
            node =
                    Optional.ofNullable(nodeMapper.findById(target.getNodeNo()))
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.STATE_CONFLICT,
                                                    "이전 상담 단계가 변경되어 돌아갈 수 없습니다."));
            ScenarioVersion version =
                    Optional.ofNullable(versionMapper.findById(node.getVersionNo()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT));
            sessionMapper.updateScenarioContext(
                    sid,
                    version.getScenarioNo(),
                    node.getVersionNo(),
                    node.getScenarioNodeNo(),
                    stateFor(node),
                    "SCENARIO",
                    expiresAt());
        } else {
            sessionMapper.restoreSearchSession(sid, expiresAt());
        }
        ChatMessage restored =
                insertBotMessageWithNodePayload(
                        sid,
                        messageMapper.selectNextSeq(sid),
                        node,
                        decryptContent(target),
                        payload);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("restoredFromMessageNo", target.getChatMessageNo());
        detail.put("fromNodeNo", session.getCurrentNodeNo());
        detail.put("toNodeNo", target.getNodeNo());
        if (node == null) {
            sessionMapper.insertSearchBackEvent(
                    sid, session.getScenarioNo(), restored.getChatMessageNo(), toJson(detail));
        } else {
            sessionMapper.insertNodeBackEvent(
                    sid,
                    session.getCurrentNodeNo(),
                    target.getNodeNo(),
                    restored.getChatMessageNo(),
                    toJson(detail));
        }
        return response(sid);
    }

    private boolean canGoBack(List<ChatMessage> messages, ChatSession session) {
        if (!"ACTIVE".equals(session.getState()) && !"COMPLETED".equals(session.getState())) {
            return false;
        }
        ChatMessage latest = latestBotMessage(messages);
        if (hasNavigation(latest)) {
            return navigationTarget(messages, latest).isPresent();
        }
        if (isSearchAnswer(latest)
                && latestSearchPayloadBefore(messages, latest, null).isPresent()) {
            return true;
        }
        if (session.getScenarioNo() == null || session.getCurrentNodeNo() == null) {
            return previousSearchOptionsBeforeLatest(session.getChatSessionNo(), messages)
                    .isPresent();
        }
        return previousNodeNo(messages, session.getCurrentNodeNo()).isPresent()
                || searchOptionsBeforeLatestScenarioSwitch(
                                session.getChatSessionNo(), session, messages)
                        .isPresent();
    }

    private boolean shouldHideNodeOptions(
            List<ChatMessage> messages, List<SearchOptionResponse> searchOptions) {
        if (!searchOptions.isEmpty()) {
            return true;
        }
        ChatMessage latestBot = latestBotMessage(messages);
        if (latestBot == null
                || latestBot.getPayload() == null
                || latestBot.getPayload().isBlank()) {
            return false;
        }
        try {
            JsonNode payload = objectMapper.readTree(latestBot.getPayload());
            return payload.path("hideNodeOptions").asBoolean(false)
                    || "SEARCH_RESULT".equals(textOrNull(payload.get("source")));
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private record OfferedSearchOption(Long messageNo, JsonNode option) {}

    private Optional<OfferedSearchOption> findOfferedSearchOption(
            String sessionId,
            Long crawlDocumentNo,
            Long scenarioNo,
            Long scenarioNodeNo,
            Long sourceMessageId) {
        List<ChatMessage> messages = messageMapper.findBySessionId(sessionId);
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (sourceMessageId != null
                    && !sourceMessageId.equals(messages.get(index).getChatMessageNo())) {
                continue;
            }
            JsonNode array = searchOptions(messages.get(index));
            if (array == null) {
                continue;
            }
            for (JsonNode node : array) {
                if (crawlDocumentNo != null
                        && crawlDocumentNo.equals(longOrNull(node.get("crawlDocumentNo")))) {
                    return Optional.of(
                            new OfferedSearchOption(messages.get(index).getChatMessageNo(), node));
                }
                if (crawlDocumentNo == null
                        && scenarioNo != null
                        && scenarioNo.equals(longOrNull(node.get("scenarioNo")))
                        && java.util.Objects.equals(
                                scenarioNodeNo, longOrNull(node.get("scenarioNodeNo")))) {
                    return Optional.of(
                            new OfferedSearchOption(messages.get(index).getChatMessageNo(), node));
                }
            }
        }
        return Optional.empty();
    }

    private JsonNode latestBotSearchOptions(List<ChatMessage> messages) {
        ChatMessage latestBot = latestBotMessage(messages);
        return searchOptions(latestBot);
    }

    private JsonNode searchOptions(ChatMessage message) {
        if (message == null
                || !"BOT".equals(message.getDirection())
                || message.getPayload() == null) {
            return null;
        }
        try {
            JsonNode array = objectMapper.readTree(message.getPayload()).get("searchOptions");
            return array != null && array.isArray() ? array : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private JsonNode latestBotOverflowOptions(List<ChatMessage> messages) {
        ChatMessage latestBot = latestBotMessage(messages);
        if (latestBot == null || latestBot.getPayload() == null) {
            return null;
        }
        try {
            JsonNode array = objectMapper.readTree(latestBot.getPayload()).get("overflowOptions");
            return array != null && array.isArray() ? array : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Optional<SearchPayloadSnapshot> previousSearchOptionsBeforeLatest(
            String sessionId, List<ChatMessage> messages) {
        ChatMessage latestBot = latestBotMessage(messages);
        if (latestBot == null) {
            return Optional.empty();
        }
        Optional<SearchPayloadSnapshot> latestSnapshot = searchPayloadSnapshot(latestBot);
        if (latestSnapshot.isEmpty()) {
            return latestSearchPayloadBefore(messages, latestBot, null);
        }

        SearchPayloadSnapshot currentSnapshot = latestSnapshot.get();
        Long currentMessageNo = latestBot.getChatMessageNo();
        Long restoredFromMessageNo = null;
        if (currentMessageNo != null) {
            Long candidate =
                    sessionMapper.findSearchBackRestoredFromMessageNo(sessionId, currentMessageNo);
            if (candidate != null && candidate > 0) {
                restoredFromMessageNo = candidate;
            }
        }
        if (restoredFromMessageNo != null) {
            for (ChatMessage message : messages) {
                if (restoredFromMessageNo.equals(message.getChatMessageNo())) {
                    Optional<SearchPayloadSnapshot> restoredSnapshot =
                            searchPayloadSnapshot(message);
                    if (restoredSnapshot.isPresent()) {
                        currentSnapshot = restoredSnapshot.get();
                        currentMessageNo = restoredFromMessageNo;
                    }
                    break;
                }
            }
        }
        return paginationParent(messages, latestBot, currentMessageNo, currentSnapshot);
    }

    private Optional<SearchPayloadSnapshot> latestSearchPayloadBefore(
            List<ChatMessage> messages, ChatMessage latestBot, Long beforeMessageNo) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if (!"BOT".equals(message.getDirection()) || message == latestBot) {
                continue;
            }
            if (beforeMessageNo != null
                    && (message.getChatMessageNo() == null
                            || message.getChatMessageNo() >= beforeMessageNo)) {
                continue;
            }
            Optional<SearchPayloadSnapshot> snapshot = searchPayloadSnapshot(message);
            if (snapshot.isPresent()) {
                return snapshot;
            }
        }
        return Optional.empty();
    }

    private Optional<SearchPayloadSnapshot> paginationParent(
            List<ChatMessage> messages,
            ChatMessage latestBot,
            Long currentMessageNo,
            SearchPayloadSnapshot currentSnapshot) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if (!"BOT".equals(message.getDirection()) || message == latestBot) {
                continue;
            }
            if (currentMessageNo != null
                    && (message.getChatMessageNo() == null
                            || message.getChatMessageNo() >= currentMessageNo)) {
                continue;
            }
            Optional<SearchPayloadSnapshot> candidate = searchPayloadSnapshot(message);
            if (candidate.isPresent() && isPaginationParent(candidate.get(), currentSnapshot)) {
                return candidate;
            }
        }
        return Optional.empty();
    }

    private boolean isPaginationParent(
            SearchPayloadSnapshot candidate, SearchPayloadSnapshot current) {
        List<Map<String, Object>> expected = new ArrayList<>(current.searchOptions());
        expected.addAll(current.overflowOptions());
        if (candidate.overflowOptions().size() != expected.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            if (!sameSearchOption(candidate.overflowOptions().get(index), expected.get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean sameSearchOption(Map<String, Object> left, Map<String, Object> right) {
        return Objects.equals(left.get("crawlDocumentNo"), right.get("crawlDocumentNo"))
                && Objects.equals(left.get("scenarioNo"), right.get("scenarioNo"))
                && Objects.equals(left.get("scenarioNodeNo"), right.get("scenarioNodeNo"));
    }

    private Optional<SearchPayloadSnapshot> searchOptionsBeforeLatestScenarioSwitch(
            String sessionId, ChatSession session, List<ChatMessage> messages) {
        Long triggerMessageNo =
                sessionMapper.findLatestScenarioSwitchTriggerMessageNo(
                        sessionId, session.getScenarioNo());
        if (triggerMessageNo == null) {
            return Optional.empty();
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if (!"BOT".equals(message.getDirection())
                    || message.getChatMessageNo() == null
                    || message.getChatMessageNo() >= triggerMessageNo) {
                continue;
            }
            Optional<SearchPayloadSnapshot> snapshot = searchPayloadSnapshot(message);
            if (snapshot.isPresent()) {
                return snapshot;
            }
        }
        return Optional.empty();
    }

    private Optional<SearchPayloadSnapshot> searchPayloadSnapshot(ChatMessage message) {
        if (message.getPayload() == null || message.getPayload().isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            JsonNode searchOptions = payload.get("searchOptions");
            if (searchOptions == null || !searchOptions.isArray() || searchOptions.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(
                    new SearchPayloadSnapshot(
                            message.getChatMessageNo(),
                            searchOptionArrayToMaps(searchOptions),
                            searchOptionArrayToMaps(payload.get("overflowOptions"))));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private List<Map<String, Object>> searchOptionArrayToMaps(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> options = new ArrayList<>();
        for (JsonNode node : array) {
            options.add(searchOptionNodeToMap(node));
        }
        return options;
    }

    private ChatMessage latestBotMessage(List<ChatMessage> messages) {
        ChatMessage latestBot = null;
        for (ChatMessage message : messages) {
            if ("BOT".equals(message.getDirection())) {
                latestBot = message;
            }
        }
        return latestBot;
    }

    private Optional<Long> previousNodeNo(List<ChatMessage> messages, Long currentNodeNo) {
        List<Long> nodeStack = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (!"BOT".equals(message.getDirection()) || message.getNodeNo() == null) {
                continue;
            }
            Long backTarget = nodeBackTarget(message.getPayload());
            if (backTarget != null) {
                while (!nodeStack.isEmpty()
                        && !backTarget.equals(nodeStack.get(nodeStack.size() - 1))) {
                    nodeStack.remove(nodeStack.size() - 1);
                }
                if (nodeStack.isEmpty()) {
                    nodeStack.add(message.getNodeNo());
                }
                continue;
            }
            if (nodeStack.isEmpty()
                    || !message.getNodeNo().equals(nodeStack.get(nodeStack.size() - 1))) {
                nodeStack.add(message.getNodeNo());
            }
        }
        if (nodeStack.isEmpty() || !currentNodeNo.equals(nodeStack.get(nodeStack.size() - 1))) {
            return Optional.empty();
        }
        if (nodeStack.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(nodeStack.get(nodeStack.size() - 2));
    }

    private Long nodeBackTarget(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (!"NODE_BACK".equals(textOrNull(node.get("source")))) {
                return null;
            }
            return longOrNull(node.get("toNodeNo"));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void insertSearchOptionsMessage(
            String sessionId,
            int seq,
            List<SearchResultItem> shown,
            List<SearchResultItem> hidden,
            Integer latencyMs) {
        insertSearchOptionsPayloadMessage(
                sessionId,
                seq,
                shown.stream().map(this::searchOptionToMap).toList(),
                hidden.stream().map(this::searchOptionToMap).toList(),
                latencyMs);
    }

    private ChatMessage insertSearchOptionsPayloadMessage(
            String sessionId,
            int seq,
            List<Map<String, Object>> options,
            List<Map<String, Object>> overflowOptions,
            Integer latencyMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("searchOptions", options);
        payload.put("overflowOptions", overflowOptions);
        if (searchProperties.isMoreEnabled() && !overflowOptions.isEmpty()) {
            payload.put("more", Map.of("available", overflowOptions.size()));
        }
        return insertBotMessageWithPayload(
                sessionId,
                seq,
                runtimeSettings == null
                        ? SEARCH_OPTIONS_PROMPT
                        : runtimeSettings.current().text(RuntimeSetting.CHAT_SEARCH_PROMPT),
                toJson(payload),
                latencyMs);
    }

    private Map<String, Object> searchOptionToMap(SearchResultItem item) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("crawlDocumentNo", item.getCrawlDocumentNo());
        option.put("scenarioNo", item.getScenarioNo());
        option.put("scenarioNodeNo", item.getScenarioNodeNo());
        option.put("label", searchOptionLabel(item));
        option.put("matchedField", item.getMatchedField());
        option.put("optionType", optionType(item.getMatchedField()));
        option.put("snippet", snippetText(item.getSnippet()));
        option.put("url", crawlDocumentLink(item.getCrawlUrl()));
        return option;
    }

    private Map<String, Object> searchOptionNodeToMap(JsonNode node) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("crawlDocumentNo", longOrNull(node.get("crawlDocumentNo")));
        option.put("scenarioNo", longOrNull(node.get("scenarioNo")));
        option.put("scenarioNodeNo", longOrNull(node.get("scenarioNodeNo")));
        option.put("label", textOrNull(node.get("label")));
        option.put("matchedField", textOrNull(node.get("matchedField")));
        option.put("optionType", optionType(textOrNull(node.get("matchedField"))));
        option.put("snippet", textOrNull(node.get("snippet")));
        option.put("url", textOrNull(node.get("url")));
        return option;
    }

    SearchDisplaySelection selectForDisplay(List<SearchResultItem> results) {
        if (results == null || results.isEmpty()) {
            return new SearchDisplaySelection(List.of(), List.of());
        }
        List<SearchResultItem> eligible =
                results.stream()
                        .filter(item -> item.getScore() >= searchProperties.getMinScore())
                        .toList();
        eligible =
                deduplicateDisplayResults(
                        eligible.stream()
                                .sorted(
                                        java.util.Comparator.comparingDouble(
                                                        SearchResultItem::getScore)
                                                .reversed())
                                .toList());
        List<SearchResultItem> relevant = new ArrayList<>();
        addRelativeMatches(
                relevant, eligible.stream().filter(item -> !isCrawlDocument(item)).toList());
        addRelativeMatches(relevant, eligible.stream().filter(this::isCrawlDocument).toList());
        eligible = List.copyOf(relevant);

        List<SearchResultItem> relativeMatches = new ArrayList<>();
        relativeMatches.addAll(
                selectDisplayGroup(
                        eligible.stream().filter(item -> !isCrawlDocument(item)).toList(), false));
        relativeMatches.addAll(
                selectDisplayGroup(eligible.stream().filter(this::isCrawlDocument).toList(), true));
        // The mapper already returns relevance-score order. Do not force every scenario ahead of
        // a more relevant source document (for example, an exact document-title match).
        relativeMatches.sort(
                java.util.Comparator.comparingDouble(SearchResultItem::getScore).reversed());

        List<SearchResultItem> shown = new ArrayList<>(relativeMatches);
        List<SearchResultItem> hidden = new ArrayList<>();
        for (SearchResultItem item : reorderSearchOptions(eligible)) {
            if (!shown.contains(item)) {
                hidden.add(item);
            }
        }
        return new SearchDisplaySelection(List.copyOf(shown), List.copyOf(hidden));
    }

    private List<SearchResultItem> selectDisplayGroup(
            List<SearchResultItem> group, boolean crawlDocuments) {
        int limit = searchProperties.getDisplayMax();
        if (crawlDocuments) {
            limit = Math.min(limit, searchProperties.getMaxCrawlDocuments());
        }
        return List.copyOf(group.subList(0, Math.min(limit, group.size())));
    }

    private List<SearchResultItem> deduplicateDisplayResults(List<SearchResultItem> results) {
        Map<String, SearchResultItem> distinct = new LinkedHashMap<>();
        for (SearchResultItem item : results) {
            String labelKey = normalizeMatchText(searchOptionLabel(item));
            String fallbackKey =
                    item.getCrawlDocumentNo() != null
                            ? "document-id:" + item.getCrawlDocumentNo()
                            : "scenario-id:" + item.getScenarioNo();
            String key =
                    optionType(item.getMatchedField())
                            + ":"
                            + (labelKey.isBlank() ? fallbackKey : labelKey);
            distinct.putIfAbsent(key, item);
        }
        return List.copyOf(distinct.values());
    }

    private void addRelativeMatches(List<SearchResultItem> target, List<SearchResultItem> group) {
        if (group.isEmpty()) {
            return;
        }
        double topScore = group.get(0).getScore();
        double threshold = topScore * searchProperties.getRelativeThreshold();
        group.stream()
                .filter(item -> topScore <= 0.0 || item.getScore() >= threshold)
                .forEach(target::add);
    }

    private List<SearchResultItem> reorderSearchOptions(List<SearchResultItem> items) {
        return List.copyOf(items);
    }

    private boolean isCrawlDocument(SearchResultItem item) {
        return item != null && "CRAWL_DOCUMENT".equals(item.getMatchedField());
    }

    private String optionType(String matchedField) {
        return "CRAWL_DOCUMENT".equals(matchedField) ? "DOCUMENT" : "SCENARIO";
    }

    record SearchDisplaySelection(List<SearchResultItem> shown, List<SearchResultItem> hidden) {}

    private void insertCrawlAnswer(
            String sessionId,
            int seq,
            Long crawlDocumentNo,
            String label,
            String snippet,
            String url,
            Integer latencyMs,
            String query) {
        insertCrawlAnswer(
                sessionId, seq, crawlDocumentNo, label, snippet, url, latencyMs, query, null);
    }

    private void insertCrawlAnswer(
            String sessionId,
            int seq,
            Long crawlDocumentNo,
            String label,
            String snippet,
            String url,
            Integer latencyMs,
            String query,
            String selectionPayload) {
        String body = snippetText(snippet);
        if (body == null || body.isBlank()) {
            body = label;
        }
        SearchResultItem evidence = new SearchResultItem();
        evidence.setMatchedField("CRAWL_DOCUMENT");
        evidence.setCrawlDocumentNo(crawlDocumentNo);
        evidence.setScenarioTitle(label);
        evidence.setCrawlUrl(url);
        evidence.setSnippet(snippet);
        body =
                aiAnswerSuggestionService
                        .suggest(
                                query,
                                List.of(evidence),
                                new AiAnswerContext(null, null, "CRAWL_DOCUMENT"))
                        .map(answer -> answer.answer())
                        .orElse(body);
        insertBotMessageWithPayload(
                sessionId,
                seq,
                body,
                selectionPayload == null
                        ? crawlAnswerPayload(crawlDocumentNo, url)
                        : navigationPayload(
                                crawlAnswerPayload(crawlDocumentNo, url),
                                longOrNull(
                                        payloadNode(selectionPayload)
                                                .path("navigation")
                                                .get("parentMessageNo"))),
                latencyMs);
    }

    private String crawlAnswerPayload(Long crawlDocumentNo, String url) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "SEARCH_RESULT");
        payload.put("hideNodeOptions", true);
        String linkUrl = crawlDocumentLink(url);
        if (linkUrl == null || linkUrl.isBlank()) {
            return toJson(payload);
        }
        Map<String, Object> link = new LinkedHashMap<>();
        link.put("label", "해당 페이지 이동");
        link.put("url", linkUrl);
        link.put("linkType", "EXTERNAL");
        link.put("sortOrder", 0);
        payload.put("links", List.of(link));
        return toJson(payload);
    }

    private String crawlDocumentLink(String sourceUrl) {
        return sourceUrl;
    }

    private ChatMessage insertBotMessageWithPayload(
            String sessionId, int seq, String content, String payload, Integer latencyMs) {
        ChatMessage message = new ChatMessage();
        message.setSessionNo(sessionId);
        message.setSeq(seq);
        message.setDirection("BOT");
        message.setNodeNo(null);
        applyEncryptedContent(
                message, content == null || content.isBlank() ? "응답 내용이 없습니다." : content);
        message.setPayload(attachNavigation(sessionId, payload));
        message.setLatencyMs(latencyMs);
        messageMapper.insert(message);
        return message;
    }

    private String snippetText(String snippet) {
        return snippet == null ? null : snippet.trim();
    }

    private String searchOptionLabel(SearchResultItem item) {
        String title =
                isCrawlDocument(item)
                        ? normalizeCrawlLabel(item.getScenarioTitle())
                        : compactText(item.getScenarioTitle());
        if (title == null || title.isBlank()) {
            title =
                    item.getCrawlDocumentNo() == null
                            ? "검색 결과"
                            : "크롤 문서 #" + item.getCrawlDocumentNo();
        }
        return abbreviate(title, 72);
    }

    String normalizeCrawlLabel(String rawTitle) {
        String compact = compactText(rawTitle);
        if (compact == null || compact.isBlank()) {
            return compact;
        }
        List<String> parts = new ArrayList<>(List.of(compact));
        for (String delimiter : searchProperties.getLabel().getDelimiters()) {
            if (delimiter == null || delimiter.isEmpty()) {
                continue;
            }
            List<String> next = new ArrayList<>();
            for (String part : parts) {
                for (String token : part.split(Pattern.quote(delimiter))) {
                    String trimmed = compactText(token);
                    if (trimmed != null && !trimmed.isBlank()) {
                        next.add(trimmed);
                    }
                }
            }
            if (!next.isEmpty()) {
                parts = next;
            }
        }
        List<String> candidates =
                parts.stream().filter(part -> !isConfiguredSiteSuffix(part)).toList();
        if (candidates.isEmpty()) {
            return compact;
        }
        return searchProperties.getLabel().getPrefer() == ChatSearchProperties.Prefer.LAST
                ? candidates.get(candidates.size() - 1)
                : candidates.get(0);
    }

    private boolean isConfiguredSiteSuffix(String value) {
        String normalized = normalizeMatchText(value);
        return searchProperties.getLabel().getSiteSuffixes().stream()
                .map(this::normalizeMatchText)
                .anyMatch(suffix -> !suffix.isBlank() && suffix.equals(normalized));
    }

    private String compactText(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
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
        boolean owned =
                context.userNo() != null
                        ? context.userNo().equals(session.getUserNo())
                        : session.getUserNo() == null
                                && session.getAnonymousId()
                                        .equals(context.anonymousId().toString());
        if (!owned) {
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
        return OffsetDateTime.now()
                .plusMinutes(
                        runtimeSettings == null
                                ? SESSION_TTL_MINUTES
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.CHAT_TTL_MINUTES));
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

    private MessageResponse toMessageResponse(ChatMessage message, ChatSession session) {
        ScenarioNode messageNode =
                message.getNodeNo() == null ? null : nodeMapper.findById(message.getNodeNo());
        List<OptionResponse> messageOptions =
                "BOT".equals(message.getDirection())
                                && messageNode != null
                                && session.getVersionNo() != null
                                && session.getVersionNo().equals(messageNode.getVersionNo())
                        ? optionMapper.findEnabledByNodeId(message.getNodeNo()).stream()
                                .map(this::toOptionResponse)
                                .toList()
                        : List.of();
        return new MessageResponse(
                message.getChatMessageNo(),
                message.getSeq(),
                message.getDirection(),
                message.getNodeNo(),
                decryptContent(message),
                message.getFrstRegDt(),
                messageLinks(message),
                messageOptions,
                toSearchOptionResponses(searchOptions(message)));
    }

    private List<SearchOptionResponse> toSearchOptionResponses(JsonNode array) {
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<SearchOptionResponse> options = new ArrayList<>();
        for (JsonNode node : array) {
            options.add(
                    new SearchOptionResponse(
                            longOrNull(node.get("crawlDocumentNo")),
                            longOrNull(node.get("scenarioNo")),
                            longOrNull(node.get("scenarioNodeNo")),
                            textOrNull(node.get("label")),
                            textOrNull(node.get("matchedField")),
                            optionType(textOrNull(node.get("matchedField")))));
        }
        return options;
    }

    private List<MessageLinkResponse> messageLinks(ChatMessage message) {
        if (!"BOT".equals(message.getDirection())) {
            return List.of();
        }
        if (message.getNodeNo() == null) {
            // Node-less bot messages (e.g. crawled-document answers) carry links in their payload.
            return payloadLinks(message.getPayload());
        }
        List<ScenarioNodeLink> links = linkMapper.findEnabledByNodeId(message.getNodeNo());
        return (links == null ? List.<ScenarioNodeLink>of() : links)
                .stream().map(this::toMessageLinkResponse).toList();
    }

    private List<MessageLinkResponse> payloadLinks(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(payload).get("links");
            if (array == null || !array.isArray()) {
                return List.of();
            }
            List<MessageLinkResponse> links = new ArrayList<>();
            int index = 0;
            for (JsonNode node : array) {
                links.add(
                        new MessageLinkResponse(
                                null,
                                textOrNull(node.get("label")),
                                textOrNull(node.get("url")),
                                textOrNull(node.get("linkType")),
                                node.has("sortOrder") ? node.get("sortOrder").asInt() : index));
                index++;
            }
            return links;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private MessageLinkResponse toMessageLinkResponse(ScenarioNodeLink link) {
        return new MessageLinkResponse(
                link.getScenarioNodeLinkNo(),
                link.getLabel(),
                link.getUrl(),
                link.getLinkType(),
                link.getSortOrder());
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
                session.getScenarioTitle() == null ? "\uAC80\uC0C9" : session.getScenarioTitle(),
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

    private String safeChatText(String text) {
        return safeChatText(text, null, null);
    }

    private String safeChatText(String text, String sessionId, Long currentNodeId) {
        List<String> piiTypes = piiGuard.detectTypes(text);
        if (piiGuard.hasHighRiskTypes(piiTypes)) {
            if (sessionId != null) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("piiTypes", piiTypes);
                detail.put("inputLength", text == null ? 0 : text.length());
                detail.put("currentNodeId", currentNodeId);
                failureRecorder.recordFailure(sessionId, null, "PII_BLOCKED", detail);
            }
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Personal information cannot be submitted.");
        }
        return piiTypes.isEmpty() ? text : piiGuard.maskLowRisk(text);
    }

    private Optional<Long> matchScenarioTitle(String text) {
        String input = normalizeMatchText(text);
        if (input.isBlank()) {
            return Optional.empty();
        }
        return scenarioMapper.findActiveForMatching().stream()
                .filter(
                        scenario -> {
                            String title = normalizeMatchText(scenario.getTitle());
                            return !title.isBlank()
                                    && (title.contains(input) || input.contains(title));
                        })
                .max(
                        Comparator.comparingInt(
                                        (Scenario scenario) ->
                                                normalizeMatchText(scenario.getTitle()).length())
                                .thenComparing(Scenario::getScenarioNo))
                .map(Scenario::getScenarioNo);
    }

    private String normalizeMatchText(String value) {
        String normalized =
                Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC)
                        .toLowerCase();
        return normalized.replaceAll("[^\\p{IsHangul}a-z0-9]", "");
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

    record SearchPayloadSnapshot(
            Long messageNo,
            List<Map<String, Object>> searchOptions,
            List<Map<String, Object>> overflowOptions) {}

    public record ChatRequestContext(
            UUID anonymousId, String ipAddress, String userAgent, Long userNo) {
        public ChatRequestContext(UUID anonymousId, String ipAddress, String userAgent) {
            this(anonymousId, ipAddress, userAgent, null);
        }
    }
}
