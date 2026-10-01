package kr.co.cleverchat.domain.crawl.browser;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlCoverage;
import kr.co.cleverchat.domain.crawl.model.CrawlJob;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlDocumentStore;
import kr.co.cleverchat.domain.crawl.service.CrawlJsonExporter;
import kr.co.cleverchat.domain.crawl.service.CrawlJsonExporter.ExportResult;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlJobRunner {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private static final Logger log = LoggerFactory.getLogger(CrawlJobRunner.class);

    private final CrawlMapper crawlMapper;
    private final CrawlBrowserProperties properties;
    private final ObjectProvider<BoardCrawler> boardCrawlerProvider;
    private final CrawlService crawlService;
    private final CrawlDocumentStore documentStore;

    @Value("${cleverchat.crawl.worker.enabled:true}")
    private boolean workerEnabled = true;

    private final AtomicInteger activeJobs = new AtomicInteger();

    public CrawlJobRunner(
            CrawlMapper crawlMapper,
            CrawlBrowserProperties properties,
            ObjectProvider<BoardCrawler> boardCrawlerProvider,
            CrawlService crawlService,
            KoreanMorphAnalyzer morphAnalyzer) {
        this(
                crawlMapper,
                properties,
                boardCrawlerProvider,
                crawlService,
                morphAnalyzer,
                new CrawlDocumentStore(
                        crawlMapper,
                        morphAnalyzer,
                        new CrawlJsonExporter(new com.fasterxml.jackson.databind.ObjectMapper())));
    }

    @Autowired
    public CrawlJobRunner(
            CrawlMapper crawlMapper,
            CrawlBrowserProperties properties,
            ObjectProvider<BoardCrawler> boardCrawlerProvider,
            CrawlService crawlService,
            KoreanMorphAnalyzer morphAnalyzer,
            CrawlDocumentStore documentStore) {
        this.crawlMapper = crawlMapper;
        this.properties = properties;
        this.boardCrawlerProvider = boardCrawlerProvider;
        this.crawlService = crawlService;
        this.documentStore = documentStore;
    }

    @Scheduled(
            fixedDelayString =
                    "${cleverchat.crawl.worker.poll-delay-ms:${cleverchat.crawl.browser.poll-delay-ms:5000}}")
    public void poll() {
        if (!(runtimeSettings == null
                ? workerEnabled
                : runtimeSettings.current().bool(RuntimeSetting.CRAWL_ENABLED))) {
            return;
        }
        crawlMapper.recoverTimedOutJobs(
                OffsetDateTime.now().minusMinutes(properties.getJobTimeoutMinutes()),
                properties.getMaxAttempts(),
                "Crawl job timed out.");
        while ((runtimeSettings == null
                        ? workerEnabled
                        : runtimeSettings.current().bool(RuntimeSetting.CRAWL_ENABLED))
                && activeJobs.get() < properties.getMaxConcurrent()) {
            CrawlJob job = crawlMapper.claimNextJob(properties.getMaxAttempts());
            if (job == null) {
                return;
            }
            activeJobs.incrementAndGet();
            try {
                runJob(job);
            } finally {
                activeJobs.decrementAndGet();
            }
        }
    }

    void runJob(CrawlJob job) {
        if (runtimeSettings == null) runJobWithSettings(job);
        else runtimeSettings.runWithSnapshot(() -> runJobWithSettings(job));
    }

    private void runJobWithSettings(CrawlJob job) {
        CrawlTarget target = crawlMapper.findTargetById(job.getTargetNo());
        if (target == null || !"Y".equals(target.getUseYn())) {
            failJob(job, "Crawl target is missing or disabled.", null);
            return;
        }
        long startedAt = System.nanoTime();
        try {
            if (!isBoardCrawlUrl(target.getUrl())) {
                runStaticJob(job, target);
                return;
            }
            BoardCrawler boardCrawler =
                    (properties.isEnabled() ? boardCrawlerProvider.getIfAvailable() : null);
            if (boardCrawler == null) {
                failJob(job, "Browser crawler is disabled.", null);
                return;
            }
            BrowserCrawlResult result = crawlBoardWithRetries(boardCrawler, target.getUrl());
            StoreResult storeResult = storeDocuments(target, result);
            CrawlRunLog runLog = new CrawlRunLog();
            runLog.setTargetNo(target.getCrawlTargetNo());
            runLog.setDocumentNo(storeResult.firstDocumentNo());
            runLog.setStatus(storeResult.status());
            runLog.setFailureCode(storeResult.failureCode());
            runLog.setMessage(storeResult.message());
            runLog.setDurationMs(durationMs(startedAt));
            applyExport(runLog, storeResult.export());
            crawlMapper.insertRunLog(runLog);
            insertCoverage(target.getCrawlTargetNo(), runLog.getCrawlRunLogNo(), result.coverage());
            crawlMapper.updateTargetRunStatus(
                    target.getCrawlTargetNo(), storeResult.status(), storeResult.message());
            crawlMapper.completeJob(
                    job.getCrawlJobNo(),
                    "DUPLICATE".equals(storeResult.status()) ? "SUCCESS" : storeResult.status(),
                    storeResult.message());
        } catch (RuntimeException e) {
            log.warn("Browser crawl job failed: jobId={}", job.getCrawlJobNo(), e);
            failJob(job, truncate(e.getMessage()), startedAt);
        }
    }

    private void runStaticJob(CrawlJob job, CrawlTarget target) {
        long startedAt = System.nanoTime();
        try {
            List<URI> urls = crawlService.discoverUrlsForJob(target.getCrawlTargetNo());
            BoardCrawler boardCrawler =
                    (properties.isEnabled() ? boardCrawlerProvider.getIfAvailable() : null);
            log.info(
                    "Hybrid crawl started: jobId={}, targetId={}, discoveredUrls={}",
                    job.getCrawlJobNo(),
                    target.getCrawlTargetNo(),
                    urls.size());
            int staticSuccess = 0;
            int staticDuplicate = 0;
            int boardSuccess = 0;
            int boardDuplicate = 0;
            int failed = 0;
            int boardTargets = 0;
            List<URI> deferredBoardRetries = new ArrayList<>();
            for (URI uri : urls) {
                if (isBoardCrawlUrl(uri.toString())) {
                    boardTargets++;
                    if (boardCrawler == null) {
                        failed++;
                        continue;
                    }
                    try {
                        BoardRunOutcome outcome =
                                crawlAndStoreBoard(
                                        job, target, boardCrawler, uri, startedAt, false);
                        if ("DUPLICATE".equals(outcome.status())) {
                            boardDuplicate++;
                        } else if ("SUCCESS".equals(outcome.status())) {
                            boardSuccess++;
                        } else {
                            failed++;
                        }
                    } catch (RuntimeException e) {
                        log.warn(
                                "Browser board crawl failed: targetId={}, url={}",
                                target.getCrawlTargetNo(),
                                uri,
                                e);
                        deferredBoardRetries.add(uri);
                    }
                    continue;
                }
                try {
                    RunResponse response =
                            crawlService.crawlStaticPageForJob(
                                    target.getCrawlTargetNo(), uri, false);
                    if (response != null && "DUPLICATE".equals(response.run().getStatus())) {
                        staticDuplicate++;
                    } else if (response != null && "SUCCESS".equals(response.run().getStatus())) {
                        staticSuccess++;
                    } else {
                        failed++;
                    }
                } catch (RuntimeException e) {
                    log.warn(
                            "Static page crawl failed: targetId={}, url={}",
                            target.getCrawlTargetNo(),
                            uri,
                            e);
                    failed++;
                }
            }
            for (URI uri : deferredBoardRetries) {
                try {
                    waitBeforeBoardRetry(properties.getBoardRetryDelayMs());
                    BoardRunOutcome outcome =
                            crawlAndStoreBoard(job, target, boardCrawler, uri, startedAt, true);
                    if ("DUPLICATE".equals(outcome.status())) {
                        boardDuplicate++;
                    } else if ("SUCCESS".equals(outcome.status())) {
                        boardSuccess++;
                    } else {
                        failed++;
                    }
                } catch (RuntimeException e) {
                    log.warn(
                            "Deferred browser board retry failed permanently: targetId={}, url={}",
                            target.getCrawlTargetNo(),
                            uri,
                            e);
                    insertBrowserFailure(target.getCrawlTargetNo(), uri.toString(), e, startedAt);
                    failed++;
                }
            }
            boolean hasSuccess =
                    staticSuccess > 0
                            || staticDuplicate > 0
                            || boardSuccess > 0
                            || boardDuplicate > 0;
            String status = failed > 0 ? "FAILED" : (hasSuccess ? "SUCCESS" : "FAILED");
            String message =
                    "Hybrid crawl visited "
                            + urls.size()
                            + " URL(s), "
                            + boardTargets
                            + " board list(s): static "
                            + staticSuccess
                            + " success, "
                            + staticDuplicate
                            + " duplicate; board "
                            + boardSuccess
                            + " success, "
                            + boardDuplicate
                            + " duplicate; "
                            + failed
                            + " failed.";
            crawlMapper.updateTargetRunStatus(target.getCrawlTargetNo(), status, truncate(message));
            crawlMapper.completeJob(job.getCrawlJobNo(), status, truncate(message));
        } catch (RuntimeException e) {
            log.warn("Hybrid crawl job failed: jobId={}", job.getCrawlJobNo(), e);
            failJob(
                    job,
                    truncate(e.getMessage() == null ? "Hybrid crawl failed." : e.getMessage()),
                    startedAt);
        }
    }

    private BoardRunOutcome crawlAndStoreBoard(
            CrawlJob job,
            CrawlTarget target,
            BoardCrawler boardCrawler,
            URI uri,
            long startedAt,
            boolean deferredRetry) {
        log.info(
                "Browser board crawl started: jobId={}, targetId={}, url={}, deferredRetry={}",
                job.getCrawlJobNo(),
                target.getCrawlTargetNo(),
                uri,
                deferredRetry);
        BrowserCrawlResult result = crawlBoardWithRetries(boardCrawler, uri.toString());
        StoreResult storeResult = storeDocuments(target, result);
        insertBrowserRun(
                target.getCrawlTargetNo(),
                uri.toString(),
                storeResult,
                result.coverage(),
                startedAt);
        log.info(
                "Browser board crawl completed: jobId={}, targetId={}, url={}, listPages={}, listItems={}, detailsFetched={}, detailsFailed={}, deferredRetry={}",
                job.getCrawlJobNo(),
                target.getCrawlTargetNo(),
                uri,
                result.coverage().listPages(),
                result.coverage().listItemsFound(),
                result.coverage().detailsFetched(),
                result.coverage().detailsFailed(),
                deferredRetry);
        return new BoardRunOutcome(storeResult.status());
    }

    private BrowserCrawlResult crawlBoardWithRetries(BoardCrawler boardCrawler, String url) {
        RuntimeException lastFailure = null;
        int attempts = properties.getBoardRetryAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return boardCrawler.crawl(url);
            } catch (RuntimeException e) {
                lastFailure = e;
                if (attempt == attempts) {
                    throw e;
                }
                long delayMs = (long) properties.getBoardRetryDelayMs() * attempt;
                log.warn(
                        "Browser board attempt failed; retrying whole board: url={}, attempt={}/{}, delayMs={}, reason={}",
                        url,
                        attempt,
                        attempts,
                        delayMs,
                        e.getMessage());
                waitBeforeBoardRetry(delayMs);
            }
        }
        throw lastFailure;
    }

    private void waitBeforeBoardRetry(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Board crawl retry was interrupted.", e);
        }
    }

    private void insertBrowserRun(
            Long targetId,
            String sourceUrl,
            StoreResult storeResult,
            BrowserCrawlCoverage coverage,
            long startedAt) {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setTargetNo(targetId);
        runLog.setDocumentNo(storeResult.firstDocumentNo());
        runLog.setStatus(storeResult.status());
        runLog.setFailureCode(storeResult.failureCode());
        runLog.setMessage(truncate("[" + sourceUrl + "] " + storeResult.message()));
        runLog.setDurationMs(durationMs(startedAt));
        applyExport(runLog, storeResult.export());
        crawlMapper.insertRunLog(runLog);
        insertCoverage(targetId, runLog.getCrawlRunLogNo(), coverage);
    }

    private void insertBrowserFailure(
            Long targetId, String sourceUrl, RuntimeException failure, long startedAt) {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setTargetNo(targetId);
        runLog.setStatus("FAILED");
        runLog.setFailureCode("BROWSER_ERROR");
        String reason =
                failure.getMessage() == null
                        ? failure.getClass().getSimpleName()
                        : failure.getMessage();
        runLog.setMessage(truncate("[" + sourceUrl + "] " + reason));
        runLog.setDurationMs(durationMs(startedAt));
        crawlMapper.insertRunLog(runLog);
    }

    static boolean isBoardCrawlUrl(String url) {
        return url != null
                && (url.contains("/boardList.do")
                        || url.contains("/boardSubList.do")
                        || url.matches(
                                "https?://(?:www\\.)?kepco\\.co\\.kr/home/about/locations/[^/?]+/headquarters\\.do(?:\\?.*)?"));
    }

    private StoreResult storeDocuments(CrawlTarget target, BrowserCrawlResult result) {
        Long firstDocumentNo = null;
        int success = 0;
        int duplicate = 0;
        int exported = 0;
        int exportFailed = 0;
        String exportDirectory = null;
        String exportError = null;
        Set<String> seenUrls = new HashSet<>();
        for (BrowserCrawlPage page : result.pages()) {
            if (!seenUrls.add(page.url())) {
                duplicate++;
                continue;
            }
            var stored =
                    documentStore.store(
                            target, page.url(), page.title(), page.content(), page.httpStatus());
            if (stored.duplicate()) {
                duplicate++;
            } else {
                success++;
            }
            if (firstDocumentNo == null) {
                firstDocumentNo = stored.document().getCrawlDocumentNo();
            }
            if (stored.export().failed()) {
                exportFailed++;
                exportError = stored.export().message();
            } else if ("SUCCESS".equals(stored.export().status())) {
                exported++;
                exportDirectory =
                        java.nio.file.Path.of(stored.export().path()).getParent().toString();
            }
        }
        ExportResult export =
                new ExportResult(
                        target.isJsonExportEnabled()
                                ? (exportFailed > 0 ? "FAILED" : "SUCCESS")
                                : "DISABLED",
                        exportDirectory,
                        target.isJsonExportEnabled()
                                ? truncate(
                                        "JSON "
                                                + exported
                                                + " saved, "
                                                + exportFailed
                                                + " failed."
                                                + (exportError == null ? "" : " " + exportError))
                                : null);
        if (exportFailed > 0) {
            return new StoreResult(
                    "FAILED", "EXPORT_ERROR", firstDocumentNo, export.message(), export);
        }
        if (success == 0 && duplicate == 0) {
            if (result.coverage().listPages() > 0
                    && result.coverage().listItemsFound() == 0
                    && result.coverage().detailsFailed() == 0
                    && !result.coverage().truncated()) {
                return new StoreResult(
                        "SUCCESS", null, null, "Browser crawl confirmed an empty board.", export);
            }
            return new StoreResult(
                    "FAILED",
                    "PARSE_ERROR",
                    null,
                    "Browser crawl produced no readable documents.",
                    export);
        }
        String message =
                "Browser crawl stored " + success + " document(s), " + duplicate + " duplicate(s).";
        if (result.coverage().truncated() || result.coverage().detailsFailed() > 0) {
            return new StoreResult(
                    "FAILED",
                    "PARSE_ERROR",
                    firstDocumentNo,
                    message
                            + " Crawl incomplete: "
                            + result.coverage().detailsFailed()
                            + " detail failure(s), truncated="
                            + result.coverage().truncated()
                            + ".",
                    export);
        }
        return new StoreResult(
                success > 0 ? "SUCCESS" : "DUPLICATE", null, firstDocumentNo, message, export);
    }

    private void applyExport(CrawlRunLog runLog, ExportResult export) {
        runLog.setExportStatus(export.status());
        runLog.setExportPath(export.path());
        runLog.setExportMessage(export.message());
    }

    private void insertCoverage(Long targetId, Long runLogNo, BrowserCrawlCoverage source) {
        CrawlCoverage coverage = new CrawlCoverage();
        coverage.setTargetNo(targetId);
        coverage.setRunLogNo(runLogNo);
        coverage.setListPages(source.listPages());
        coverage.setListItemsFound(source.listItemsFound());
        coverage.setDetailsFetched(source.detailsFetched());
        coverage.setDetailsFailed(source.detailsFailed());
        coverage.setTruncatedYn(source.truncated() ? "Y" : "N");
        crawlMapper.insertCoverage(coverage);
    }

    private void failJob(CrawlJob job, String message, Long startedAtNanos) {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setTargetNo(job.getTargetNo());
        runLog.setStatus("FAILED");
        runLog.setFailureCode("SYSTEM_ERROR");
        runLog.setMessage(truncate(message == null ? "Browser crawl failed." : message));
        if (startedAtNanos != null) {
            runLog.setDurationMs(durationMs(startedAtNanos));
        }
        crawlMapper.insertRunLog(runLog);
        crawlMapper.updateTargetRunStatus(job.getTargetNo(), "FAILED", runLog.getMessage());
        crawlMapper.completeJob(job.getCrawlJobNo(), "FAILED", runLog.getMessage());
    }

    private int durationMs(long startedAt) {
        return (int) ((System.nanoTime() - startedAt) / 1_000_000);
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private record StoreResult(
            String status,
            String failureCode,
            Long firstDocumentNo,
            String message,
            ExportResult export) {}

    private record BoardRunOutcome(String status) {}
}
