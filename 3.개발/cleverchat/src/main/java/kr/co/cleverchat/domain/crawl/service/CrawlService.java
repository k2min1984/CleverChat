package kr.co.cleverchat.domain.crawl.service;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.JobResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RetentionResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ReviewRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.SchedulePreviewResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ScheduleRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlCoverage;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlJob;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrawlService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private static final int DOCUMENT_LIMIT = 100;
    private static final int MAX_CONTENT_LENGTH = 200_000;
    private static final int TOKEN_REINDEX_BATCH_LIMIT = 120;
    private static final int DEFAULT_RUN_RETENTION_DAYS = 365;
    private static final int DEFAULT_DOCUMENT_RETENTION_DAYS = 90;
    private static final int MIN_RETENTION_DAYS = 30;
    private static final int MIN_SCHEDULE_INTERVAL_MINUTES = 5;
    private static final int DEFAULT_SCHEDULE_INTERVAL_MINUTES = 1440;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final URI KEPCO_HOME = URI.create("https://www.kepco.co.kr/home/index.do");
    private static final Pattern KEPCO_SUB_LIST_ARGUMENTS =
            Pattern.compile(
                    "fn_SubList\\(\\s*['\\\"]?([0-9]+)['\\\"]?\\s*,\\s*['\\\"]?([0-9]+)['\\\"]?");
    private static final Pattern KEPCO_LOCATION_BRANCH_ARGUMENTS =
            Pattern.compile(
                    "fn_locationsBranch\\(\\s*['\\\"]?([0-9]+)['\\\"]?\\s*,\\s*['\\\"]?[0-9]+['\\\"]?\\s*,\\s*['\\\"]?([a-zA-Z0-9_-]+)['\\\"]?");

    private final CrawlMapper crawlMapper;
    private final CrawlUrlPolicy urlPolicy;
    private final CrawlFetcher crawlFetcher;
    private final CrawlRobotsService robotsService;
    private final OpsNotificationService notificationService;
    private final KoreanMorphAnalyzer morphAnalyzer;
    private final CrawlDocumentStore documentStore;

    /** 0 means unlimited. Discovery still de-duplicates normalized URLs. */
    @Value("${cleverchat.crawl.max-discovered-pages:0}")
    private int maxDiscoveredPages;

    @Autowired private kr.co.cleverchat.domain.settings.SystemSettingsService systemSettings;

    public String exportDirectory(CrawlTarget target) {
        return systemSettings == null
                ? target.getJsonExportDirectory()
                : systemSettings.exportDirectory(target.getJsonExportDirectory());
    }

    public String defaultExportDirectory() {
        return systemSettings == null ? null : systemSettings.current().crawlExportDirectory();
    }

    public long documentCount(Long targetId, String query) {
        return crawlMapper.countDocuments(targetId, query);
    }

    public List<CrawlDocument> documentPage(Long targetId, String query, int page) {
        return crawlMapper.findDocumentPage(targetId, query, 20, (page - 1) * 20);
    }

    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService) {
        this(crawlMapper, urlPolicy, crawlFetcher, robotsService, null);
    }

    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService,
            OpsNotificationService notificationService) {
        this(
                crawlMapper,
                urlPolicy,
                crawlFetcher,
                robotsService,
                notificationService,
                new KoreanMorphAnalyzer());
    }

    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService,
            OpsNotificationService notificationService,
            KoreanMorphAnalyzer morphAnalyzer) {
        this(
                crawlMapper,
                urlPolicy,
                crawlFetcher,
                robotsService,
                notificationService,
                morphAnalyzer,
                new CrawlDocumentStore(
                        crawlMapper,
                        morphAnalyzer,
                        new CrawlJsonExporter(new com.fasterxml.jackson.databind.ObjectMapper())));
    }

    @Autowired
    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService,
            OpsNotificationService notificationService,
            KoreanMorphAnalyzer morphAnalyzer,
            CrawlDocumentStore documentStore) {
        this.crawlMapper = crawlMapper;
        this.urlPolicy = urlPolicy;
        this.crawlFetcher = crawlFetcher;
        this.robotsService = robotsService;
        this.notificationService = notificationService;
        this.morphAnalyzer = morphAnalyzer;
        this.documentStore = documentStore;
    }

    @Transactional(readOnly = true)
    public List<CrawlTarget> targets(Boolean enabled) {
        String useYn = enabled == null ? null : (enabled ? "Y" : "N");
        return crawlMapper.findTargets(useYn);
    }

    @Transactional(readOnly = true)
    public CrawlTarget target(Long id) {
        return requireTarget(id);
    }

    @Transactional(readOnly = true)
    public List<CrawlTarget> dueTargets(int limit) {
        return crawlMapper.findDueTargets(Math.max(1, Math.min(limit, 50)));
    }

    @Transactional
    @RequireRole("OPERATOR")
    public CrawlTarget createTarget(TargetRequest request, Long adminId) {
        URI uri = urlPolicy.validateAndNormalize(request.url());
        String normalizedUrl = uri.toString();
        CrawlTarget existing = crawlMapper.findTargetByUrl(normalizedUrl);
        if (existing != null) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE, "Crawl target already exists.");
        }
        CrawlTarget target = new CrawlTarget();
        target.setUrl(normalizedUrl);
        target.setLabel(blankToNull(request.label()));
        target.setUseYn("N".equals(request.useYn()) ? "N" : "Y");
        target.setFrstRegrEmpno(adminId);
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
        applyJsonExport(target, request);
        crawlMapper.insertTarget(target);
        return crawlMapper.findTargetById(target.getCrawlTargetNo());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public CrawlTarget updateTarget(Long id, TargetRequest request) {
        CrawlTarget target = requireTarget(id);
        URI uri = urlPolicy.validateAndNormalize(request.url());
        String normalizedUrl = uri.toString();
        CrawlTarget duplicate = crawlMapper.findTargetByUrl(normalizedUrl);
        if (duplicate != null && !duplicate.getCrawlTargetNo().equals(id)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE, "Crawl target already exists.");
        }
        target.setUrl(normalizedUrl);
        target.setLabel(blankToNull(request.label()));
        target.setUseYn("N".equals(request.useYn()) ? "N" : "Y");
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
        applyJsonExport(target, request);
        crawlMapper.updateTarget(target);
        return crawlMapper.findTargetById(id);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public CrawlTarget updateSchedule(Long id, ScheduleRequest request) {
        CrawlTarget target = requireTarget(id);
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
        crawlMapper.updateTargetSchedule(target);
        return crawlMapper.findTargetById(id);
    }

    @Transactional(readOnly = true)
    public SchedulePreviewResponse previewSchedule(ScheduleRequest request) {
        CrawlTarget target = new CrawlTarget();
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
        List<OffsetDateTime> nextRunTimes = new ArrayList<>();
        OffsetDateTime next = target.getNextRunAt();
        for (int i = 0; i < 5 && next != null; i++) {
            nextRunTimes.add(next);
            next = nextRunAt(target, next);
        }
        return new SchedulePreviewResponse(
                target.isScheduleEnabled(), target.getScheduleMode(), nextRunTimes);
    }

    @Transactional(readOnly = true)
    public List<CrawlDocument> documents(Long targetId) {
        return crawlMapper.findDocuments(targetId, DOCUMENT_LIMIT);
    }

    @Transactional(readOnly = true)
    public CrawlDocument document(Long id) {
        CrawlDocument document = crawlMapper.findDocumentById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return document;
    }

    @Transactional(readOnly = true)
    public List<CrawlRunLog> runLogs(
            Long targetId, String status, String failureCode, Integer limit) {
        int resolvedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return crawlMapper.findRunLogs(
                targetId, status, normalizeFailureCode(failureCode), resolvedLimit);
    }

    @Transactional(readOnly = true)
    public List<CrawlRunLog> failedRunLogs(Boolean reviewed, String failureCode, Integer limit) {
        int resolvedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return crawlMapper.findFailedRunLogs(
                reviewed, normalizeFailureCode(failureCode), resolvedLimit);
    }

    @Transactional(readOnly = true)
    public List<CrawlJob> jobs(Long targetId, Integer limit) {
        int resolvedLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return crawlMapper.findJobs(targetId, resolvedLimit);
    }

    @Transactional(readOnly = true)
    public List<CrawlCoverage> coverages(Long targetId, Integer limit) {
        int resolvedLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return crawlMapper.findCoverages(targetId, resolvedLimit);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public CrawlRunLog reviewFailure(Long runId, ReviewRequest request, Long reviewedBy) {
        int updated =
                crawlMapper.reviewRunLog(
                        runId,
                        reviewedBy,
                        truncate(blankToNull(request == null ? null : request.comment())));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Failed crawl run log not found.");
        }
        return crawlMapper.findRunLogById(runId);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public RetentionResponse deleteExpired(
            Integer runRetentionDays, Integer documentRetentionDays, boolean dryRun) {
        return deleteExpiredData(runRetentionDays, documentRetentionDays, dryRun);
    }

    @Transactional
    public RetentionResponse deleteExpiredScheduled() {
        return deleteExpiredData(null, null, false);
    }

    private RetentionResponse deleteExpiredData(
            Integer runRetentionDays, Integer documentRetentionDays, boolean dryRun) {
        int resolvedRunDays =
                resolveRetentionDays(
                        runRetentionDays,
                        runtimeSettings == null
                                ? DEFAULT_RUN_RETENTION_DAYS
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.RUN_RETENTION_DAYS));
        int resolvedDocumentDays =
                resolveRetentionDays(
                        documentRetentionDays,
                        runtimeSettings == null
                                ? DEFAULT_DOCUMENT_RETENTION_DAYS
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.DOCUMENT_RETENTION_DAYS));
        OffsetDateTime runCutoff = OffsetDateTime.now().minusDays(resolvedRunDays);
        OffsetDateTime documentCutoff = OffsetDateTime.now().minusDays(resolvedDocumentDays);
        long wouldDeleteRunLogs = crawlMapper.countRunLogsBefore(runCutoff);
        long wouldDeleteDocuments = crawlMapper.countDocumentsBefore(documentCutoff);
        int deletedRunLogs = dryRun ? 0 : crawlMapper.deleteRunLogsBefore(runCutoff);
        int deletedDocuments = dryRun ? 0 : crawlMapper.deleteDocumentsBefore(documentCutoff);
        return new RetentionResponse(
                resolvedRunDays,
                resolvedDocumentDays,
                runCutoff,
                documentCutoff,
                wouldDeleteRunLogs,
                wouldDeleteDocuments,
                deletedRunLogs,
                deletedDocuments,
                dryRun);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public JobResponse run(Long targetId) {
        return enqueueRun(targetId, "MANUAL", null);
    }

    @Transactional
    public JobResponse enqueueRun(Long targetId, String triggerType, Long requestedBy) {
        // Serialize manual/scheduled enqueue requests for the same target within this transaction.
        crawlMapper.lockTargetForRun(targetId);
        CrawlTarget target = requireTarget(targetId);
        if (!"Y".equals(target.getUseYn())) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "Disabled crawl targets cannot be run.");
        }
        CrawlJob job = crawlMapper.enqueueJob(targetId, triggerType, requestedBy);
        return new JobResponse(job, job != null);
    }

    @Transactional
    public List<URI> discoverUrlsForJob(Long targetId) {
        CrawlTarget target = requireTarget(targetId);
        URI uri = urlPolicy.validateAndNormalize(target.getUrl());
        return discoverUrls(targetId, uri);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RunResponse crawlStaticPageForJob(Long targetId, URI uri, boolean updateTargetStatus) {
        return crawlOnePage(targetId, uri, updateTargetStatus);
    }

    private RunResponse crawlOnePage(Long targetId, URI uri, boolean updateTargetStatus) {
        long startedAt = System.nanoTime();
        CrawlRunLog runLog = baseRunLog(targetId);
        try {
            CrawlRobotsService.RobotsDecision robotsDecision = robotsService.check(uri);
            crawlMapper.updateTargetRobots(targetId, robotsDecision.allowed());
            if (Boolean.FALSE.equals(robotsDecision.allowed())) {
                throw new BusinessException(
                        ErrorCode.CRAWL_ROBOTS_BLOCKED, robotsDecision.message());
            }
            CrawlFetcher.FetchedPage fetched = crawlFetcher.fetch(uri.toString());
            ParsedPage parsed;
            try {
                parsed = parseHtml(fetched.finalUrl(), fetched.html());
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.CRAWL_FETCH_FAILED) {
                    runLog.setFailureCode("PARSE_ERROR");
                }
                throw e;
            }
            var stored =
                    documentStore.store(
                            requireTarget(targetId),
                            fetched.finalUrl(),
                            parsed.title(),
                            parsed.content(),
                            fetched.httpStatus());
            CrawlDocument document = stored.document();
            runLog.setDocumentNo(document.getCrawlDocumentNo());
            runLog.setHttpStatus(fetched.httpStatus());
            runLog.setExportStatus(stored.export().status());
            runLog.setExportPath(stored.export().path());
            runLog.setExportMessage(stored.export().message());
            if (stored.export().failed()) {
                runLog.setStatus("FAILED");
                runLog.setFailureCode("EXPORT_ERROR");
                runLog.setMessage(stored.export().message());
            } else if (stored.duplicate()) {
                runLog.setStatus("DUPLICATE");
                runLog.setFailureCode("DUP_HASH");
                runLog.setMessage(
                        withRobotsMessage(
                                "Duplicate content hash. Existing document #"
                                        + document.getCrawlDocumentNo()
                                        + ".",
                                robotsDecision));
            } else {
                runLog.setStatus("SUCCESS");
                runLog.setMessage(
                        withRobotsMessage(
                                "Fetched and parsed " + parsed.content().length() + " characters.",
                                robotsDecision));
            }
            finishRun(runLog, startedAt, updateTargetStatus);
            if (stored.export().failed()) {
                notifyFailure(runLog);
            }
            return new RunResponse(runLog, document);
        } catch (BusinessException e) {
            recordFailure(runLog, e, startedAt, updateTargetStatus);
            throw e;
        }
    }

    private void recordFailure(
            CrawlRunLog runLog, BusinessException e, long startedAt, boolean updateTargetStatus) {
        runLog.setStatus("FAILED");
        if (runLog.getFailureCode() == null) {
            runLog.setFailureCode(classifyFailure(e));
        }
        runLog.setMessage(truncate(e.getMessage()));
        finishRun(runLog, startedAt, updateTargetStatus);
        notifyFailure(runLog);
    }

    @Transactional
    public JobResponse runScheduled(Long targetId) {
        CrawlTarget target = requireTarget(targetId);
        try {
            return enqueueRun(targetId, "SCHEDULE", null);
        } finally {
            scheduleNextRun(target);
        }
    }

    private CrawlTarget requireTarget(Long id) {
        CrawlTarget target = crawlMapper.findTargetById(id);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Crawl target not found.");
        }
        return target;
    }

    private void applyJsonExport(CrawlTarget target, TargetRequest request) {
        // Older API clients omit the new fields: preserve an existing export setting.
        if (request.jsonExportEnabled() == null && request.jsonExportDirectory() == null) {
            return;
        }
        boolean enabled =
                request.jsonExportEnabled() == null
                        ? target.isJsonExportEnabled()
                        : request.jsonExportEnabled();
        String directory =
                request.jsonExportDirectory() == null
                        ? target.getJsonExportDirectory()
                        : blankToNull(request.jsonExportDirectory());
        if (enabled) {
            String effective =
                    systemSettings == null ? directory : systemSettings.exportDirectory(directory);
            CrawlJsonExporter.validateDirectory(effective);
            if (directory != null) directory = CrawlJsonExporter.validateDirectory(directory);
        } else if (directory != null && directory.length() > 1000) {
            throw new IllegalArgumentException("JSON 저장 폴더는 최대 1000자까지 입력할 수 있습니다.");
        }
        target.setJsonExportEnabled(enabled);
        target.setJsonExportDirectory(directory);
    }

    private void applySchedule(
            CrawlTarget target,
            Boolean scheduleEnabled,
            Integer scheduleIntervalMinutes,
            String scheduleMode,
            String scheduleCron) {
        boolean enabled = scheduleEnabled != null && scheduleEnabled;
        String mode = resolveScheduleMode(scheduleMode);
        target.setScheduleEnabled(enabled);
        target.setScheduleIntervalMinutes(resolveInterval(scheduleIntervalMinutes));
        target.setScheduleMode(mode);
        if (!enabled) {
            target.setScheduleCron(null);
            target.setNextRunAt(null);
            return;
        }
        target.setScheduleCron("CRON".equals(mode) ? normalizeCron(scheduleCron) : null);
        target.setNextRunAt(nextRunAt(target, OffsetDateTime.now()));
    }

    private int resolveInterval(Integer scheduleIntervalMinutes) {
        if (scheduleIntervalMinutes == null) {
            return DEFAULT_SCHEDULE_INTERVAL_MINUTES;
        }
        return Math.max(MIN_SCHEDULE_INTERVAL_MINUTES, Math.min(scheduleIntervalMinutes, 10080));
    }

    private String resolveScheduleMode(String scheduleMode) {
        if (scheduleMode == null || scheduleMode.isBlank()) {
            return "INTERVAL";
        }
        String mode = scheduleMode.trim().toUpperCase();
        if (!"INTERVAL".equals(mode) && !"CRON".equals(mode)) {
            throw new IllegalArgumentException("Invalid schedule mode.");
        }
        return mode;
    }

    private String normalizeCron(String scheduleCron) {
        String cron = blankToNull(scheduleCron);
        if (cron == null) {
            throw new IllegalArgumentException("Cron expression is required.");
        }
        CronExpression expression = parseCron(cron);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime first = nextCronTime(expression, now);
        OffsetDateTime second = nextCronTime(expression, first);
        if (Duration.between(first, second).toMinutes() < MIN_SCHEDULE_INTERVAL_MINUTES) {
            throw new IllegalArgumentException("Cron interval must be at least 5 minutes.");
        }
        return cron;
    }

    private void scheduleNextRun(CrawlTarget target) {
        try {
            OffsetDateTime next = nextRunAt(target, OffsetDateTime.now());
            crawlMapper.updateNextRunAt(target.getCrawlTargetNo(), next);
        } catch (RuntimeException e) {
            CrawlRunLog runLog = baseRunLog(target.getCrawlTargetNo());
            runLog.setStatus("FAILED");
            runLog.setFailureCode("SCHEDULE_INVALID");
            runLog.setMessage("Invalid crawl schedule. Automatic run has been paused.");
            finishRun(runLog, System.nanoTime());
            crawlMapper.updateNextRunAt(target.getCrawlTargetNo(), null);
            log.warn(
                    "Failed to compute next crawl schedule: targetId={}",
                    target.getCrawlTargetNo());
        }
    }

    private OffsetDateTime nextRunAt(CrawlTarget target, OffsetDateTime from) {
        if (!target.isScheduleEnabled()) {
            return null;
        }
        if ("CRON".equals(target.getScheduleMode())) {
            return nextCronTime(parseCron(target.getScheduleCron()), from);
        }
        return from.plusMinutes(resolveInterval(target.getScheduleIntervalMinutes()));
    }

    private CronExpression parseCron(String cron) {
        try {
            return CronExpression.parse(cron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression.");
        }
    }

    private OffsetDateTime nextCronTime(CronExpression expression, OffsetDateTime from) {
        ZonedDateTime next = expression.next(from.atZoneSameInstant(SEOUL));
        if (next == null) {
            throw new IllegalArgumentException("Cron expression has no next execution.");
        }
        return next.toOffsetDateTime();
    }

    private String normalizeFailureCode(String failureCode) {
        String code = blankToNull(failureCode);
        return code == null ? null : code.toUpperCase();
    }

    private String classifyFailure(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.CRAWL_ROBOTS_BLOCKED) {
            return "ROBOTS_BLOCKED";
        }
        if (e.getErrorCode() == ErrorCode.CRAWL_URL_BLOCKED) {
            return "URL_BLOCKED";
        }
        if (e.getErrorCode() != ErrorCode.CRAWL_FETCH_FAILED) {
            return "SYSTEM_ERROR";
        }
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (message.contains("http ")) {
            return "HTTP_ERROR";
        }
        if (message.contains("timed out") || message.contains("timeout")) {
            return "TIMEOUT";
        }
        if (message.contains("not html")) {
            return "NOT_HTML";
        }
        if (message.contains("no readable body text")) {
            return "PARSE_ERROR";
        }
        return "FETCH_ERROR";
    }

    private int resolveRetentionDays(Integer retentionDays, int defaultDays) {
        if (retentionDays == null) {
            return defaultDays;
        }
        return Math.max(MIN_RETENTION_DAYS, retentionDays);
    }

    private String withRobotsMessage(String message, CrawlRobotsService.RobotsDecision decision) {
        if (decision == null || decision.allowed() != null) {
            return message;
        }
        return message + " " + decision.message();
    }

    private List<URI> discoverUrls(Long targetId, URI seedUri) {
        if (isKepcoOfficialMenuSeed(seedUri)) {
            return discoverKepcoOfficialMenuUrls(targetId, seedUri);
        }
        Set<URI> urls = new LinkedHashSet<>();
        Set<URI> visited = new LinkedHashSet<>();
        Queue<URI> pending = new ArrayDeque<>();
        urls.add(seedUri);
        pending.add(seedUri);
        try {
            CrawlRobotsService.RobotsDecision robotsDecision = robotsService.check(seedUri);
            if (Boolean.FALSE.equals(robotsDecision.allowed())) {
                crawlMapper.updateTargetRobots(targetId, false);
                throw new BusinessException(
                        ErrorCode.CRAWL_ROBOTS_BLOCKED, robotsDecision.message());
            }
            while (!pending.isEmpty() && !discoveryLimitReached(urls.size())) {
                URI currentUri = pending.poll();
                if (!visited.add(currentUri)) {
                    continue;
                }
                CrawlFetcher.FetchedPage fetched;
                try {
                    fetched = crawlFetcher.fetch(currentUri.toString());
                } catch (RuntimeException e) {
                    if (currentUri.equals(seedUri)) {
                        throw e;
                    }
                    log.warn(
                            "Crawl link discovery skipped an unreachable child URL: targetId={}, url={}, reason={}",
                            targetId,
                            currentUri,
                            e.getMessage());
                    continue;
                }
                Document document =
                        Jsoup.parse(
                                fetched.html() == null ? "" : fetched.html(), fetched.finalUrl());
                for (org.jsoup.nodes.Element link : document.select("a[href]")) {
                    if (discoveryLimitReached(urls.size())) {
                        break;
                    }
                    Optional<URI> discovered =
                            normalizeDiscoveredUrl(seedUri, link.attr("abs:href"));
                    if (discovered.isPresent() && urls.add(discovered.get())) {
                        pending.add(discovered.get());
                    }
                }
            }
            if (!pending.isEmpty() && discoveryLimitReached(urls.size())) {
                log.warn(
                        "Crawl link discovery reached the configured limit: targetId={}, limit={}",
                        targetId,
                        discoveryLimit());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn(
                    "Crawl link discovery stopped early: targetId={}, discoveredPages={}",
                    targetId,
                    urls.size());
        }
        return new ArrayList<>(urls);
    }

    private List<URI> discoverKepcoOfficialMenuUrls(Long targetId, URI seedUri) {
        Set<URI> urls = new LinkedHashSet<>();
        urls.add(seedUri);
        CrawlRobotsService.RobotsDecision robotsDecision = robotsService.check(seedUri);
        if (Boolean.FALSE.equals(robotsDecision.allowed())) {
            crawlMapper.updateTargetRobots(targetId, false);
            throw new BusinessException(ErrorCode.CRAWL_ROBOTS_BLOCKED, robotsDecision.message());
        }

        CrawlFetcher.FetchedPage sitemapPage = crawlFetcher.fetch(KEPCO_HOME.toString());
        Document sitemap =
                Jsoup.parse(
                        sitemapPage.html() == null ? "" : sitemapPage.html(),
                        sitemapPage.finalUrl());
        for (Element link : sitemap.select(".sitemap-container a[href]")) {
            normalizeDiscoveredUrl(seedUri, link.attr("abs:href"))
                    .filter(this::isKepcoHomeUrl)
                    .ifPresent(urls::add);
        }
        if (urls.size() <= 1) {
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED,
                    "KEPCO official sitemap did not contain any crawlable menu URLs.");
        }

        int menuUrlCount = urls.size();
        urls.stream()
                .filter(this::isKepcoLocationsIndex)
                .findFirst()
                .ifPresent(
                        locations -> discoverKepcoRegionalHeadquarters(targetId, locations, urls));
        List<URI> boardLists =
                urls.stream().filter(uri -> uri.getPath().endsWith("/boardList.do")).toList();
        for (URI boardList : boardLists) {
            discoverKepcoSubBoardUrls(targetId, boardList, urls);
        }
        log.info(
                "KEPCO official-menu discovery completed: targetId={}, menuUrls={}, boardLists={}, totalWithSubBoards={}",
                targetId,
                menuUrlCount,
                boardLists.size(),
                urls.size());
        return new ArrayList<>(urls);
    }

    private void discoverKepcoRegionalHeadquarters(
            Long targetId, URI locationsIndex, Set<URI> urls) {
        try {
            CrawlFetcher.FetchedPage fetched = crawlFetcher.fetch(locationsIndex.toString());
            Document document =
                    Jsoup.parse(fetched.html() == null ? "" : fetched.html(), fetched.finalUrl());
            for (Element element :
                    document.select("[href*=fn_locationsBranch], [onclick*=fn_locationsBranch]")) {
                String script = element.attr("href") + " " + element.attr("onclick");
                Matcher matcher = KEPCO_LOCATION_BRANCH_ARGUMENTS.matcher(script);
                if (!matcher.find()) {
                    continue;
                }
                URI headquarters =
                        kepcoRegionalHeadquartersUrl(
                                locationsIndex, matcher.group(1), matcher.group(2));
                try {
                    urls.add(urlPolicy.validateAndNormalize(headquarters.toString()));
                } catch (BusinessException e) {
                    log.warn(
                            "KEPCO regional headquarters URL was rejected: targetId={}, url={}",
                            targetId,
                            headquarters);
                }
            }
        } catch (RuntimeException e) {
            log.warn(
                    "KEPCO regional headquarters discovery failed: targetId={}, locations={}, reason={}",
                    targetId,
                    locationsIndex,
                    e.getMessage());
        }
    }

    private URI kepcoRegionalHeadquartersUrl(URI locationsIndex, String branchNo, String domain) {
        try {
            return new URI(
                    locationsIndex.getScheme(),
                    locationsIndex.getAuthority(),
                    "/home/about/locations/" + domain + "/headquarters.do",
                    "branchNo=" + branchNo,
                    null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid KEPCO regional headquarters URL", e);
        }
    }

    private void discoverKepcoSubBoardUrls(Long targetId, URI boardList, Set<URI> urls) {
        try {
            CrawlFetcher.FetchedPage fetched = crawlFetcher.fetch(boardList.toString());
            Document document =
                    Jsoup.parse(fetched.html() == null ? "" : fetched.html(), fetched.finalUrl());
            for (Element link : document.select("a[href*=fn_SubList]")) {
                Matcher matcher = KEPCO_SUB_LIST_ARGUMENTS.matcher(link.attr("href"));
                if (!matcher.find()) {
                    continue;
                }
                URI subList = kepcoSubListUrl(boardList, matcher.group(1), matcher.group(2));
                try {
                    urls.add(urlPolicy.validateAndNormalize(subList.toString()));
                } catch (BusinessException e) {
                    log.warn(
                            "KEPCO sub-board URL was rejected: targetId={}, url={}",
                            targetId,
                            subList);
                }
            }
        } catch (RuntimeException e) {
            log.warn(
                    "KEPCO sub-board discovery failed: targetId={}, boardList={}, reason={}",
                    targetId,
                    boardList,
                    e.getMessage());
        }
    }

    private URI kepcoSubListUrl(URI boardList, String boardMngNo, String parentBoardNo) {
        try {
            String path = boardList.getPath().replace("/boardList.do", "/boardSubList.do");
            return new URI(
                    boardList.getScheme(),
                    boardList.getAuthority(),
                    path,
                    "boardMngNo=" + boardMngNo + "&pBoardNo=" + parentBoardNo,
                    null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid KEPCO sub-board URL", e);
        }
    }

    private boolean isKepcoOfficialMenuSeed(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
        return ("www.kepco.co.kr".equals(host) || "kepco.co.kr".equals(host))
                && ("/".equals(path) || "/home/index.do".equals(path));
    }

    private boolean isKepcoHomeUrl(URI uri) {
        return uri.getPath() != null
                && uri.getPath().startsWith("/home/")
                && !"/home/".equals(uri.getPath());
    }

    private boolean isKepcoLocationsIndex(URI uri) {
        return "/home/about/locations.do".equals(uri.getPath());
    }

    private int discoveryLimit() {
        return runtimeSettings == null
                ? maxDiscoveredPages
                : runtimeSettings.current().integer(RuntimeSetting.DISCOVERY_LIMIT);
    }

    private boolean discoveryLimitReached(int discoveredCount) {
        int limit = discoveryLimit();
        return limit > 0 && discoveredCount >= limit;
    }

    private Optional<URI> normalizeDiscoveredUrl(URI seedUri, String href) {
        if (href == null || href.isBlank()) {
            return Optional.empty();
        }
        URI candidate;
        try {
            candidate = seedUri.resolve(href.trim()).normalize();
            candidate =
                    new URI(
                            candidate.getScheme(),
                            candidate.getAuthority(),
                            candidate.getPath(),
                            candidate.getQuery(),
                            null);
        } catch (Exception e) {
            return Optional.empty();
        }
        String scheme = candidate.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return Optional.empty();
        }
        if (!sameOrigin(seedUri, candidate)
                || looksLikeBinary(candidate.getPath())
                || looksLikeNonContentPage(candidate)) {
            return Optional.empty();
        }
        try {
            return Optional.of(urlPolicy.validateAndNormalize(candidate.toString()));
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }

    private boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && left.getPort() == right.getPort();
    }

    private boolean looksLikeBinary(String path) {
        String normalized = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return normalized.matches(
                ".*\\.(pdf|hwp|hwpx|doc|docx|xls|xlsx|ppt|pptx|zip|jpg|jpeg|png|gif|webp|svg|mp4|mp3|avi|wmv)$");
    }

    private boolean looksLikeNonContentPage(URI uri) {
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("/search/")
                || path.endsWith("/search")
                || path.endsWith("/search.do")
                || path.endsWith("/search.jsp")
                || path.endsWith("/searchnew.jsp")
                || path.contains("totalsearch")
                || path.contains("login")
                || path.contains("logout")
                || path.contains("member")
                || path.contains("auth")
                || path.contains("certification")
                || path.endsWith("/photo/index.do")
                || path.endsWith("/gallery/index.do")) {
            return true;
        }
        String query = uri.getQuery() == null ? "" : uri.getQuery().toLowerCase(Locale.ROOT);
        return query.matches("(^|.*&)(query|keyword|searchkeyword|searchword|q)=");
    }

    private ParsedPage parseHtml(String url, String html) {
        Document document = Jsoup.parse(html == null ? "" : html, url);
        document.select("script,style,noscript,iframe,nav,footer,header,aside").remove();
        String title = blankToNull(document.title());
        String content = extractStaticContent(url, document);
        if (content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl target has no readable body text.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }
        return new ParsedPage(title, content);
    }

    private String extractStaticContent(String url, Document document) {
        if (document.body() == null) {
            return "";
        }
        if (isKepcoBoardView(url)) {
            for (String selector :
                    List.of(
                            "main .detail-content",
                            "main .board-detail .detail-content",
                            "main .board-detail article",
                            "main .board-detail")) {
                Element candidate = document.selectFirst(selector);
                String content = candidate == null ? "" : normalizeMultilineContent(candidate);
                if (!content.isBlank()) {
                    return content;
                }
            }
        }
        document.select(
                        "[class*=satisfaction],[class*=satis],"
                                + "[class*=charge],[class*=manager],[class*=department],"
                                + "[class*=logout],[class*=session],[id*=logout],[id*=session],"
                                + ".pagination,.paging,[class*=pagination],[class*=paging]")
                .remove();
        return normalizeMultilineContent(document.body());
    }

    private boolean isKepcoBoardView(String url) {
        String value = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return value.contains("kepco.co.kr") && value.contains("boardview.do");
    }

    private String normalizeMultilineContent(Element element) {
        Element clone = element.clone();
        clone.select(
                        "script,style,noscript,header,footer,nav,"
                                + ".pagination,.paging,[class*=pagination],[class*=paging],"
                                + "[class*=satisfaction],[class*=satis],"
                                + "[class*=charge],[class*=manager],[class*=department],"
                                + "[class*=logout],[class*=session],[id*=logout],[id*=session],"
                                + ".breadcrumb,.sub-title-block,.tab-list-box")
                .remove();
        clone.select("br").forEach(node -> node.appendText("\n"));
        clone.select(
                        "address,article,aside,blockquote,dd,div,dl,dt,fieldset,figcaption,"
                                + "figure,h1,h2,h3,h4,h5,h6,header,hr,li,main,nav,ol,p,"
                                + "pre,section,table,tbody,td,tfoot,th,thead,tr,ul")
                .forEach(node -> node.appendText("\n"));
        String text =
                clone.wholeText()
                        .replace('\u00A0', ' ')
                        .replaceAll("\\?\\s*\\?\\s*-\\s*", "\n- ")
                        .replaceAll("\\?\\s*(?=(\\d+[.)]|[□<\\-]))", "\n")
                        .replaceAll("\\?\\s*$", "");
        StringBuilder normalized = new StringBuilder();
        for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.replaceAll("[\\t\\x0B\\f ]+", " ").trim();
            if (isStaticNoiseLine(line)) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append('\n');
            }
            normalized.append(line);
        }
        String result = normalized.toString().replaceAll("\\n{3,}", "\n\n").trim();
        if (!result.contains("\n")) {
            result =
                    result.replaceAll("\\s+(?=\\d+[.)]\\s)", "\n")
                            .replaceAll("\\s+(?=[□<])", "\n")
                            .replaceAll("\\s+(?=-\\s)", "\n")
                            .replaceAll("\\n{3,}", "\n\n")
                            .trim();
        }
        return result;
    }

    private boolean isStaticNoiseLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String compact = line.replaceAll("\\s+", "");
        if (compact.matches(".*(만족하셨습니까|매우만족|만족도|페이지에서제공하는정보).*")) {
            return true;
        }
        if (compact.matches(".*(담당부서|담당자|연락처|최종업데이트|페이지번호입력|다음페이지|이전페이지).*")) {
            return true;
        }
        if (compact.matches(".*(자동로그아웃|로그아웃됩니다|로그인연장|세션).*")) {
            return true;
        }
        if (compact.matches(".*(등록일|작성일|조회수|첨부파일|다운로드|미리보기|점자로보기|Loading).*")) {
            return true;
        }
        return compact.matches("(홈|목록|이전|다음|이전글|다음글|맨위|TOP|검색|닫기|열기)");
    }

    private static String tokenSource(String title, String content) {
        return (title == null ? "" : title) + " " + (content == null ? "" : content);
    }

    /**
     * Tokenizes documents whose {@code content_tokens} are still NULL (rows crawled before morpheme
     * indexing was introduced, or whose backfill has not run yet). Processes up to {@code
     * batchLimit} documents per call and returns how many were updated. Idempotent: once a document
     * has tokens it is no longer selected.
     */
    @Transactional
    public int reindexMissingTokens(int batchLimit) {
        int limit = Math.max(1, Math.min(batchLimit, TOKEN_REINDEX_BATCH_LIMIT));
        List<CrawlDocument> pending = crawlMapper.findDocumentsMissingTokens(limit);
        for (CrawlDocument document : pending) {
            String tokens =
                    morphAnalyzer.tokenize(tokenSource(document.getTitle(), document.getContent()));
            crawlMapper.updateDocumentTokens(document.getCrawlDocumentNo(), tokens);
        }
        return pending.size();
    }

    private CrawlRunLog baseRunLog(Long targetId) {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setTargetNo(targetId);
        return runLog;
    }

    private void finishRun(CrawlRunLog runLog, long startedAt) {
        finishRun(runLog, startedAt, true);
    }

    private void finishRun(CrawlRunLog runLog, long startedAt, boolean updateTargetStatus) {
        runLog.setDurationMs((int) ((System.nanoTime() - startedAt) / 1_000_000));
        runLog.setMessage(truncate(runLog.getMessage()));
        crawlMapper.insertRunLog(runLog);
        if (updateTargetStatus) {
            crawlMapper.updateTargetRunStatus(
                    runLog.getTargetNo(), runLog.getStatus(), runLog.getMessage());
        }
    }

    private void notifyFailure(CrawlRunLog runLog) {
        if (notificationService != null) {
            try {
                notificationService.notifyCrawlFailure(runLog);
            } catch (RuntimeException e) {
                log.warn(
                        "Crawl failure notification failed: runLogId={}",
                        runLog.getCrawlRunLogNo());
            }
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private record ParsedPage(String title, String content) {}
}
