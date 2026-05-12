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
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRuntimeServiceTest {

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANONYMOUS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    ChatSessionMapper sessionMapper;
    @Mock
    ChatMessageMapper messageMapper;
    @Mock
    ChatFailureRecorder failureRecorder;
    @Mock
    ChatRecommendationMapper recommendationMapper;
    @Mock
    ScenarioMapper scenarioMapper;
    @Mock
    ScenarioVersionMapper versionMapper;
    @Mock
    ScenarioNodeMapper nodeMapper;
    @Mock
    ScenarioNodeOptionMapper optionMapper;
    @Mock
    ScenarioMatchingService matchingService;

    ChatRuntimeService service;
    ChatRequestContext context;
    AtomicLong messageIds;

    @BeforeEach
    void setUp() {
        service = new ChatRuntimeService(
            sessionMapper,
            messageMapper,
            failureRecorder,
            recommendationMapper,
            scenarioMapper,
            versionMapper,
            nodeMapper,
            optionMapper,
            matchingService,
            new ObjectMapper()
        );
        context = new ChatRequestContext(ANONYMOUS_ID, "127.0.0.1", "JUnit");
        messageIds = new AtomicLong(1L);
        lenient().doAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(messageIds.getAndIncrement());
            return null;
        }).when(messageMapper).insert(any(ChatMessage.class));
        lenient().when(messageMapper.findBySessionId(any(UUID.class))).thenReturn(List.of());
        lenient().when(optionMapper.findEnabledByNodeId(any())).thenReturn(List.of());
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
        doAnswer(invocation -> {
            ChatSession inserted = invocation.getArgument(0);
            when(sessionMapper.findById(inserted.getId())).thenReturn(Optional.of(inserted));
            return null;
        }).when(sessionMapper).insert(any(ChatSession.class));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of());

        var response = service.start(100L, context);

        verify(sessionMapper).insert(sessionCaptor.capture());
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(response.scenarioId()).isEqualTo(100L);
        assertThat(sessionCaptor.getValue().getState()).isEqualTo("ACTIVE");
        assertThat(sessionCaptor.getValue().getAnonymousId()).isEqualTo(ANONYMOUS_ID);
        assertThat(messageCaptor.getValue().getDirection()).isEqualTo("BOT");
        assertThat(messageCaptor.getValue().getSeq()).isEqualTo(1);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("안녕하세요");
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
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Map<String, Object>> detailCaptor = ArgumentCaptor.forClass((Class) Map.class);

        assertBusinessError(() -> service.selectOption(SESSION_ID, 999L, context), ErrorCode.VALIDATION_ERROR);

        verify(sessionMapper).lockSessionByAdvisoryKey(SESSION_ID);
        verify(failureRecorder).recordFailure(eq(SESSION_ID), isNull(), eq("INVALID_OPTION"), detailCaptor.capture());
        assertThat(detailCaptor.getValue()).containsEntry("requestedOptionId", 999L);
    }

    @Test
    void selectOptionCompletesWhenNextNodeIsNull() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of(option(10L, null, "종료")));
        when(messageMapper.selectNextSeq(SESSION_ID)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.selectOption(SESSION_ID, 10L, context);

        verify(sessionMapper).updateCurrentNode(eq(SESSION_ID), eq(300L), eq("COMPLETED"), any(OffsetDateTime.class));
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo("대화가 완료되었습니다.");
    }

    @Test
    void selectOptionRejectsNextNodeFromDifferentVersion() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(optionMapper.findEnabledByNodeId(300L)).thenReturn(List.of(option(10L, 400L, "다음")));
        when(messageMapper.selectNextSeq(SESSION_ID)).thenReturn(2);
        when(nodeMapper.findById(400L)).thenReturn(node(400L, 999L, "ANSWER", "다음", "내용"));

        assertBusinessError(() -> service.selectOption(SESSION_ID, 10L, context), ErrorCode.STATE_CONFLICT);
    }

    @Test
    void freeTextNoMatchRecordsFailureAndFallbackMessage() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "모르는 질문")).thenReturn(Optional.empty());
        when(messageMapper.selectNextSeq(SESSION_ID)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "모르는 질문", context);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        verify(failureRecorder).recordFailure(SESSION_ID, null, "NO_MATCH");
        assertThat(messageCaptor.getAllValues().get(0).getPayload()).contains("\"matched\":false", "\"matchType\":\"NONE\"");
        assertThat(messageCaptor.getAllValues().get(1).getLatencyMs()).isNotNull();
        assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo("질문에 맞는 답변을 찾지 못했습니다.");
    }

    @Test
    void freeTextMatchWithNextNodeAdvancesSession() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "배송")).thenReturn(Optional.of(new MatchResult(100L, null, 400L, 80.0, 100, 0, MatchType.KEYWORD)));
        when(messageMapper.selectNextSeq(SESSION_ID)).thenReturn(2);
        when(nodeMapper.findById(400L)).thenReturn(node(400L, 200L, "ANSWER", "배송", "배송 안내"));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "배송", context);

        verify(sessionMapper).updateCurrentNode(eq(SESSION_ID), eq(400L), eq("ACTIVE"), any(OffsetDateTime.class));
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getPayload()).contains("\"matched\":true", "\"score\":80.0", "\"matchType\":\"KEYWORD\"", "\"scenarioId\":100");
        assertThat(messageCaptor.getAllValues().get(1).getLatencyMs()).isNotNull();
    }

    @Test
    void freeTextMatchWithoutNextNodeReturnsScenarioTitle() {
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "환불")).thenReturn(Optional.of(new MatchResult(200L, null, null, 60.0, 100, 1, MatchType.KEYWORD)));
        when(messageMapper.selectNextSeq(SESSION_ID)).thenReturn(2);
        when(scenarioMapper.findById(200L)).thenReturn(scenario(200L, "ACTIVE", "환불 상담"));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "환불", context);

        verify(sessionMapper, never()).updateCurrentNode(eq(SESSION_ID), any(), any(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getContent()).isEqualTo("환불 상담");
    }

    @Test
    void getRejectsDifferentAnonymousOwner() {
        ChatSession session = activeSession();
        session.setAnonymousId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertBusinessError(() -> service.get(SESSION_ID, context), ErrorCode.ACCESS_DENIED);
    }

    @Test
    void selectOptionRejectsCompletedSession() {
        ChatSession session = activeSession();
        session.setState("COMPLETED");
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertBusinessError(() -> service.selectOption(SESSION_ID, 10L, context), ErrorCode.STATE_CONFLICT);
    }

    @Test
    void freeTextExpiredSessionRecordsExpiredFailure() {
        ChatSession session = activeSession();
        session.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(sessionMapper.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertBusinessError(() -> service.freeText(SESSION_ID, "배송", context), ErrorCode.STATE_CONFLICT);

        verify(failureRecorder).recordFailure(SESSION_ID, null, "EXPIRED");
    }

    private void assertBusinessError(ThrowingCallable callable, ErrorCode errorCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(errorCode);
    }

    private ChatSession activeSession() {
        ChatSession session = new ChatSession();
        session.setId(SESSION_ID);
        session.setAnonymousId(ANONYMOUS_ID);
        session.setScenarioId(100L);
        session.setVersionId(200L);
        session.setCurrentNodeId(300L);
        session.setState("ACTIVE");
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        return session;
    }

    private Scenario scenario(Long id, String status, String title) {
        Scenario scenario = new Scenario();
        scenario.setId(id);
        scenario.setStatus(status);
        scenario.setTitle(title);
        return scenario;
    }

    private ScenarioVersion version(Long id, Long scenarioId, Long startNodeId) {
        ScenarioVersion version = new ScenarioVersion();
        version.setId(id);
        version.setScenarioId(scenarioId);
        version.setStartNodeId(startNodeId);
        return version;
    }

    private ScenarioNode node(Long id, Long versionId, String type, String title, String content) {
        ScenarioNode node = new ScenarioNode();
        node.setId(id);
        node.setVersionId(versionId);
        node.setNodeType(type);
        node.setTitle(title);
        node.setContent(content);
        return node;
    }

    private ScenarioNodeOption option(Long id, Long nextNodeId, String label) {
        ScenarioNodeOption option = new ScenarioNodeOption();
        option.setId(id);
        option.setNextNodeId(nextNodeId);
        option.setLabel(label);
        option.setEnabled(true);
        return option;
    }

    private interface ThrowingCallable {
        void call();
    }
}
