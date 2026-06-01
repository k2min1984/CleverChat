package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerCitation;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionResponse;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionService;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedback;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService.ChatRequestContext;
import kr.co.cleverchat.domain.chatbot.service.ScenarioMatchingService.MatchResult;
import kr.co.cleverchat.domain.chatbot.service.ScenarioMatchingService.MatchType;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioVersionMapper;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.search.dto.SearchDtos.SearchResponse;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class ChatRuntimeServiceTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANONYMOUS_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String SESSION_ID_STR = SESSION_ID.toString();
    private static final String ANONYMOUS_ID_STR = ANONYMOUS_ID.toString();

    @Mock ChatSessionMapper sessionMapper;
    @Mock ChatMessageMapper messageMapper;
    @Mock ChatFeedbackMapper feedbackMapper;
    @Mock ChatFailureRecorder failureRecorder;
    @Mock ChatRecommendationMapper recommendationMapper;
    @Mock ScenarioMapper scenarioMapper;
    @Mock ScenarioVersionMapper versionMapper;
    @Mock ScenarioNodeMapper nodeMapper;
    @Mock ScenarioNodeOptionMapper optionMapper;
    @Mock ScenarioMatchingService matchingService;
    @Mock ChatPiiGuard piiGuard;
    @Mock ChatRateLimiter rateLimiter;
    @Mock SearchService searchService;
    @Mock AiAnswerSuggestionService aiAnswerSuggestionService;
    FieldEncryptionService fieldEncryptionService;

    ChatRuntimeService service;
    ChatRequestContext context;
    AtomicLong messageIds;

    @BeforeEach
    void setUp() {
        fieldEncryptionService =
                new FieldEncryptionService("", "test-v1", "", new MockEnvironment());
        service =
                new ChatRuntimeService(
                        sessionMapper,
                        messageMapper,
                        feedbackMapper,
                        failureRecorder,
                        recommendationMapper,
                        scenarioMapper,
                        versionMapper,
                        nodeMapper,
                        optionMapper,
                        matchingService,
                        piiGuard,
                        rateLimiter,
                        searchService,
                        aiAnswerSuggestionService,
                        fieldEncryptionService,
                        new ObjectMapper());
        context = new ChatRequestContext(ANONYMOUS_ID, "127.0.0.1", "JUnit");
        messageIds = new AtomicLong(1L);
        lenient()
                .doAnswer(
                        invocation -> {
                            ChatMessage message = invocation.getArgument(0);
                            message.setChatMessageNo(messageIds.getAndIncrement());
                            return null;
                        })
                .when(messageMapper)
                .insert(any(ChatMessage.class));
        lenient().when(messageMapper.findBySessionId(any(String.class))).thenReturn(List.of());
        lenient().when(optionMapper.findEnabledByNodeId(any())).thenReturn(List.of());
        lenient().when(piiGuard.detectTypes(any())).thenReturn(List.of());
        lenient().when(piiGuard.hasHighRiskTypes(any())).thenReturn(false);
        lenient()
                .when(piiGuard.maskLowRisk(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(rateLimiter.isAllowed(any())).thenReturn(true);
        lenient()
                .when(searchService.search(any(), any(), any(), any(), any()))
                .thenReturn(new SearchResponse("", 0, List.of()));
        lenient()
                .when(aiAnswerSuggestionService.suggest(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void startCreatesSessionAndFirstBotMessage() {
        Scenario scenario = scenario(100L, "ACTIVE", "상담");
        ScenarioVersion version = version(200L, 100L, 300L);
        ScenarioNode startNode = node(300L, 200L, "START", "시작", "안녕하세요");
        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        when(scenarioMapper.findById(100L)).thenReturn(scenario);
        when(versionMapper.findPublishedByScenarioId(100L)).thenReturn(version);
        when(nodeMapper.findById(300L)).thenReturn(startNode);
        doAnswer(
                        invocation -> {
                            ChatSession inserted = invocation.getArgument(0);
                            when(sessionMapper.findById(inserted.getChatSessionNo()))
                                    .thenReturn(Optional.of(inserted));
                            return null;
                        })
                .when(sessionMapper)
                .insert(any(ChatSession.class));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of());

        var response = service.start(100L, context);

        verify(sessionMapper).insert(sessionCaptor.capture());
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(response.scenarioId()).isEqualTo(100L);
        assertThat(sessionCaptor.getValue().getState()).isEqualTo("ACTIVE");
        assertThat(sessionCaptor.getValue().getAnonymousId()).isEqualTo(ANONYMOUS_ID_STR);
        assertThat(messageCaptor.getValue().getDirection()).isEqualTo("BOT");
        assertThat(messageCaptor.getValue().getSeq()).isEqualTo(1);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("[encrypted]");
        assertThat(decrypted(messageCaptor.getValue())).isEqualTo("안녕하세요");
    }

    @Test
    void activeScenariosReturnsActiveScenarioSummaries() {
        when(scenarioMapper.findAll("ACTIVE"))
                .thenReturn(
                        List.of(
                                scenario(100L, "ACTIVE", "가입 상담"),
                                scenario(200L, "ACTIVE", "배송 상담")));

        var result = service.activeScenarios();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(100L);
        assertThat(result.get(0).title()).isEqualTo("가입 상담");
        assertThat(result.get(1).id()).isEqualTo(200L);
    }

    @Test
    void startRejectsInactiveScenario() {
        when(scenarioMapper.findById(100L)).thenReturn(scenario(100L, "INACTIVE", "상담"));

        assertBusinessError(() -> service.start(100L, context), ErrorCode.NOT_FOUND);
    }

    @Test
    void startRejectsPublishedVersionWithoutStartNode() {
        ScenarioVersion version = version(200L, 100L, null);
        when(scenarioMapper.findById(100L)).thenReturn(scenario(100L, "ACTIVE", "상담"));
        when(versionMapper.findPublishedByScenarioId(100L)).thenReturn(version);

        assertBusinessError(() -> service.start(100L, context), ErrorCode.STATE_CONFLICT);
    }

    @Test
    void selectOptionRecordsInvalidOptionFailure() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Map<String, Object>> detailCaptor =
                ArgumentCaptor.forClass((Class) Map.class);

        assertBusinessError(
                () -> service.selectOption(SESSION_ID, 999L, context), ErrorCode.VALIDATION_ERROR);

        verify(sessionMapper).lockSessionByAdvisoryKey(SESSION_ID_STR);
        verify(failureRecorder)
                .recordFailure(
                        eq(SESSION_ID_STR), isNull(), eq("INVALID_OPTION"), detailCaptor.capture());
        assertThat(detailCaptor.getValue()).containsEntry("requestedOptionId", 999L);
    }

    @Test
    void selectOptionCompletesWhenNextNodeIsNull() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of(option(10L, null, "종료")));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.selectOption(SESSION_ID, 10L, context);

        verify(sessionMapper)
                .updateCurrentNode(
                        eq(SESSION_ID_STR), eq(300L), eq("COMPLETED"), any(OffsetDateTime.class));
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(decrypted(messageCaptor.getAllValues().get(1))).isEqualTo("대화가 완료되었습니다.");
    }

    @Test
    void selectOptionRejectsNextNodeFromDifferentVersion() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of(option(10L, 400L, "다음")));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        when(nodeMapper.findById(400L)).thenReturn(node(400L, 999L, "ANSWER", "다음", "내용"));

        assertBusinessError(
                () -> service.selectOption(SESSION_ID, 10L, context), ErrorCode.STATE_CONFLICT);
    }

    @Test
    void freeTextNoMatchRecordsFailureAndFallbackMessage() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "모르는 질문")).thenReturn(Optional.empty());
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "모르는 질문", context);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        verify(failureRecorder).recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        assertThat(messageCaptor.getAllValues().get(0).getPayload())
                .contains("\"matched\":false", "\"matchType\":\"NONE\"");
        assertThat(messageCaptor.getAllValues().get(1).getLatencyMs()).isNotNull();
        assertThat(decrypted(messageCaptor.getAllValues().get(1)))
                .isEqualTo("질문에 맞는 답변을 찾지 못했습니다.");
    }

    @Test
    void freeTextNoMatchUsesSearchFallbackWhenAvailable() {
        ChatSession session = activeSession();
        SearchResultItem result = new SearchResultItem();
        result.setScenarioNo(200L);
        result.setScenarioTitle("Search result");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "unknown")).thenReturn(Optional.empty());
        when(searchService.search(eq("unknown"), eq("CHAT_FALLBACK"), eq(3), isNull(), any()))
                .thenReturn(new SearchResponse("unknown", 1, List.of(result)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "unknown", context);

        verify(failureRecorder, never())
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(decrypted(messageCaptor.getAllValues().get(1)))
                .isEqualTo("관련 상담: Search result");
    }

    @Test
    void freeTextNoMatchUsesAiSuggestionWhenAvailable() {
        ChatSession session = activeSession();
        SearchResultItem result = new SearchResultItem();
        result.setScenarioNo(200L);
        result.setScenarioTitle("Search result");
        result.setMatchedField("SCENARIO");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "unknown")).thenReturn(Optional.empty());
        when(searchService.search(eq("unknown"), eq("CHAT_FALLBACK"), eq(3), isNull(), any()))
                .thenReturn(new SearchResponse("unknown", 1, List.of(result)));
        when(aiAnswerSuggestionService.suggest(eq("unknown"), any(), any()))
                .thenReturn(
                        Optional.of(
                                new AiAnswerSuggestionResponse(
                                        "AI scaffold answer",
                                        List.of(
                                                new AiAnswerCitation(
                                                        "SCENARIO",
                                                        200L,
                                                        "Search result",
                                                        null,
                                                        null,
                                                        "SCENARIO",
                                                        null)),
                                        "stub",
                                        true)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "unknown", context);

        verify(failureRecorder, never())
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(decrypted(messageCaptor.getAllValues().get(1))).isEqualTo("AI scaffold answer");
    }

    @Test
    void freeTextMatchWithNextNodeAdvancesSession() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "배송"))
                .thenReturn(
                        Optional.of(
                                new MatchResult(
                                        100L, null, 400L, 80.0, 100, 0, MatchType.KEYWORD)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        when(nodeMapper.findById(400L)).thenReturn(node(400L, 200L, "ANSWER", "배송", "배송 안내"));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "배송", context);

        verify(sessionMapper)
                .updateCurrentNode(
                        eq(SESSION_ID_STR), eq(400L), eq("ACTIVE"), any(OffsetDateTime.class));
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getPayload())
                .contains(
                        "\"matched\":true",
                        "\"score\":80.0",
                        "\"matchType\":\"KEYWORD\"",
                        "\"scenarioId\":100");
        assertThat(messageCaptor.getAllValues().get(1).getLatencyMs()).isNotNull();
    }

    @Test
    void freeTextMatchWithoutNextNodeReturnsScenarioTitle() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "환불"))
                .thenReturn(
                        Optional.of(
                                new MatchResult(
                                        200L, null, null, 60.0, 100, 1, MatchType.KEYWORD)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        when(scenarioMapper.findById(200L)).thenReturn(scenario(200L, "ACTIVE", "환불 상담"));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "환불", context);

        verify(sessionMapper, never()).updateCurrentNode(eq(SESSION_ID_STR), any(), any(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(decrypted(messageCaptor.getAllValues().get(1))).isEqualTo("환불 상담");
    }

    @Test
    void getRejectsDifferentAnonymousOwner() {
        ChatSession session = activeSession();
        session.setAnonymousId("00000000-0000-0000-0000-000000000003");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));

        assertBusinessError(() -> service.get(SESSION_ID, context), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void historyReturnsOwnedSessionMessagesWithoutExpiryCheck() {
        ChatSession session = activeSession();
        session.setState("COMPLETED");
        session.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        ChatMessage message = botMessage(10L, SESSION_ID_STR);
        when(messageMapper.findBySessionId(SESSION_ID_STR)).thenReturn(List.of(message));

        var result = service.history(SESSION_ID, context);

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).id()).isEqualTo(10L);
        verify(sessionMapper, never()).markExpired(any());
    }

    @Test
    void historyListReturnsRecentSessionsForAnonymousId() {
        ChatSessionListItem item = new ChatSessionListItem();
        item.setChatSessionNo(SESSION_ID_STR);
        item.setScenarioNo(100L);
        item.setScenarioTitle("상담");
        item.setState("COMPLETED");
        item.setMessageCount(2);
        var encryptedLastMessage = fieldEncryptionService.encrypt("decrypted answer");
        item.setLastMessage("끝");
        item.setLastMessage("[encrypted]");
        item.setLastMessageCiphertext(encryptedLastMessage.ciphertext());
        item.setLastMessageKeyId(encryptedLastMessage.keyId());
        item.setLastMessageEncryptionVersion(encryptedLastMessage.version());
        when(sessionMapper.findHistoryByAnonymousId(
                        eq(ANONYMOUS_ID_STR), any(OffsetDateTime.class), eq(50)))
                .thenReturn(List.of(item));

        var result = service.historyList(ANONYMOUS_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.get(0).scenarioTitle()).isEqualTo("상담");
        assertThat(result.get(0).lastMessage()).isEqualTo("decrypted answer");
    }

    @Test
    void selectOptionRejectsCompletedSession() {
        ChatSession session = activeSession();
        session.setState("COMPLETED");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));

        assertBusinessError(
                () -> service.selectOption(SESSION_ID, 10L, context), ErrorCode.STATE_CONFLICT);
    }

    @Test
    void freeTextExpiredSessionRecordsExpiredFailure() {
        ChatSession session = activeSession();
        session.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));

        assertBusinessError(
                () -> service.freeText(SESSION_ID, "shipping", context), ErrorCode.SESSION_EXPIRED);

        verify(sessionMapper).markExpired(SESSION_ID_STR);
        verify(failureRecorder).recordFailure(SESSION_ID_STR, null, "EXPIRED");
    }

    @Test
    void freeTextBlocksHighRiskPiiWithoutSavingUserMessage() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(piiGuard.detectTypes("rrn 900101-1234567")).thenReturn(List.of("RRN"));
        when(piiGuard.hasHighRiskTypes(List.of("RRN"))).thenReturn(true);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Map<String, Object>> detailCaptor =
                ArgumentCaptor.forClass((Class) Map.class);

        assertBusinessError(
                () -> service.freeText(SESSION_ID, "rrn 900101-1234567", context),
                ErrorCode.VALIDATION_ERROR);

        verify(failureRecorder)
                .recordFailure(
                        eq(SESSION_ID_STR), isNull(), eq("PII_BLOCKED"), detailCaptor.capture());
        assertThat(detailCaptor.getValue()).containsEntry("piiTypes", List.of("RRN"));
        verify(messageMapper, never()).insert(any(ChatMessage.class));
        verify(matchingService, never()).match(any(), any(), any());
    }

    @Test
    void freeTextMasksLowRiskPiiBeforeSavingAndFallbackSearch() {
        ChatSession session = activeSession();
        String original = "email test@example.com phone 010-1234-5678";
        String masked = "email te***@example.com phone 010-****-**78";
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(piiGuard.detectTypes(original)).thenReturn(List.of("EMAIL", "PHONE"));
        when(piiGuard.hasHighRiskTypes(List.of("EMAIL", "PHONE"))).thenReturn(false);
        when(piiGuard.maskLowRisk(original)).thenReturn(masked);
        when(matchingService.match(100L, 300L, masked)).thenReturn(Optional.empty());
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, original, context);

        verify(searchService).search(eq(masked), eq("CHAT_FALLBACK"), eq(3), isNull(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getContent()).isEqualTo("[encrypted]");
        assertThat(messageCaptor.getAllValues().get(0).getContentCiphertext()).isNotBlank();
        assertThat(decrypted(messageCaptor.getAllValues().get(0))).isEqualTo(masked);
        assertThat(messageCaptor.getAllValues().get(0).getContentCiphertext())
                .doesNotContain("test@example.com", "1234-5678");
        verify(failureRecorder).recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
    }

    @Test
    void freeTextRateLimitedRecordsFailureWithoutSavingUserMessage() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(rateLimiter.isAllowed(ANONYMOUS_ID_STR + ":" + SESSION_ID_STR)).thenReturn(false);

        assertBusinessError(
                () -> service.freeText(SESSION_ID, "hello", context), ErrorCode.RATE_LIMITED);

        verify(failureRecorder)
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("RATE_LIMITED"), any());
        verify(messageMapper, never()).insert(any(ChatMessage.class));
        verify(matchingService, never()).match(any(), any(), any());
    }

    @Test
    void feedbackUpsertsForOwnedBotMessageWithMaskedComment() {
        ChatSession session = activeSession();
        ChatMessage message = botMessage(10L, SESSION_ID_STR);
        String original = "email test@example.com";
        String masked = "email te***@example.com";
        when(messageMapper.findById(10L)).thenReturn(Optional.of(message));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(piiGuard.detectTypes(original)).thenReturn(List.of("EMAIL"));
        when(piiGuard.hasHighRiskTypes(List.of("EMAIL"))).thenReturn(false);
        when(piiGuard.maskLowRisk(original)).thenReturn(masked);
        ArgumentCaptor<ChatFeedback> feedbackCaptor = ArgumentCaptor.forClass(ChatFeedback.class);

        var result = service.feedback(10L, "UP", original, context);

        verify(feedbackMapper).upsert(feedbackCaptor.capture());
        assertThat(result.comment()).isEqualTo(masked);
        assertThat(feedbackCaptor.getValue().getMessageNo()).isEqualTo(10L);
        assertThat(feedbackCaptor.getValue().getRating()).isEqualTo("UP");
        assertThat(feedbackCaptor.getValue().getComment()).isEqualTo("[encrypted]");
        assertThat(feedbackCaptor.getValue().getCommentCiphertext()).isNotBlank();
        assertThat(
                        fieldEncryptionService.decryptOrFallback(
                                feedbackCaptor.getValue().getCommentCiphertext(),
                                feedbackCaptor.getValue().getComment()))
                .isEqualTo(masked);
        assertThat(feedbackCaptor.getValue().getIpHash()).hasSize(64);
    }

    @Test
    void feedbackRejectsUserMessage() {
        ChatSession session = activeSession();
        ChatMessage message = botMessage(10L, SESSION_ID_STR);
        message.setDirection("USER");
        when(messageMapper.findById(10L)).thenReturn(Optional.of(message));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));

        assertBusinessError(
                () -> service.feedback(10L, "DOWN", null, context), ErrorCode.VALIDATION_ERROR);

        verify(feedbackMapper, never()).upsert(any(ChatFeedback.class));
    }

    @Test
    void feedbackRejectsOtherAnonymousOwner() {
        ChatSession session = activeSession();
        session.setAnonymousId("00000000-0000-0000-0000-000000000003");
        when(messageMapper.findById(10L)).thenReturn(Optional.of(botMessage(10L, SESSION_ID_STR)));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));

        assertBusinessError(
                () -> service.feedback(10L, "UP", null, context), ErrorCode.ACCESS_DENIED);
    }

    private void assertBusinessError(ThrowingCallable callable, ErrorCode errorCode) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private ChatSession activeSession() {
        ChatSession session = new ChatSession();
        session.setChatSessionNo(SESSION_ID_STR);
        session.setAnonymousId(ANONYMOUS_ID_STR);
        session.setScenarioNo(100L);
        session.setVersionNo(200L);
        session.setCurrentNodeNo(300L);
        session.setState("ACTIVE");
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        return session;
    }

    private Scenario scenario(Long id, String status, String title) {
        Scenario scenario = new Scenario();
        scenario.setScenarioNo(id);
        scenario.setStatus(status);
        scenario.setTitle(title);
        return scenario;
    }

    private ScenarioVersion version(Long id, Long scenarioId, Long startNodeId) {
        ScenarioVersion version = new ScenarioVersion();
        version.setScenarioVersionNo(id);
        version.setScenarioNo(scenarioId);
        version.setStartNodeNo(startNodeId);
        return version;
    }

    private ScenarioNode node(Long id, Long versionId, String type, String title, String content) {
        ScenarioNode node = new ScenarioNode();
        node.setScenarioNodeNo(id);
        node.setVersionNo(versionId);
        node.setNodeType(type);
        node.setTitle(title);
        node.setContent(content);
        return node;
    }

    private ScenarioNodeOption option(Long id, Long nextNodeId, String label) {
        ScenarioNodeOption option = new ScenarioNodeOption();
        option.setScenarioNodeOptionNo(id);
        option.setNextNodeNo(nextNodeId);
        option.setLabel(label);
        option.setUseYn("Y");
        return option;
    }

    private ChatMessage botMessage(Long id, String sessionId) {
        ChatMessage message = new ChatMessage();
        message.setChatMessageNo(id);
        message.setSessionNo(sessionId);
        message.setSeq(1);
        message.setDirection("BOT");
        message.setContent("답변");
        message.setFrstRegDt(OffsetDateTime.now());
        return message;
    }

    private String decrypted(ChatMessage message) {
        return fieldEncryptionService.decryptOrFallback(
                message.getContentCiphertext(), message.getContent());
    }

    private interface ThrowingCallable {
        void call();
    }
}
