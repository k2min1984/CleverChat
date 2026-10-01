package kr.co.cleverchat.domain.crawl.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlCoverage;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlJob;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class CrawlJobRunnerTest {
    @org.junit.jupiter.api.BeforeEach
    void enableBrowserForBoardFixtures() {
        properties.setEnabled(true);
    }

    private final CrawlMapper crawlMapper = org.mockito.Mockito.mock(CrawlMapper.class);
    private final CrawlBrowserProperties properties = new CrawlBrowserProperties();
    private final BoardCrawler boardCrawler = org.mockito.Mockito.mock(BoardCrawler.class);
    private final ObjectProvider<BoardCrawler> boardCrawlerProvider =
            org.mockito.Mockito.mock(ObjectProvider.class);
    private final CrawlService crawlService = org.mockito.Mockito.mock(CrawlService.class);
    private final CrawlJobRunner runner =
            new CrawlJobRunner(
                    crawlMapper,
                    properties,
                    boardCrawlerProvider,
                    crawlService,
                    new KoreanMorphAnalyzer());

    @Test
    void regionalHeadquartersPageIsHandledAsBoard() {
        assertThat(
                        CrawlJobRunner.isBoardCrawlUrl(
                                "https://www.kepco.co.kr/home/about/locations/southseoul/headquarters.do?branchNo=11"))
                .isTrue();
    }

    @Test
    void pollDoesNothingWhenWorkerFlagIsOff() {
        org.springframework.test.util.ReflectionTestUtils.setField(runner, "workerEnabled", false);

        runner.poll();

        org.mockito.Mockito.verifyNoInteractions(crawlMapper);
    }

    @Test
    void pollProcessesStaticJobsWhileBrowserIsDisabled() {
        properties.setEnabled(false);
        CrawlTarget target = target();
        target.setUrl("https://example.com/");
        when(crawlMapper.claimNextJob(properties.getMaxAttempts())).thenReturn(job(), null);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(crawlService.discoverUrlsForJob(10L)).thenReturn(List.of(URI.create(target.getUrl())));
        CrawlRunLog run = new CrawlRunLog();
        run.setStatus("SUCCESS");
        when(crawlService.crawlStaticPageForJob(10L, URI.create(target.getUrl()), false))
                .thenReturn(new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse(run, null));
        runner.poll();
        verify(crawlMapper)
                .completeJob(
                        org.mockito.ArgumentMatchers.eq(5L),
                        org.mockito.ArgumentMatchers.eq("SUCCESS"),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failedStaticResponseDoesNotCountAsSuccess() {
        CrawlTarget target = target();
        target.setUrl("https://example.com/");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(crawlService.discoverUrlsForJob(10L)).thenReturn(List.of(URI.create(target.getUrl())));
        CrawlRunLog run = new CrawlRunLog();
        run.setStatus("FAILED");
        run.setFailureCode("EXPORT_ERROR");
        when(crawlService.crawlStaticPageForJob(10L, URI.create(target.getUrl()), false))
                .thenReturn(new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse(run, null));
        runner.runJob(job());
        verify(crawlMapper)
                .completeJob(
                        org.mockito.ArgumentMatchers.eq(5L),
                        org.mockito.ArgumentMatchers.eq("FAILED"),
                        org.mockito.ArgumentMatchers.contains("1 failed"));
    }

    @Test
    void runJobStoresDocumentsWithTokensAndCoverage() {
        CrawlTarget target = target();
        CrawlJob job = job();
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(boardCrawler);
        when(boardCrawler.crawl("https://example.com/home/boardList.do"))
                .thenReturn(
                        new BrowserCrawlResult(
                                List.of(
                                        new BrowserCrawlPage(
                                                "https://example.com/detail",
                                                "전기 요금",
                                                "전기 요금 안내 본문",
                                                200)),
                                new BrowserCrawlCoverage(1, 1, 1, 0, false)));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlDocument document = invocation.getArgument(0);
                            document.setCrawlDocumentNo(99L);
                            return null;
                        })
                .when(crawlMapper)
                .insertDocument(any(CrawlDocument.class));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlRunLog runLog = invocation.getArgument(0);
                            runLog.setCrawlRunLogNo(7L);
                            return null;
                        })
                .when(crawlMapper)
                .insertRunLog(any(CrawlRunLog.class));
        ArgumentCaptor<CrawlDocument> documentCaptor = ArgumentCaptor.forClass(CrawlDocument.class);
        ArgumentCaptor<CrawlCoverage> coverageCaptor = ArgumentCaptor.forClass(CrawlCoverage.class);

        runner.runJob(job);

        verify(crawlMapper).insertDocument(documentCaptor.capture());
        verify(crawlMapper).insertCoverage(coverageCaptor.capture());
        verify(crawlMapper)
                .completeJob(5L, "SUCCESS", "Browser crawl stored 1 document(s), 0 duplicate(s).");
        assertThat(documentCaptor.getValue().getContentTokens()).isNotBlank();
        assertThat(coverageCaptor.getValue().getListPages()).isEqualTo(1);
        assertThat(coverageCaptor.getValue().getDetailsFetched()).isEqualTo(1);
    }

    @Test
    void runJobDoesNotReportSuccessWhenBoardCrawlWasTruncated() {
        CrawlTarget target = target();
        CrawlJob job = job();
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(boardCrawler);
        when(boardCrawler.crawl("https://example.com/home/boardList.do"))
                .thenReturn(
                        new BrowserCrawlResult(
                                List.of(
                                        new BrowserCrawlPage(
                                                "https://example.com/detail",
                                                "title",
                                                "content",
                                                200)),
                                new BrowserCrawlCoverage(20, 240, 240, 0, true)));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlDocument document = invocation.getArgument(0);
                            document.setCrawlDocumentNo(99L);
                            return null;
                        })
                .when(crawlMapper)
                .insertDocument(any(CrawlDocument.class));

        runner.runJob(job);

        verify(crawlMapper)
                .completeJob(
                        5L,
                        "FAILED",
                        "Browser crawl stored 1 document(s), 0 duplicate(s). Crawl incomplete: 0 detail failure(s), truncated=true.");
    }

    @Test
    void runJobTreatsUrlUpdateContentHashCollisionAsDuplicate() {
        CrawlTarget target = target();
        CrawlJob job = job();
        CrawlDocument existingByUrl = new CrawlDocument();
        existingByUrl.setCrawlDocumentNo(101L);
        CrawlDocument existingByContent = new CrawlDocument();
        existingByContent.setCrawlDocumentNo(202L);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(boardCrawler);
        when(boardCrawler.crawl("https://example.com/home/boardList.do"))
                .thenReturn(
                        new BrowserCrawlResult(
                                List.of(
                                        new BrowserCrawlPage(
                                                "https://example.com/detail",
                                                "title",
                                                "same content",
                                                200)),
                                new BrowserCrawlCoverage(1, 1, 1, 0, false)));
        when(crawlMapper.findDocumentByTargetAndUrlHash(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(existingByUrl);
        when(crawlMapper.findDocumentByTargetAndHash(
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(existingByContent);

        runner.runJob(job);

        org.mockito.Mockito.verify(crawlMapper, org.mockito.Mockito.never())
                .updateDocument(any(CrawlDocument.class));
        verify(crawlMapper)
                .completeJob(5L, "SUCCESS", "Browser crawl stored 0 document(s), 1 duplicate(s).");
    }

    @Test
    void runJobUsesStaticCrawlerForNonBoardUrl() {
        CrawlTarget target = target();
        target.setUrl("https://example.com/");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        org.mockito.Mockito.lenient()
                .when(
                        crawlService.crawlStaticPageForJob(
                                org.mockito.ArgumentMatchers.eq(10L),
                                any(URI.class),
                                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(
                        invocation -> {
                            CrawlRunLog log = new CrawlRunLog();
                            log.setStatus("SUCCESS");
                            return new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse(
                                    log, null);
                        });
        when(crawlService.discoverUrlsForJob(10L))
                .thenReturn(List.of(URI.create("https://example.com/")));

        runner.runJob(job());

        verify(crawlService).crawlStaticPageForJob(10L, URI.create("https://example.com/"), false);
        verify(crawlMapper)
                .completeJob(
                        5L,
                        "SUCCESS",
                        "Hybrid crawl visited 1 URL(s), 0 board list(s): static 1 success, 0 duplicate; board 0 success, 0 duplicate; 0 failed.");
    }

    @Test
    void runJobUsesBrowserCrawlerForDiscoveredBoardList() {
        CrawlTarget target = target();
        target.setUrl("https://example.com/");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(boardCrawler);
        org.mockito.Mockito.lenient()
                .when(
                        crawlService.crawlStaticPageForJob(
                                org.mockito.ArgumentMatchers.eq(10L),
                                any(URI.class),
                                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(
                        invocation -> {
                            CrawlRunLog log = new CrawlRunLog();
                            log.setStatus("SUCCESS");
                            return new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse(
                                    log, null);
                        });
        when(crawlService.discoverUrlsForJob(10L))
                .thenReturn(
                        List.of(
                                URI.create("https://example.com/"),
                                URI.create("https://example.com/home/boardList.do")));
        when(boardCrawler.crawl("https://example.com/home/boardList.do"))
                .thenReturn(
                        new BrowserCrawlResult(
                                List.of(
                                        new BrowserCrawlPage(
                                                "https://example.com/detail",
                                                "title",
                                                "content",
                                                200)),
                                new BrowserCrawlCoverage(1, 1, 1, 0, false)));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlDocument document = invocation.getArgument(0);
                            document.setCrawlDocumentNo(99L);
                            return null;
                        })
                .when(crawlMapper)
                .insertDocument(any(CrawlDocument.class));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlRunLog runLog = invocation.getArgument(0);
                            runLog.setCrawlRunLogNo(7L);
                            return null;
                        })
                .when(crawlMapper)
                .insertRunLog(any(CrawlRunLog.class));

        runner.runJob(job());

        verify(crawlService).crawlStaticPageForJob(10L, URI.create("https://example.com/"), false);
        verify(boardCrawler).crawl("https://example.com/home/boardList.do");
        verify(crawlMapper).insertCoverage(any(CrawlCoverage.class));
        verify(crawlMapper)
                .completeJob(
                        5L,
                        "SUCCESS",
                        "Hybrid crawl visited 2 URL(s), 1 board list(s): static 1 success, 0 duplicate; board 1 success, 0 duplicate; 0 failed.");
    }

    @Test
    void runJobMarksHybridCrawlFailedWhenAnyDiscoveredUrlFails() {
        properties.setBoardRetryDelayMs(0);
        CrawlTarget target = target();
        target.setUrl("https://example.com/");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(boardCrawler);
        org.mockito.Mockito.lenient()
                .when(
                        crawlService.crawlStaticPageForJob(
                                org.mockito.ArgumentMatchers.eq(10L),
                                any(URI.class),
                                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(
                        invocation -> {
                            CrawlRunLog log = new CrawlRunLog();
                            log.setStatus("SUCCESS");
                            return new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse(
                                    log, null);
                        });
        when(crawlService.discoverUrlsForJob(10L))
                .thenReturn(
                        List.of(
                                URI.create("https://example.com/"),
                                URI.create("https://example.com/home/boardList.do")));
        when(boardCrawler.crawl("https://example.com/home/boardList.do"))
                .thenThrow(new IllegalStateException("unsupported board layout"));

        runner.runJob(job());

        verify(boardCrawler, times(6)).crawl("https://example.com/home/boardList.do");
        verify(crawlMapper)
                .completeJob(
                        5L,
                        "FAILED",
                        "Hybrid crawl visited 2 URL(s), 1 board list(s): static 1 success, 0 duplicate; board 0 success, 0 duplicate; 1 failed.");
    }

    @Test
    void runJobMarksFailedWhenCrawlerIsUnavailable() {
        when(crawlMapper.findTargetById(10L)).thenReturn(target());
        when(boardCrawlerProvider.getIfAvailable()).thenReturn(null);
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        runner.runJob(job());

        verify(crawlMapper).insertRunLog(runCaptor.capture());
        verify(crawlMapper).completeJob(5L, "FAILED", "Browser crawler is disabled.");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runCaptor.getValue().getFailureCode()).isEqualTo("SYSTEM_ERROR");
    }

    private CrawlTarget target() {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(10L);
        target.setUrl("https://example.com/home/boardList.do");
        target.setUseYn("Y");
        return target;
    }

    private CrawlJob job() {
        CrawlJob job = new CrawlJob();
        job.setCrawlJobNo(5L);
        job.setTargetNo(10L);
        job.setStatus("RUNNING");
        job.setTriggerType("MANUAL");
        return job;
    }
}
