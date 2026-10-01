package kr.co.cleverchat.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SearchOptionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SessionResponse;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService.ChatRequestContext;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@Tag("integration")
@org.springframework.test.annotation.DirtiesContext
class SearchRelevanceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("cleverchat")
                    .withUsername("cleverchat")
                    .withPassword("cleverchat");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("cleverchat.crawl.browser.enabled", () -> false);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SearchService search;
    @Autowired ChatRuntimeService chat;
    @Autowired KoreanMorphAnalyzer analyzer;
    private Long customer;
    private Long answer;
    private Long esg;
    private Long esgAnswer;
    private ChatRequestContext context;

    @BeforeEach
    void seed() {
        // This database belongs to the disposable Testcontainer, never the development server.
        jdbc.execute("TRUNCATE tb_chat_session, tb_scenario_category, tb_crawl_target CASCADE");
        context = new ChatRequestContext(UUID.randomUUID(), "127.0.0.1", "search-relevance-test");
        customer = scenario("고객소통", "전기요금 관련 고객업무");
        answer =
                node(
                        customer,
                        "신청·요금조회·납부",
                        "전기요금 조회와 납부는 한전ON에서 처리합니다. 고객번호로 조회하고 자동이체를 신청할 수 있습니다.");
        esg = scenario("ESG경영", "전기요금 정책을 포함한 경영 성과");
        esgAnswer = node(esg, "사회공헌", "취약계층 전기요금 지원 실적을 소개합니다.");
        jdbc.update(
                "INSERT INTO tb_scenario_keyword (scenario_no, keyword) VALUES (?, '전기요금')", esg);
        Long business = scenario("사업분야", "전력 사업");
        node(business, "전기 설비", "송배전 설비 점검에 대한 설명입니다.");
        document("전기요금 납부 방법", "전기요금은 계좌 자동이체 또는 신용카드로 납부할 수 있습니다.");
        document("전기 설비 점검", "전기 설비 유지보수 설명입니다.");
        document("ESG 핵심성과", "사회공헌 실적으로 전기요금 지원 내역을 보고합니다.");
    }

    @Test
    void billSearchOffersSpecificAnswerAndDocumentsInsteadOfBroadMenus() {
        for (String query : List.of("전기요금", "전기 요금", "전기요금 알려주세요", "전기요금에 대해 설명해 주세요")) {
            var session = chat.startWithText(query, context);
            assertThat(session.searchOptions())
                    .extracting(SearchOptionResponse::label)
                    .contains("신청·요금조회·납부", "전기요금 납부 방법")
                    .doesNotContain("고객소통", "ESG경영", "사업분야", "사회공헌", "전기 설비 점검", "ESG 핵심성과");
            assertThat(session.searchOptions())
                    .filteredOn(o -> answer.equals(o.scenarioNodeNo()))
                    .hasSize(1);
            assertThat(session.searchMoreCount()).isZero();
        }
    }

    @Test
    void selectionOpensMatchedAnswerInSameSessionAndBackRestoresSearch() {
        var searchSession = chat.startWithText("전기요금", context);
        var selected =
                chat.selectSearchResult(searchSession.sessionId(), null, customer, answer, context);
        assertThat(selected.sessionId()).isEqualTo(searchSession.sessionId());
        assertThat(selected.currentNodeId()).isEqualTo(answer);
        assertThat(selected.messages().get(selected.messages().size() - 1).content())
                .contains("한전ON", "고객번호", "자동이체");
        var restored = chat.goBack(searchSession.sessionId(), context);
        assertThat(restored.searchOptions()).isEqualTo(searchSession.searchOptions());
    }

    @Test
    void documentOpenedDuringScenarioReturnsToItsSearchBeforeTheScenario() {
        var beforeSearch = beginAtAnswer();
        var results = chat.freeText(beforeSearch.sessionId(), "전기요금", context);
        var doc =
                results.searchOptions().stream()
                        .filter(o -> o.crawlDocumentNo() != null)
                        .findFirst()
                        .orElseThrow();
        var opened =
                chat.selectSearchResult(
                        results.sessionId(),
                        doc.crawlDocumentNo(),
                        null,
                        null,
                        results.messages().get(results.messages().size() - 1).id(),
                        context);
        assertThat(opened.backTargetType()).isEqualTo("SEARCH_RESULTS");
        var back = chat.goBack(results.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(results.searchOptions());
        assertThat(back.backTargetType()).isEqualTo("SCENARIO");
        back = chat.goBack(results.sessionId(), context);
        assertThat(back.currentNodeId()).isEqualTo(answer);
        back = chat.goBack(results.sessionId(), context);
        assertThat(back.currentNodeId()).isNotEqualTo(answer);
        assertThat(back.canGoBack()).isFalse();
        assertThatThrownBy(() -> chat.goBack(results.sessionId(), context))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void revisitingSameAnswerFromSearchDoesNotSkipTheSearchListOnBack() {
        var beforeSearch = beginAtAnswer();
        var results = chat.freeText(beforeSearch.sessionId(), "전기요금", context);
        chat.selectSearchResult(results.sessionId(), null, customer, answer, context);
        var back = chat.goBack(results.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(results.searchOptions());
        back = chat.goBack(results.sessionId(), context);
        assertThat(back.currentNodeId()).isEqualTo(beforeSearch.currentNodeId());
        back = chat.goBack(results.sessionId(), context);
        assertThat(back.canGoBack()).isFalse();
    }

    @Test
    void clickingOlderSearchBubbleReturnsToThatExactListEvenWhenDocumentRepeats() {
        document("가스요금 납부 방법", "가스요금 납부에 대한 안내입니다.");
        var first = chat.startWithText("전기요금", context);
        var second = chat.freeText(first.sessionId(), "요금", context);
        assertThat(second.searchOptions()).isNotEqualTo(first.searchOptions());
        var doc =
                first.searchOptions().stream()
                        .filter(o -> o.crawlDocumentNo() != null)
                        .findFirst()
                        .orElseThrow();
        Long sourceId = first.messages().get(first.messages().size() - 1).id();
        chat.selectSearchResult(
                first.sessionId(), doc.crawlDocumentNo(), null, null, sourceId, context);
        var back = chat.goBack(first.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(first.searchOptions());
        assertThat(back.canGoBack()).isFalse();
        assertThatThrownBy(
                        () ->
                                chat.selectSearchResult(
                                        first.sessionId(),
                                        doc.crawlDocumentNo(),
                                        null,
                                        null,
                                        first.messages().get(0).id(),
                                        context))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void moreDocumentBackWalksThroughMoreAndInitialPageWithoutLooping() {
        for (int index = 0; index < 7; index++) {
            document("전기요금 납부 안내 " + index, "전기요금 납부 방법을 설명합니다.");
        }
        var first = chat.startWithText("전기요금", context);
        assertThat(first.searchMoreCount()).isPositive();
        var more = chat.searchMore(first.sessionId(), context);
        var doc =
                more.searchOptions().stream()
                        .filter(o -> o.crawlDocumentNo() != null)
                        .findFirst()
                        .orElseThrow();
        chat.selectSearchResult(
                first.sessionId(),
                doc.crawlDocumentNo(),
                null,
                null,
                more.messages().get(more.messages().size() - 1).id(),
                context);
        var back = chat.goBack(first.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(more.searchOptions());
        back = chat.goBack(first.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(first.searchOptions());
        assertThat(back.searchMoreCount()).isEqualTo(first.searchMoreCount());
        assertThat(back.canGoBack()).isFalse();
    }

    @Test
    void existingConversationWithoutNavigationMetadataAlsoRestoresSearchBeforeScenario() {
        var beforeSearch = beginAtAnswer();
        var results = chat.freeText(beforeSearch.sessionId(), "전기요금", context);
        var doc =
                results.searchOptions().stream()
                        .filter(o -> o.crawlDocumentNo() != null)
                        .findFirst()
                        .orElseThrow();
        chat.selectSearchResult(results.sessionId(), doc.crawlDocumentNo(), null, context);
        jdbc.update(
                "UPDATE tb_chat_message SET payload = payload - 'navigation' WHERE session_no = ?::uuid",
                results.sessionId().toString());
        var back = chat.goBack(results.sessionId(), context);
        assertThat(back.searchOptions()).isEqualTo(results.searchOptions());
        back = chat.goBack(results.sessionId(), context);
        assertThat(back.currentNodeId()).isEqualTo(beforeSearch.currentNodeId());
    }

    private SessionResponse beginAtAnswer() {
        var root = chat.start(customer, context);
        Long option =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_node_option (node_no, next_node_no, label) VALUES (?, ?, '요금 안내') RETURNING scenario_node_option_no",
                        Long.class,
                        root.currentNodeId(),
                        answer);
        return chat.selectOption(
                root.sessionId(),
                option,
                root.currentNodeId(),
                root.messages().get(root.messages().size() - 1).id(),
                context);
    }

    @Test
    void selectionRejectsUnOfferedNodeAndMissingNodeIdentity() {
        var session = chat.startWithText("전기요금", context);
        assertThatThrownBy(
                        () ->
                                chat.selectSearchResult(
                                        session.sessionId(), null, customer, esgAnswer, context))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () ->
                                chat.selectSearchResult(
                                        session.sessionId(), null, customer, null, context))
                .isInstanceOf(BusinessException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM tb_chat_failure WHERE session_no = ?::uuid AND reason = 'INVALID_OPTION' AND detail->>'selectionType' = 'SEARCH_RESULT'",
                                Integer.class,
                                session.sessionId().toString()))
                .isEqualTo(2);
    }

    @Test
    void selectionRejectsNodeFromReplacedPublishedVersion() {
        var session = chat.startWithText("전기요금", context);
        Long oldVersion =
                jdbc.queryForObject(
                        "SELECT active_version_no FROM tb_scenario WHERE scenario_no = ?",
                        Long.class,
                        customer);
        jdbc.update(
                "UPDATE tb_scenario_version SET status = 'ARCHIVED' WHERE scenario_version_no = ?",
                oldVersion);
        Long version =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_version (scenario_no, version_no, status, frst_regr_empno) VALUES (?, 2, 'PUBLISHED', 'test') RETURNING scenario_version_no",
                        Long.class,
                        customer);
        Long start =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_node (version_no, node_key, node_type, title) VALUES (?, 'new-start', 'QUESTION', '새 상담') RETURNING scenario_node_no",
                        Long.class,
                        version);
        jdbc.update(
                "UPDATE tb_scenario_version SET start_node_no = ? WHERE scenario_version_no = ?",
                start,
                version);
        jdbc.update(
                "UPDATE tb_scenario SET active_version_no = ? WHERE scenario_no = ?",
                version,
                customer);
        assertThatThrownBy(
                        () ->
                                chat.selectSearchResult(
                                        session.sessionId(), null, customer, answer, context))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void specificSubjectTermsMustAllMatchAndTopicNamesStillWork() {
        var payment = search.search("전기요금 납부", "ADMIN_TEST", 20, null, null);
        assertThat(payment.results())
                .extracting(SearchResultItem::getScenarioTitle)
                .contains("신청·요금조회·납부", "전기요금 납부 방법")
                .doesNotContain("전기 설비", "사회공헌", "ESG경영", "ESG 핵심성과");
        assertThat(search.search("ESG경영", "ADMIN_TEST", 20, null, null).results())
                .anyMatch(
                        item ->
                                esg.equals(item.getScenarioNo())
                                        && item.getScenarioNodeNo() == null);
        assertThat(search.search("전기요금 우주왕복선", "ADMIN_TEST", 20, null, null).results()).isEmpty();
    }

    @Test
    void inactiveAndDraftContentIsExcluded() {
        jdbc.update("UPDATE tb_scenario SET status = 'INACTIVE' WHERE scenario_no = ?", customer);
        Long draft = scenario("비공개", "초안");
        node(draft, "전기요금 초안", "전기요금 납부 초안입니다.");
        jdbc.update("UPDATE tb_scenario_version SET status = 'DRAFT' WHERE scenario_no = ?", draft);
        assertThat(search.search("전기요금", "ADMIN_TEST", 20, null, null).results())
                .noneMatch(
                        item ->
                                customer.equals(item.getScenarioNo())
                                        || draft.equals(item.getScenarioNo()));
    }

    private Long scenario(String title, String description) {
        Long category =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_category (name) VALUES (?) RETURNING scenario_category_no",
                        Long.class,
                        title);
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario (category_no, title, description, status) VALUES (?, ?, ?, 'ACTIVE') RETURNING scenario_no",
                        Long.class,
                        category,
                        title,
                        description);
        Long version =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_version (scenario_no, version_no, status, frst_regr_empno) VALUES (?, 1, 'PUBLISHED', 'test') RETURNING scenario_version_no",
                        Long.class,
                        id);
        Long start =
                jdbc.queryForObject(
                        "INSERT INTO tb_scenario_node (version_no, node_key, node_type, title, content) VALUES (?, 'start', 'QUESTION', ?, ?) RETURNING scenario_node_no",
                        Long.class,
                        version,
                        title,
                        description);
        jdbc.update(
                "UPDATE tb_scenario_version SET start_node_no = ? WHERE scenario_version_no = ?",
                start,
                version);
        jdbc.update(
                "UPDATE tb_scenario SET active_version_no = ? WHERE scenario_no = ?", version, id);
        return id;
    }

    private Long node(Long scenario, String title, String body) {
        return jdbc.queryForObject(
                "INSERT INTO tb_scenario_node (version_no, node_key, node_type, title, content) SELECT active_version_no, ?, 'ANSWER', ?, ? FROM tb_scenario WHERE scenario_no = ? RETURNING scenario_node_no",
                Long.class,
                UUID.randomUUID().toString(),
                title,
                body,
                scenario);
    }

    private Long document(String title, String body) {
        String url = "https://example.com/" + UUID.randomUUID();
        Long target =
                jdbc.queryForObject(
                        "INSERT INTO tb_crawl_target (url) VALUES (?) RETURNING crawl_target_no",
                        Long.class,
                        url);
        return jdbc.queryForObject(
                "INSERT INTO tb_crawl_document (target_no, url, title, content, content_tokens, url_hash, content_hash, status) VALUES (?, ?, ?, ?, ?, repeat('a', 64), repeat('b', 64), 'SUCCESS') RETURNING crawl_document_no",
                Long.class,
                target,
                url,
                title,
                body,
                analyzer.tokenize(title + " " + body));
    }
}
