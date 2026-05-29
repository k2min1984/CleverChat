package kr.co.cleverchat.domain.crawl.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RetentionResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ReviewRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.SchedulePreviewResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ScheduleRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);
    private static final int DOCUMENT_LIMIT = 100;
    private static final int MAX_CONTENT_LENGTH = 200_000;
    private static final int DEFAULT_RUN_RETENTION_DAYS = 365;
    private static final int DEFAULT_DOCUMENT_RETENTION_DAYS = 90;
    private static final int MIN_RETENTION_DAYS = 30;
    private static final int MIN_SCHEDULE_INTERVAL_MINUTES = 5;
    private static final int DEFAULT_SCHEDULE_INTERVAL_MINUTES = 1440;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final CrawlMapper crawlMapper;
    private final CrawlUrlPolicy urlPolicy;
    private final CrawlFetcher crawlFetcher;
    private final CrawlRobotsService robotsService;
    private final OpsNotificationService notificationService;

    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService) {
        this(crawlMapper, urlPolicy, crawlFetcher, robotsService, null);
    }

    @Autowired
    public CrawlService(
            CrawlMapper crawlMapper,
            CrawlUrlPolicy urlPolicy,
            CrawlFetcher crawlFetcher,
            CrawlRobotsService robotsService,
            OpsNotificationService notificationService) {
        this.crawlMapper = crawlMapper;
        this.urlPolicy = urlPolicy;
        this.crawlFetcher = crawlFetcher;
        this.robotsService = robotsService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CrawlTarget> targets(Boolean enabled) {
        return crawlMapper.findTargets(enabled);
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
        target.setEnabled(request.enabled() == null || request.enabled());
        target.setCreatedBy(adminId);
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
        crawlMapper.insertTarget(target);
        return crawlMapper.findTargetById(target.getId());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public CrawlTarget updateTarget(Long id, TargetRequest request) {
        CrawlTarget target = requireTarget(id);
        URI uri = urlPolicy.validateAndNormalize(request.url());
        String normalizedUrl = uri.toString();
        CrawlTarget duplicate = crawlMapper.findTargetByUrl(normalizedUrl);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE, "Crawl target already exists.");
        }
        target.setUrl(normalizedUrl);
        target.setLabel(blankToNull(request.label()));
        target.setEnabled(request.enabled() == null || request.enabled());
        applySchedule(
                target,
                request.scheduleEnabled(),
                request.scheduleIntervalMinutes(),
                request.scheduleMode(),
                request.scheduleCron());
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
        int resolvedRunDays = resolveRetentionDays(runRetentionDays, DEFAULT_RUN_RETENTION_DAYS);
        int resolvedDocumentDays =
                resolveRetentionDays(documentRetentionDays, DEFAULT_DOCUMENT_RETENTION_DAYS);
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
    public RunResponse run(Long targetId) {
        CrawlTarget target = requireTarget(targetId);
        if (!target.isEnabled()) {
            throw new BusinessException(
                    ErrorCode.STATE_CONFLICT, "Disabled crawl targets cannot be run.");
        }

        long startedAt = System.nanoTime();
        CrawlRunLog runLog = baseRunLog(targetId);
        try {
            URI uri = urlPolicy.validateAndNormalize(target.getUrl());
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
            CrawlDocument document = buildDocument(targetId, fetched, parsed);
            CrawlDocument duplicate =
                    crawlMapper.findDocumentByTargetAndHash(targetId, document.getContentHash());
            if (duplicate != null) {
                runLog.setStatus("DUPLICATE");
                runLog.setFailureCode("DUP_HASH");
                runLog.setDocumentId(duplicate.getId());
                runLog.setHttpStatus(fetched.httpStatus());
                runLog.setMessage(
                        withRobotsMessage(
                                "Duplicate content hash. Existing document #"
                                        + duplicate.getId()
                                        + ".",
                                robotsDecision));
                finishRun(runLog, startedAt);
                return new RunResponse(runLog, duplicate);
            }
            crawlMapper.insertDocument(document);
            runLog.setStatus("SUCCESS");
            runLog.setDocumentId(document.getId());
            runLog.setHttpStatus(fetched.httpStatus());
            runLog.setMessage(
                    withRobotsMessage(
                            "Fetched and parsed " + parsed.content().length() + " characters.",
                            robotsDecision));
            finishRun(runLog, startedAt);
            return new RunResponse(runLog, document);
        } catch (BusinessException e) {
            runLog.setStatus("FAILED");
            if (runLog.getFailureCode() == null) {
                runLog.setFailureCode(classifyFailure(e));
            }
            runLog.setMessage(truncate(e.getMessage()));
            finishRun(runLog, startedAt);
            notifyFailure(runLog);
            throw e;
        }
    }

    @Transactional
    public RunResponse runScheduled(Long targetId) {
        CrawlTarget target = requireTarget(targetId);
        try {
            return run(targetId);
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
            crawlMapper.updateNextRunAt(target.getId(), next);
        } catch (RuntimeException e) {
            CrawlRunLog runLog = baseRunLog(target.getId());
            runLog.setStatus("FAILED");
            runLog.setFailureCode("SCHEDULE_INVALID");
            runLog.setMessage("Invalid crawl schedule. Automatic run has been paused.");
            finishRun(runLog, System.nanoTime());
            crawlMapper.updateNextRunAt(target.getId(), null);
            log.warn("Failed to compute next crawl schedule: targetId={}", target.getId());
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

    private ParsedPage parseHtml(String url, String html) {
        Document document = Jsoup.parse(html == null ? "" : html, url);
        document.select("script,style,noscript,iframe,nav,footer,header,aside").remove();
        String title = blankToNull(document.title());
        String content =
                document.body() == null
                        ? ""
                        : document.body().text().replaceAll("\\s+", " ").trim();
        if (content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.CRAWL_FETCH_FAILED, "Crawl target has no readable body text.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }
        return new ParsedPage(title, content);
    }

    private CrawlDocument buildDocument(
            Long targetId, CrawlFetcher.FetchedPage fetched, ParsedPage parsed) {
        CrawlDocument document = new CrawlDocument();
        document.setTargetId(targetId);
        document.setUrl(fetched.finalUrl());
        document.setTitle(parsed.title());
        document.setContent(parsed.content());
        document.setUrlHash(sha256(fetched.finalUrl()));
        document.setContentHash(sha256(parsed.content()));
        document.setStatus("SUCCESS");
        document.setHttpStatus(fetched.httpStatus());
        document.setFetchedAt(OffsetDateTime.now());
        return document;
    }

    private CrawlRunLog baseRunLog(Long targetId) {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setTargetId(targetId);
        return runLog;
    }

    private void finishRun(CrawlRunLog runLog, long startedAt) {
        runLog.setDurationMs((int) ((System.nanoTime() - startedAt) / 1_000_000));
        runLog.setMessage(truncate(runLog.getMessage()));
        crawlMapper.insertRunLog(runLog);
        crawlMapper.updateTargetRunStatus(
                runLog.getTargetId(), runLog.getStatus(), runLog.getMessage());
    }

    private void notifyFailure(CrawlRunLog runLog) {
        if (notificationService != null) {
            try {
                notificationService.notifyCrawlFailure(runLog);
            } catch (RuntimeException e) {
                log.warn("Crawl failure notification failed: runLogId={}", runLog.getId());
            }
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
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
