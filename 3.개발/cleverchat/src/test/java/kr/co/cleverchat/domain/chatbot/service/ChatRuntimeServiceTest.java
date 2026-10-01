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
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionResponse;
import kr.co.cleverchat.domain.chatbot.ai.AiAnswerSuggestionService;
import kr.co.cleverchat.domain.chatbot.config.ChatSearchProperties;
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
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeLinkMapper;
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
    @Mock ScenarioNodeLinkMapper linkMapper;
    @Mock ScenarioMatchingService matchingService;
    @Mock ChatPiiGuard piiGuard;
    @Mock ChatRateLimiter rateLimiter;
    @Mock SearchService searchService;
    @Mock AiAnswerSuggestionService aiAnswerSuggestionService;
    FieldEncryptionService fieldEncryptionService;
    ChatSearchProperties searchProperties;

    ChatRuntimeService service;
    ChatRequestContext context;
    AtomicLong messageIds;

    @BeforeEach
    void setUp() {
        fieldEncryptionService =
                new FieldEncryptionService("", "test-v1", "", new MockEnvironment());
        searchProperties = new ChatSearchProperties();
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
                        linkMapper,
                        matchingService,
                        piiGuard,
                        rateLimiter,
                        searchService,
                        searchProperties,
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
        when(scenarioMapper.findActiveForMatching())
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
        when(searchService.search(eq("unknown"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("unknown", 1, List.of(result)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "unknown", context);

        verify(failureRecorder, never())
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(decrypted(messageCaptor.getAllValues().get(1)))
                .isEqualTo("관련 자료를 찾았어요. 아래에서 선택해 주세요.");
        assertThat(messageCaptor.getAllValues().get(1).getPayload()).contains("\"label\":\"Search result\"");
    }

    @Test
    void crawlSearchResultLinksDirectlyToGetDetailPage() {
        ChatSession session = activeSession();
        SearchResultItem result = searchResult(77L, "공지사항", 1.0);
        result.setCrawlUrl(
                "https://www.kepco.co.kr/home/customer/notice/boardView.do?boardMngNo=1&boardNo=2");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "공지사항")).thenReturn(Optional.empty());
        when(searchService.search(eq("공지사항"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("공지사항", 1, List.of(result)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "공지사항", context);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getPayload())
                .contains(
                        "\"label\":\"해당 페이지 이동\"",
                        "\"linkType\":\"EXTERNAL\"",
                        "boardView.do?boardMngNo=1&boardNo=2")
                .doesNotContain("/chat/crawl-documents/", "boardList.do");
    }

    @Test
    void crawledDocumentUsesAiAnswerAndKeepsSourceLink() {
        ChatSession session = activeSession();
        SearchResultItem result = searchResult(77L, "이용 안내", 1.0);
        result.setCrawlUrl("https://example.com/guide");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "이용 방법")).thenReturn(Optional.empty());
        when(searchService.search(eq("이용 방법"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("이용 방법", 1, List.of(result)));
        when(aiAnswerSuggestionService.suggest(eq("이용 방법"), any(), any()))
                .thenReturn(
                        Optional.of(
                                new AiAnswerSuggestionResponse(
                                        "근거에 따른 AI 안내", List.of(), "vllm", true)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        var messages = ArgumentCaptor.forClass(ChatMessage.class);
        service.freeText(SESSION_ID, "이용 방법", context);
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messages.capture());
        assertThat(decrypted(messages.getAllValues().get(1))).isEqualTo("근거에 따른 AI 안내");
        assertThat(messages.getAllValues().get(1).getPayload())
                .contains("https://example.com/guide");
    }

    @Test
    void freeTextSearchResultsExcludeWeakMatchesFromBothDisplayAndOverflow() {
        ChatSession session = activeSession();
        searchProperties.setMaxCrawlDocuments(5);
        SearchResultItem first = searchResult(1L, "first", 1.0);
        SearchResultItem second = searchResult(2L, "second", 0.75);
        SearchResultItem third = searchResult(3L, "third", 0.7);
        SearchResultItem fourth = searchResult(4L, "fourth", 0.65);
        SearchResultItem fifth = searchResult(5L, "fifth", 0.6);
        SearchResultItem hidden = searchResult(6L, "hidden", 0.49);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "query")).thenReturn(Optional.empty());
        when(searchService.search(eq("query"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(
                        new SearchResponse(
                                "query", 6, List.of(first, second, third, fourth, fifth, hidden)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "query", context);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        String payload = messageCaptor.getAllValues().get(1).getPayload();
        assertThat(payload).contains("\"searchOptions\"");
        assertThat(payload).contains("\"crawlDocumentNo\":1", "\"crawlDocumentNo\":2");
        assertThat(payload).contains("\"overflowOptions\":[]");
        assertThat(payload).doesNotContain("\"crawlDocumentNo\":6", "\"more\"");
    }

    @Test
    void normalizeCrawlLabelRemovesBreadcrumbAndSiteSuffix() {
        assertThat(service.normalizeCrawlLabel("Hydrogen | ESG | KEPCO")).isEqualTo("Hydrogen");
        assertThat(service.normalizeCrawlLabel("KEPCO | Research")).isEqualTo("Research");
        assertThat(service.normalizeCrawlLabel("KEPCO")).isEqualTo("KEPCO");
    }

    @Test
    void searchOptionsExposeOptionTypeAndPreserveRelevanceOrder() {
        ChatSession session = activeSession();
        SearchResultItem document = searchResult(77L, "Doc title | KEPCO", 1.0);
        SearchResultItem scenario = new SearchResultItem();
        scenario.setScenarioNo(55L);
        scenario.setScenarioTitle("Scenario title");
        scenario.setMatchedField("SCENARIO");
        scenario.setScore(0.9);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "power")).thenReturn(Optional.empty());
        when(searchService.search(eq("power"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("power", 2, List.of(document, scenario)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "power", context);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        String payload = messageCaptor.getAllValues().get(1).getPayload();
        assertThat(payload).contains("\"optionType\":\"SCENARIO\"", "\"optionType\":\"DOCUMENT\"");
        assertThat(payload).contains("\"label\":\"Doc title\"");
        assertThat(payload.indexOf("\"crawlDocumentNo\":77"))
                .isLessThan(payload.indexOf("\"scenarioNo\":55"));
    }

    @Test
    void selectForDisplayAppliesMinScoreAndCrawlDocumentCapWithoutForcingWeakResult() {
        searchProperties.setDisplayMax(2);
        searchProperties.setMinScore(0.5);
        searchProperties.setMaxCrawlDocuments(2);
        SearchResultItem first = searchResult(1L, "first", 1.0);
        SearchResultItem second = searchResult(2L, "second", 0.9);
        SearchResultItem capped = searchResult(3L, "capped", 0.8);
        SearchResultItem cut = searchResult(4L, "cut", 0.1);

        var selection = service.selectForDisplay(List.of(first, second, capped, cut));

        assertThat(selection.shown())
                .extracting(SearchResultItem::getCrawlDocumentNo)
                .containsExactly(1L, 2L);
        assertThat(selection.hidden())
                .extracting(SearchResultItem::getCrawlDocumentNo)
                .containsExactly(3L);

        searchProperties.setMinScore(2.0);
        var topOnly = service.selectForDisplay(List.of(first, second));
        assertThat(topOnly.shown()).isEmpty();
        assertThat(topOnly.hidden()).isEmpty();
    }

    @Test
    void selectForDisplayAppliesRelativeThresholdWithinEachResultGroupAndSortsByScore() {
        searchProperties.setRelativeThreshold(0.5);
        SearchResultItem topDocument = searchResult(1L, "top document", 10.0);
        SearchResultItem firstScenario = scenarioResult(11L, "first scenario", 4.0);
        SearchResultItem secondScenario = scenarioResult(12L, "second scenario", 3.0);
        SearchResultItem secondDocument = searchResult(2L, "second document", 8.0);

        var selection =
                service.selectForDisplay(
                        List.of(topDocument, firstScenario, secondScenario, secondDocument));

        assertThat(selection.shown())
                .extracting(SearchResultItem::getScenarioTitle)
                .containsExactly(
                        "top document", "second document", "first scenario", "second scenario");
        assertThat(selection.hidden()).isEmpty();
    }

    @Test
    void selectForDisplayAppliesRelevanceThresholdEvenToSmallResultSets() {
        SearchResultItem first = searchResult(1L, "first", 100.0);
        SearchResultItem second = searchResult(2L, "second", 10.0);
        SearchResultItem third = searchResult(3L, "third", 5.0);
        SearchResultItem fourth = searchResult(4L, "fourth", 1.0);

        var selection = service.selectForDisplay(List.of(first, second, third, fourth));

        assertThat(selection.shown())
                .extracting(SearchResultItem::getCrawlDocumentNo)
                .containsExactly(1L);
        assertThat(selection.hidden()).isEmpty();
    }

    @Test
    void selectForDisplayDeduplicatesDocumentsWithSameVisibleTitle() {
        SearchResultItem higher = searchResult(6480L, "서울 동남권지역 전기공급시설 전력구 건설(동서울#2-강남1차)", 10.0);
        SearchResultItem duplicate = searchResult(6405L, "서울 동남권지역 전기공급시설 전력구 건설(동서울#2-강남1차)", 9.0);
        SearchResultItem other = searchResult(3925L, "동해안-동서울 HVDC 건설사업", 8.0);

        var selection = service.selectForDisplay(List.of(higher, duplicate, other));

        assertThat(selection.shown())
                .extracting(SearchResultItem::getCrawlDocumentNo)
                .containsExactly(6480L, 3925L);
        assertThat(selection.hidden()).isEmpty();
    }

    @Test
    void selectSearchResultRejectsOverflowOptionUntilMoreIsRequested() {
        ChatSession session = activeSession();
        String payload =
                "{\"searchOptions\":[{\"crawlDocumentNo\":1,\"label\":\"shown\"}],"
                        + "\"overflowOptions\":[{\"crawlDocumentNo\":3,\"label\":\"hidden\"}]}";
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(botMessageWithPayload(payload)));

        assertBusinessError(
                () -> service.selectSearchResult(SESSION_ID, 3L, null, context),
                ErrorCode.VALIDATION_ERROR);

        verify(failureRecorder)
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("INVALID_OPTION"), any());
    }

    @Test
    void documentCapAlsoAppliesWhenResultsFitWithinDisplayMax() {
        searchProperties.setDisplayMax(5);
        searchProperties.setMaxCrawlDocuments(1);
        var selection = service.selectForDisplay(List.of(
                searchResult(1L, "납부 방법", 10), searchResult(2L, "자동이체", 9)));
        assertThat(selection.shown()).extracting(SearchResultItem::getCrawlDocumentNo).containsExactly(1L);
        assertThat(selection.hidden()).extracting(SearchResultItem::getCrawlDocumentNo).containsExactly(2L);
    }

    @Test
    void belowMinimumScoreProducesNoMatchInsteadOfAnUnrelatedAnswer() {
        searchProperties.setMinScore(5);
        ChatSession session = activeSession();
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "unknown")).thenReturn(Optional.empty());
        when(searchService.search(eq("unknown"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("unknown", 1, List.of(searchResult(1L, "무관한 자료", 1))));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        service.freeText(SESSION_ID, "unknown", context);
        verify(failureRecorder).recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        verify(aiAnswerSuggestionService, never()).suggest(any(), any(), any());
    }

    @Test
    void searchMoreAppendsAllRemainingOptionsWithoutAnotherMoreButton() {
        ChatSession session = activeSession();
        String payload =
                "{\"searchOptions\":[{\"crawlDocumentNo\":1,\"label\":\"shown\"}],"
                        + "\"overflowOptions\":["
                        + "{\"crawlDocumentNo\":3,\"label\":\"hidden 1\"},"
                        + "{\"crawlDocumentNo\":4,\"label\":\"hidden 2\"},"
                        + "{\"crawlDocumentNo\":5,\"label\":\"hidden 3\"},"
                        + "{\"crawlDocumentNo\":6,\"label\":\"hidden 4\"},"
                        + "{\"crawlDocumentNo\":7,\"label\":\"hidden 5\"},"
                        + "{\"crawlDocumentNo\":8,\"label\":\"hidden 6\"},"
                        + "{\"crawlDocumentNo\":9,\"label\":\"hidden 7\"}]}";
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(botMessageWithPayload(payload)), List.of());
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(7);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.searchMore(SESSION_ID, context);

        verify(messageMapper).insert(messageCaptor.capture());
        ChatMessage botMessage = messageCaptor.getValue();
        assertThat(botMessage.getSeq()).isEqualTo(7);
        assertThat(botMessage.getPayload())
                .contains(
                        "\"searchOptions\"",
                        "\"crawlDocumentNo\":3",
                        "\"crawlDocumentNo\":8",
                        "\"crawlDocumentNo\":9");
        assertThat(botMessage.getPayload()).contains("\"overflowOptions\":[]");
        assertThat(botMessage.getPayload()).doesNotContain("\"more\"");
    }

    @Test
    void singleScenarioSearchResultRemainsSelectable() {
        ChatSession session = activeSession();
        SearchResultItem result = new SearchResultItem();
        result.setScenarioNo(200L);
        result.setScenarioTitle("Search result");
        result.setMatchedField("SCENARIO");
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "unknown")).thenReturn(Optional.empty());
        when(searchService.search(eq("unknown"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("unknown", 1, List.of(result)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "unknown", context);

        verify(failureRecorder, never())
                .recordFailure(eq(SESSION_ID_STR), isNull(), eq("NO_MATCH"), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getPayload()).contains("\"scenarioNo\":200");
        verify(aiAnswerSuggestionService, never()).suggest(eq("unknown"), any(), any());
    }

    @Test
    void contextualKeywordDoesNotInjectAnUnrankedParentMenu() {
        ChatSession session = activeSession();
        SearchResultItem document = searchResult(77L, "환경 보고서", 20.0);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "배송"))
                .thenReturn(
                        Optional.of(
                                new MatchResult(
                                        100L, null, 400L, 80.0, 100, 0, MatchType.KEYWORD)));
        when(searchService.search(eq("배송"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("배송", 1, List.of(document)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "배송", context);

        verify(sessionMapper, never()).updateCurrentNode(any(), any(), any(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(0).getPayload())
                .contains(
                        "\"matched\":true",
                        "\"score\":80.0",
                        "\"matchType\":\"KEYWORD\"",
                        "\"scenarioId\":100");
        assertThat(messageCaptor.getAllValues().get(1).getPayload()).doesNotContain("\"scenarioNo\":100");
        verify(scenarioMapper, never()).findActiveMenuMatches(any());
    }

    @Test
    void freeTextGlobalScenarioMatchStillUsesIntegratedSearch() {
        ChatSession session = activeSession();
        SearchResultItem document = searchResult(88L, "환불 안내문", 10.0);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(matchingService.match(100L, 300L, "환불"))
                .thenReturn(
                        Optional.of(
                                new MatchResult(
                                        200L, null, null, 60.0, 100, 1, MatchType.KEYWORD)));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(2);
        when(searchService.search(eq("환불"), eq("CHAT_FALLBACK"), eq(10), isNull(), any()))
                .thenReturn(new SearchResponse("환불", 1, List.of(document)));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.freeText(SESSION_ID, "환불", context);

        verify(sessionMapper, never()).updateCurrentNode(eq(SESSION_ID_STR), any(), any(), any());
        verify(messageMapper, org.mockito.Mockito.times(2)).insert(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1).getPayload())
                .doesNotContain("\"scenarioNo\":200");
        verify(scenarioMapper, never()).findActiveMenuMatches(any());
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
    void goBackAppendsBotMessageAndRecordsNodeBackEvent() {
        ChatSession session = activeSession();
        session.setCurrentNodeNo(302L);
        ScenarioNode previous = node(301L, 200L, "QUESTION", "previous", "previous content");
        ChatMessage first = botMessage(10L, SESSION_ID_STR);
        first.setSeq(1);
        first.setNodeNo(300L);
        ChatMessage middle = botMessage(11L, SESSION_ID_STR);
        middle.setSeq(3);
        middle.setNodeNo(301L);
        ChatMessage current = botMessage(12L, SESSION_ID_STR);
        current.setSeq(5);
        current.setNodeNo(302L);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(first, middle, current), List.of(first, middle, current));
        when(nodeMapper.findById(301L)).thenReturn(previous);
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(6);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        var response = service.goBack(SESSION_ID, context);

        verify(sessionMapper).updateCurrentNode(eq(SESSION_ID_STR), eq(301L), eq("ACTIVE"), any());
        verify(sessionMapper)
                .insertNodeBackEvent(
                        eq(SESSION_ID_STR),
                        eq(302L),
                        eq(301L),
                        any(),
                        org.mockito.ArgumentMatchers.contains("\"fromNodeNo\":302"));
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getDirection()).isEqualTo("BOT");
        assertThat(messageCaptor.getValue().getNodeNo()).isEqualTo(301L);
        assertThat(decrypted(messageCaptor.getValue())).contains("이전 단계로 돌아갑니다.");
        assertThat(response.canGoBack()).isTrue();
    }

    @Test
    void goBackReplaysPriorBackPayloadAsNavigationStack() {
        ChatSession session = activeSession();
        session.setCurrentNodeNo(301L);
        ScenarioNode previous = node(300L, 200L, "QUESTION", "start", "start content");
        ChatMessage first = botMessage(10L, SESSION_ID_STR);
        first.setSeq(1);
        first.setNodeNo(300L);
        ChatMessage middle = botMessage(11L, SESSION_ID_STR);
        middle.setSeq(3);
        middle.setNodeNo(301L);
        ChatMessage current = botMessage(12L, SESSION_ID_STR);
        current.setSeq(5);
        current.setNodeNo(302L);
        ChatMessage priorBack =
                botMessageWithPayload(
                        "{\"source\":\"NODE_BACK\",\"fromNodeNo\":302,\"toNodeNo\":301}");
        priorBack.setSeq(6);
        priorBack.setNodeNo(301L);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(first, middle, current, priorBack), List.of());
        when(nodeMapper.findById(300L)).thenReturn(previous);
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(7);

        service.goBack(SESSION_ID, context);

        verify(sessionMapper).updateCurrentNode(eq(SESSION_ID_STR), eq(300L), eq("ACTIVE"), any());
    }

    @Test
    void goBackInSearchSessionRestoresPreviousSearchOptions() {
        ChatSession session = searchSession();
        String searchPayload =
                "{\"searchOptions\":[{\"crawlDocumentNo\":77,\"label\":\"Doc\","
                        + "\"matchedField\":\"CRAWL_DOCUMENT\",\"optionType\":\"DOCUMENT\"}],"
                        + "\"overflowOptions\":[]}";
        ChatMessage searchOptions = botMessageWithPayload(searchPayload);
        searchOptions.setChatMessageNo(10L);
        searchOptions.setSeq(2);
        ChatMessage answer = botMessageWithPayload("{\"source\":\"SEARCH_RESULT\"}");
        answer.setChatMessageNo(11L);
        answer.setSeq(4);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(searchOptions, answer), List.of(searchOptions, answer));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(5);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.goBack(SESSION_ID, context);

        verify(sessionMapper).restoreSearchSession(eq(SESSION_ID_STR), any());
        verify(sessionMapper)
                .insertSearchBackEvent(
                        eq(SESSION_ID_STR),
                        isNull(),
                        any(),
                        org.mockito.ArgumentMatchers.contains("\"restoredFromMessageNo\":10"));
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload()).contains("\"crawlDocumentNo\":77");
    }

    @Test
    void repeatedSearchBackWalksPastRestoredPageInsteadOfToggling() {
        ChatSession session = searchSession();
        ChatMessage initial = searchPayloadMessage(10L, 2, 101L, "확정지역 최초 결과", List.of(102L, 103L));
        ChatMessage pageTwo = searchPayloadMessage(20L, 3, 102L, "검색결과 더 보기 7건", List.of(103L));
        ChatMessage pageThree = searchPayloadMessage(30L, 4, 103L, "검색결과 더 보기 2건", List.of());
        ChatMessage restoredPageTwo =
                searchPayloadMessage(40L, 5, 102L, "검색결과 더 보기 7건", List.of(103L));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(
                        List.of(initial, pageTwo, pageThree, restoredPageTwo),
                        List.of(initial, pageTwo, pageThree, restoredPageTwo));
        when(sessionMapper.findSearchBackRestoredFromMessageNo(SESSION_ID_STR, 40L))
                .thenReturn(20L);
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(6);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.goBack(SESSION_ID, context);

        verify(sessionMapper)
                .insertSearchBackEvent(
                        eq(SESSION_ID_STR),
                        isNull(),
                        any(),
                        org.mockito.ArgumentMatchers.contains("\"restoredFromMessageNo\":10"));
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload())
                .contains("확정지역 최초 결과", "\"crawlDocumentNo\":101");
    }

    @Test
    void responseCannotGoBackPastInitialSearchPageAfterBackRestore() {
        ChatSession session = searchSession();
        ChatMessage unrelatedEarlierSearch =
                searchPayloadMessage(5L, 1, 77L, "이전 검색 결과", List.of(78L));
        ChatMessage initial = searchPayloadMessage(10L, 2, 101L, "확정지역 최초 결과", List.of(102L, 103L));
        ChatMessage restoredInitial =
                searchPayloadMessage(40L, 5, 101L, "확정지역 최초 결과", List.of(102L, 103L));
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(unrelatedEarlierSearch, initial, restoredInitial));
        when(sessionMapper.findSearchBackRestoredFromMessageNo(SESSION_ID_STR, 40L))
                .thenReturn(10L);

        var response = service.get(SESSION_ID, context);

        assertThat(response.canGoBack()).isFalse();
    }

    @Test
    void responseCanGoBackInSearchSessionWhenPreviousSearchOptionsExist() {
        ChatSession session = searchSession();
        String searchPayload =
                "{\"searchOptions\":[{\"crawlDocumentNo\":77,\"label\":\"Doc\","
                        + "\"matchedField\":\"CRAWL_DOCUMENT\",\"optionType\":\"DOCUMENT\"}],"
                        + "\"overflowOptions\":[]}";
        ChatMessage searchOptions = botMessageWithPayload(searchPayload);
        searchOptions.setSeq(2);
        ChatMessage answer = botMessageWithPayload("{\"source\":\"SEARCH_RESULT\"}");
        answer.setSeq(4);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(searchOptions, answer));

        var response = service.get(SESSION_ID, context);

        assertThat(response.canGoBack()).isTrue();
    }

    @Test
    void goBackImmediatelyAfterScenarioSwitchRestoresSearchOptions() {
        ChatSession session = activeSession();
        session.setScenarioNo(55L);
        session.setVersionNo(550L);
        session.setCurrentNodeNo(551L);
        String searchPayload =
                "{\"searchOptions\":[{\"scenarioNo\":55,\"label\":\"Scenario\","
                        + "\"matchedField\":\"SCENARIO\",\"optionType\":\"SCENARIO\"}],"
                        + "\"overflowOptions\":[]}";
        ChatMessage searchOptions = botMessageWithPayload(searchPayload);
        searchOptions.setChatMessageNo(20L);
        searchOptions.setSeq(2);
        ChatMessage startNode = botMessage(21L, SESSION_ID_STR);
        startNode.setSeq(4);
        startNode.setNodeNo(551L);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(List.of(searchOptions, startNode), List.of(searchOptions, startNode));
        when(sessionMapper.findLatestScenarioSwitchTriggerMessageNo(SESSION_ID_STR, 55L))
                .thenReturn(21L);
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(5);

        service.goBack(SESSION_ID, context);

        verify(sessionMapper).restoreSearchSession(eq(SESSION_ID_STR), any());
        verify(sessionMapper)
                .insertSearchBackEvent(
                        eq(SESSION_ID_STR),
                        eq(55L),
                        any(),
                        org.mockito.ArgumentMatchers.contains("\"restoredFromMessageNo\":20"));
    }

    @Test
    void goBackAfterScenarioSwitchUsesSearchPayloadBeforeLatestSwitchOnly() {
        ChatSession session = activeSession();
        session.setScenarioNo(55L);
        session.setVersionNo(550L);
        session.setCurrentNodeNo(551L);
        ChatMessage originSearch =
                botMessageWithPayload(
                        "{\"searchOptions\":[{\"scenarioNo\":55,\"label\":\"Origin\"}],"
                                + "\"overflowOptions\":[]}");
        originSearch.setChatMessageNo(10L);
        originSearch.setSeq(2);
        ChatMessage startNode = botMessage(21L, SESSION_ID_STR);
        startNode.setSeq(4);
        startNode.setNodeNo(551L);
        ChatMessage laterSearch =
                botMessageWithPayload(
                        "{\"searchOptions\":[{\"crawlDocumentNo\":99,\"label\":\"Later\"}],"
                                + "\"overflowOptions\":[]}");
        laterSearch.setChatMessageNo(30L);
        laterSearch.setSeq(6);
        when(sessionMapper.findById(SESSION_ID_STR)).thenReturn(Optional.of(session));
        when(sessionMapper.findLatestScenarioSwitchTriggerMessageNo(SESSION_ID_STR, 55L))
                .thenReturn(20L);
        when(messageMapper.findBySessionId(SESSION_ID_STR))
                .thenReturn(
                        List.of(originSearch, startNode, laterSearch),
                        List.of(originSearch, startNode, laterSearch));
        when(messageMapper.selectNextSeq(SESSION_ID_STR)).thenReturn(7);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        service.goBack(SESSION_ID, context);

        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getPayload())
                .contains("\"label\":\"Origin\"")
                .doesNotContain("\"label\":\"Later\"");
        verify(sessionMapper)
                .insertSearchBackEvent(
                        eq(SESSION_ID_STR),
                        eq(55L),
                        any(),
                        org.mockito.ArgumentMatchers.contains("\"restoredFromMessageNo\":10"));
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

        verify(searchService).search(eq(masked), eq("CHAT_FALLBACK"), eq(10), isNull(), any());
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

    private ChatSession searchSession() {
        ChatSession session = activeSession();
        session.setScenarioNo(null);
        session.setVersionNo(null);
        session.setCurrentNodeNo(null);
        session.setSessionType("SEARCH");
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

    private SearchResultItem searchResult(Long crawlDocumentNo, String title, double score) {
        SearchResultItem item = new SearchResultItem();
        item.setCrawlDocumentNo(crawlDocumentNo);
        item.setScenarioTitle(title);
        item.setMatchedField("CRAWL_DOCUMENT");
        item.setScore(score);
        item.setSnippet(title + " body");
        return item;
    }

    private SearchResultItem scenarioResult(Long scenarioNo, String title, double score) {
        SearchResultItem item = new SearchResultItem();
        item.setScenarioNo(scenarioNo);
        item.setScenarioTitle(title);
        item.setMatchedField("SCENARIO");
        item.setScore(score);
        return item;
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

    private ChatMessage botMessageWithPayload(String payload) {
        ChatMessage message = botMessage(50L, SESSION_ID_STR);
        message.setPayload(payload);
        return message;
    }

    private ChatMessage searchPayloadMessage(
            Long messageNo,
            int seq,
            Long crawlDocumentNo,
            String label,
            List<Long> overflowDocumentNos) {
        String overflow =
                overflowDocumentNos.stream()
                        .map(
                                id ->
                                        "{\"crawlDocumentNo\":"
                                                + id
                                                + ",\"label\":\"overflow "
                                                + id
                                                + "\",\"matchedField\":\"CRAWL_DOCUMENT\"}")
                        .collect(java.util.stream.Collectors.joining(","));
        String payload =
                "{\"searchOptions\":[{\"crawlDocumentNo\":"
                        + crawlDocumentNo
                        + ",\"label\":\""
                        + label
                        + "\",\"matchedField\":\"CRAWL_DOCUMENT\"}],"
                        + "\"overflowOptions\":["
                        + overflow
                        + "]}";
        ChatMessage message = botMessage(messageNo, SESSION_ID_STR);
        message.setSeq(seq);
        message.setPayload(payload);
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
