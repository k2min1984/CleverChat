package kr.co.cleverchat.domain.crawl.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        properties = {
            "cleverchat.crawl.worker.enabled=false",
            "cleverchat.crawl.browser.enabled=false",
            "cleverchat.crawl.max-discovered-pages=1",
            "cleverchat.crawl.url-policy.allowed-hosts=127.0.0.1"
        })
@ActiveProfiles("dev")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@Tag("integration")
class CrawlPipelineIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("cleverchat")
                    .withUsername("cleverchat")
                    .withPassword("cleverchat");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired CrawlMapper mapper;
    @Autowired CrawlService service;
    @Autowired CrawlJobRunner runner;
    @Autowired SearchService search;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @TempDir Path output;
    private HttpServer server;
    private CrawlTarget target;
    private final AtomicReference<String> body = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        body.set("<title>전기요금 안내</title><main><h1>전기요금</h1><p>온라인 납부 안내입니다.</p></main>");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/robots.txt",
                exchange -> {
                    byte[] bytes =
                            "User-agent: *\nDisallow: /blocked".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var stream = exchange.getResponseBody()) {
                        stream.write(bytes);
                    }
                });
        server.createContext(
                "/article",
                exchange -> {
                    byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var stream = exchange.getResponseBody()) {
                        stream.write(bytes);
                    }
                });
        server.start();
        target = new CrawlTarget();
        target.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/article");
        target.setLabel("JSON integration fixture");
        target.setUseYn("Y");
        target.setScheduleMode("INTERVAL");
        target.setScheduleIntervalMinutes(1440);
        target.setJsonExportEnabled(true);
        target.setJsonExportDirectory(output.toString());
        mapper.insertTarget(target);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
        if (target != null && target.getCrawlTargetNo() != null) {
            jdbc.update(
                    "DELETE FROM tb_crawl_target WHERE crawl_target_no = ?",
                    target.getCrawlTargetNo());
        }
    }

    @Test
    void queuedHttpCrawlPersistsJsonAndSearchableContentAndRefreshesSameUrl() throws Exception {
        runJob();
        var first = mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1).get(0);
        assertThat(first.getStatus()).isEqualTo("SUCCESS");
        assertThat(first.getExportStatus()).isEqualTo("SUCCESS");
        var json = objectMapper.readTree(Files.readString(Path.of(first.getExportPath())));
        assertThat(json.get("content").asText()).contains("전기요금", "온라인 납부");
        assertThat(json.get("url").asText()).isEqualTo(target.getUrl());
        Path artifacts = Path.of("target/qa-artifacts");
        Files.createDirectories(artifacts);
        Files.writeString(
                artifacts.resolve("crawl-sample.json"),
                Files.readString(Path.of(first.getExportPath())));
        assertThat(search.search("전기요금", "API", 20, null, null).results())
                .anyMatch(item -> first.getDocumentNo().equals(item.getCrawlDocumentNo()));

        runJob();
        var duplicate = mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1).get(0);
        assertThat(duplicate.getStatus()).isEqualTo("DUPLICATE");
        assertThat(duplicate.getExportPath()).isNotEqualTo(first.getExportPath());
        assertThat(mapper.findJobs(target.getCrawlTargetNo(), 1).get(0).getStatus())
                .isEqualTo("SUCCESS");

        body.set("<title>전기요금 변경 안내</title><main>자동이체 신청 안내로 변경했습니다.</main>");
        runJob();
        var documents = mapper.findDocuments(target.getCrawlTargetNo(), 100);
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getContent()).contains("자동이체 신청");
        assertThat(documents.get(0).getCrawlDocumentNo()).isEqualTo(first.getDocumentNo());
        try (var paths = Files.walk(output)) {
            assertThat(paths.filter(path -> path.toString().endsWith(".json")).toList()).hasSize(3);
        }
    }

    @Test
    void exportFailureKeepsFetchedDataAndRecordsFailure() throws Exception {
        Path file = output.resolve("not-a-directory");
        Files.writeString(file, "keep");
        target.setJsonExportDirectory(file.toString());
        mapper.updateTarget(target);
        runJob();
        var run = mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1).get(0);
        assertThat(run.getFailureCode()).isEqualTo("EXPORT_ERROR");
        assertThat(run.getExportStatus()).isEqualTo("FAILED");
        assertThat(mapper.findDocuments(target.getCrawlTargetNo(), 100)).hasSize(1);
        assertThat(mapper.findJobs(target.getCrawlTargetNo(), 1).get(0).getStatus())
                .isEqualTo("FAILED");
        assertThat(Files.readString(file)).isEqualTo("keep");
    }

    @Test
    void robotsFailureLogSurvivesServiceTransaction() {
        URI blocked = URI.create(target.getUrl().replace("/article", "/blocked"));
        assertThatThrownBy(
                        () ->
                                service.crawlStaticPageForJob(
                                        target.getCrawlTargetNo(), blocked, true))
                .isInstanceOf(BusinessException.class);
        assertThat(mapper.findRunLogs(target.getCrawlTargetNo(), "FAILED", "ROBOTS_BLOCKED", 10))
                .hasSize(1);
        assertThat(mapper.findDocuments(target.getCrawlTargetNo(), 100)).isEmpty();
    }

    @Test
    void disabledExportProducesNoFilesAndSettingsRoundTrip() throws Exception {
        target.setJsonExportEnabled(false);
        mapper.updateTarget(target);
        assertThat(mapper.findTargetById(target.getCrawlTargetNo()).isJsonExportEnabled())
                .isFalse();
        runJob();
        assertThat(
                        mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1)
                                .get(0)
                                .getExportStatus())
                .isEqualTo("DISABLED");
        try (var paths = Files.list(output)) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    @Test
    void simultaneousRequestsOnlyEnqueueOneActiveJob() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> enqueue =
                    () -> {
                        ready.countDown();
                        start.await();
                        return service.enqueueRun(target.getCrawlTargetNo(), "MANUAL", null)
                                .enqueued();
                    };
            var first = executor.submit(enqueue);
            var second = executor.submit(enqueue);
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(
                            List.of(
                                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(mapper.findJobs(target.getCrawlTargetNo(), 100)).hasSize(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void browserResultsAlsoExportAndRepeatedBoardCompletesSuccessfully() throws Exception {
        target.setUrl(target.getUrl().replace("/article", "/boardList.do"));
        mapper.updateTarget(target);
        BoardCrawler board = org.mockito.Mockito.mock(BoardCrawler.class);
        org.springframework.beans.factory.ObjectProvider<BoardCrawler> provider =
                org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(board);
        org.mockito.Mockito.when(board.crawl(target.getUrl()))
                .thenReturn(
                        new BrowserCrawlResult(
                                List.of(
                                        new BrowserCrawlPage(
                                                target.getUrl().replace("boardList", "boardView"),
                                                "게시판 안내",
                                                "게시판 수집 본문입니다.",
                                                200)),
                                new BrowserCrawlCoverage(1, 1, 1, 0, false)));
        var browserProperties = new CrawlBrowserProperties();
        browserProperties.setEnabled(true);
        CrawlJobRunner browserRunner =
                new CrawlJobRunner(
                        mapper,
                        browserProperties,
                        provider,
                        service,
                        new kr.co.cleverchat.common.search.KoreanMorphAnalyzer());
        for (int i = 0; i < 2; i++) {
            service.enqueueRun(target.getCrawlTargetNo(), "MANUAL", null);
            browserRunner.runJob(mapper.claimNextJob(2));
        }
        assertThat(mapper.findDocuments(target.getCrawlTargetNo(), 100)).hasSize(1);
        assertThat(mapper.findJobs(target.getCrawlTargetNo(), 2))
                .allMatch(job -> "SUCCESS".equals(job.getStatus()));
        assertThat(mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1).get(0).getStatus())
                .isEqualTo("DUPLICATE");
        try (var paths = Files.walk(output)) {
            var files = paths.filter(path -> path.toString().endsWith(".json")).toList();
            assertThat(files).hasSize(2);
            assertThat(
                            objectMapper
                                    .readTree(Files.readString(files.get(0)))
                                    .get("content")
                                    .asText())
                    .isEqualTo("게시판 수집 본문입니다.");
        }
    }

    @Test
    void scheduledRetentionWorksWithoutAdminAndPreservesJsonArchive() throws Exception {
        runJob();
        var run = mapper.findRunLogs(target.getCrawlTargetNo(), null, null, 1).get(0);
        jdbc.update(
                "UPDATE tb_crawl_document SET status = 'FAILED', frst_reg_dt = now() - interval '100 days' WHERE target_no = ?",
                target.getCrawlTargetNo());
        assertThatThrownBy(() -> service.deleteExpired(null, null, false))
                .isInstanceOf(BusinessException.class);
        assertThat(service.deleteExpiredScheduled().deletedDocuments()).isEqualTo(1);
        assertThat(Path.of(run.getExportPath())).exists();
    }

    @Test
    void adminFormRendersAndPersistsExportOptionsWithCsrf() throws Exception {
        var session = new org.springframework.mock.web.MockHttpSession();
        new kr.co.cleverchat.domain.auth.security.CsrfTokenIssuer().issue(session);
        session.setAttribute(
                kr.co.cleverchat.domain.auth.security.AdminSession.SESSION_KEY,
                new kr.co.cleverchat.domain.auth.security.AdminSession(
                        1L,
                        "qa-admin",
                        "QA",
                        java.util.Set.of("ADMIN"),
                        false,
                        java.time.LocalDateTime.now()));
        var response =
                mvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .get(
                                                "/admin/crawl-targets/"
                                                        + target.getCrawlTargetNo()
                                                        + "/edit")
                                        .session(session))
                        .andExpect(
                                org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                        .status()
                                        .isOk())
                        .andReturn()
                        .getResponse();
        var html = org.jsoup.Jsoup.parse(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(html.select("input[name=useYn]")).hasSize(1);
        assertThat(html.select("input[name=scheduleEnabled]")).hasSize(1);
        assertThat(html.selectFirst("#jsonExportEnabled").hasAttr("checked")).isTrue();
        assertThat(html.selectFirst("#jsonExportDirectory").val()).isEqualTo(output.toString());
        var csrf = html.selectFirst("input[name=csrfToken]");
        var csrfFormId = html.selectFirst("input[name=csrfFormId]");
        assertThat(csrf).isNotNull();
        assertThat(csrfFormId).isNotNull();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/admin/crawl-targets/" + target.getCrawlTargetNo())
                                .session(session)
                                .param(csrf.attr("name"), csrf.val())
                                .param(csrfFormId.attr("name"), csrfFormId.val())
                                .param("url", target.getUrl())
                                .param("useYn", "Y")
                                .param("scheduleMode", "INTERVAL")
                                .param("scheduleIntervalMinutes", "1440")
                                .param("jsonExportEnabled", "true")
                                .param("jsonExportDirectory", output.resolve("chosen").toString()))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .is3xxRedirection());
        var saved = mapper.findTargetById(target.getCrawlTargetNo());
        assertThat(saved.getUseYn()).isEqualTo("Y");
        assertThat(saved.isJsonExportEnabled()).isTrue();
        assertThat(saved.getJsonExportDirectory()).isEqualTo(output.resolve("chosen").toString());
    }

    private void runJob() {
        assertThat(service.enqueueRun(target.getCrawlTargetNo(), "MANUAL", null).enqueued())
                .isTrue();
        var job = mapper.claimNextJob(2);
        assertThat(job).isNotNull();
        runner.runJob(job);
    }
}
