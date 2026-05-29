package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.OffsetDateTime;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ScheduleRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CrawlServiceTest {

    private final CrawlMapper crawlMapper = org.mockito.Mockito.mock(CrawlMapper.class);
    private final CrawlUrlPolicy urlPolicy = org.mockito.Mockito.mock(CrawlUrlPolicy.class);
    private final CrawlFetcher crawlFetcher = org.mockito.Mockito.mock(CrawlFetcher.class);
    private final CrawlRobotsService robotsService =
            org.mockito.Mockito.mock(CrawlRobotsService.class);
    private final CrawlService service =
            new CrawlService(crawlMapper, urlPolicy, crawlFetcher, robotsService);

    @Test
    void createTargetRejectsDuplicateUrl() {
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(crawlMapper.findTargetByUrl("https://example.com/help")).thenReturn(target(1L));

        assertThatThrownBy(
                        () ->
                                service.createTarget(
                                        new TargetRequest(
                                                "https://example.com/help",
                                                "Help",
                                                true,
                                                false,
                                                1440,
                                                "INTERVAL",
                                                null),
                                        10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void runStoresParsedDocumentAndRunLog() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://example.com/help",
                                "<html><head><title>Help</title></head><body><nav>skip</nav><h1>Hello</h1><p>World</p></body></html>"));
        ArgumentCaptor<CrawlDocument> documentCaptor = ArgumentCaptor.forClass(CrawlDocument.class);
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        var response = service.run(10L);

        verify(crawlMapper).insertDocument(documentCaptor.capture());
        verify(crawlMapper).insertRunLog(runCaptor.capture());
        assertThat(documentCaptor.getValue().getTitle()).isEqualTo("Help");
        assertThat(documentCaptor.getValue().getContent()).isEqualTo("Hello World");
        assertThat(documentCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(response.document().getContentHash()).isNotBlank();
    }

    @Test
    void runReusesDuplicateDocument() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        CrawlDocument duplicate = new CrawlDocument();
        duplicate.setId(99L);
        duplicate.setStatus("SUCCESS");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://example.com/help",
                                "<html><body>Same content</body></html>"));
        when(crawlMapper.findDocumentByTargetAndHash(any(), any())).thenReturn(duplicate);
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        var response = service.run(10L);

        org.mockito.Mockito.verify(crawlMapper, org.mockito.Mockito.never()).insertDocument(any());
        verify(crawlMapper).insertRunLog(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("DUPLICATE");
        assertThat(runCaptor.getValue().getFailureCode()).isEqualTo("DUP_HASH");
        assertThat(runCaptor.getValue().getDocumentId()).isEqualTo(99L);
        assertThat(response.document().getId()).isEqualTo(99L);
    }

    @Test
    void runRecordsFailureWhenFetchFails() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenThrow(new BusinessException(ErrorCode.CRAWL_FETCH_FAILED, "boom"));
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        assertThatThrownBy(() -> service.run(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_FETCH_FAILED);

        verify(crawlMapper).insertRunLog(runCaptor.capture());
        verify(crawlMapper).updateTargetRunStatus(10L, "FAILED", "boom");
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runCaptor.getValue().getFailureCode()).isEqualTo("FETCH_ERROR");
    }

    @Test
    void runNotifiesCrawlFailureAfterRunLogIsCreated() {
        OpsNotificationService notificationService =
                org.mockito.Mockito.mock(OpsNotificationService.class);
        CrawlService notifyingService =
                new CrawlService(
                        crawlMapper, urlPolicy, crawlFetcher, robotsService, notificationService);
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenThrow(new BusinessException(ErrorCode.CRAWL_FETCH_FAILED, "boom"));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            CrawlRunLog runLog = invocation.getArgument(0);
                            runLog.setId(77L);
                            return null;
                        })
                .when(crawlMapper)
                .insertRunLog(any(CrawlRunLog.class));

        assertThatThrownBy(() -> notifyingService.run(10L)).isInstanceOf(BusinessException.class);

        ArgumentCaptor<CrawlRunLog> notificationCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);
        verify(notificationService).notifyCrawlFailure(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getId()).isEqualTo(77L);
        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void runRecordsFailureWhenRobotsDisallows() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/private");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/private"))
                .thenReturn(URI.create("https://example.com/private"));
        when(robotsService.check(URI.create("https://example.com/private")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(
                                false, "robots.txt disallows crawl."));
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        assertThatThrownBy(() -> service.run(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CRAWL_ROBOTS_BLOCKED);

        verify(crawlMapper).updateTargetRobots(10L, false);
        verify(crawlMapper).insertRunLog(runCaptor.capture());
        org.mockito.Mockito.verify(crawlFetcher, org.mockito.Mockito.never()).fetch(any());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runCaptor.getValue().getFailureCode()).isEqualTo("ROBOTS_BLOCKED");
    }

    @Test
    void runClassifiesEmptyBodyAsParseError() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/empty");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/empty"))
                .thenReturn(URI.create("https://example.com/empty"));
        when(robotsService.check(URI.create("https://example.com/empty")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/empty"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200, "https://example.com/empty", "<html><body></body></html>"));
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        assertThatThrownBy(() -> service.run(10L)).isInstanceOf(BusinessException.class);

        verify(crawlMapper).insertRunLog(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(runCaptor.getValue().getFailureCode()).isEqualTo("PARSE_ERROR");
    }

    @Test
    void runContinuesWhenRobotsCheckFailsOpen() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(null, "robots.txt check failed."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://example.com/help",
                                "<html><body>Readable content</body></html>"));
        ArgumentCaptor<CrawlRunLog> runCaptor = ArgumentCaptor.forClass(CrawlRunLog.class);

        service.run(10L);

        verify(crawlMapper).updateTargetRobots(10L, null);
        verify(crawlMapper).insertRunLog(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(runCaptor.getValue().getMessage()).contains("robots.txt check failed.");
    }

    @Test
    void runScheduledAlwaysSchedulesNextRun() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        target.setScheduleEnabled(true);
        target.setScheduleIntervalMinutes(30);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://example.com/help",
                                "<html><body>Scheduled content</body></html>"));

        service.runScheduled(10L);

        verify(crawlMapper).updateNextRunAt(org.mockito.Mockito.eq(10L), any(OffsetDateTime.class));
    }

    @Test
    void updateScheduleStoresCronModeAndNextRun() {
        CrawlTarget target = target(10L);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        ArgumentCaptor<CrawlTarget> targetCaptor = ArgumentCaptor.forClass(CrawlTarget.class);

        service.updateSchedule(10L, new ScheduleRequest(true, 1440, "CRON", "0 */10 * * * *"));

        verify(crawlMapper).updateTargetSchedule(targetCaptor.capture());
        assertThat(targetCaptor.getValue().isScheduleEnabled()).isTrue();
        assertThat(targetCaptor.getValue().getScheduleMode()).isEqualTo("CRON");
        assertThat(targetCaptor.getValue().getScheduleCron()).isEqualTo("0 */10 * * * *");
        assertThat(targetCaptor.getValue().getNextRunAt()).isNotNull();
    }

    @Test
    void updateScheduleRejectsCronMoreFrequentThanFiveMinutes() {
        CrawlTarget target = target(10L);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);

        assertThatThrownBy(
                        () ->
                                service.updateSchedule(
                                        10L,
                                        new ScheduleRequest(true, 1440, "CRON", "0 * * * * *")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 5 minutes");
    }

    @Test
    void updateScheduleClearsNextRunWhenDisabled() {
        CrawlTarget target = target(10L);
        target.setNextRunAt(OffsetDateTime.now());
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        ArgumentCaptor<CrawlTarget> targetCaptor = ArgumentCaptor.forClass(CrawlTarget.class);

        service.updateSchedule(10L, new ScheduleRequest(false, 30, "INTERVAL", null));

        verify(crawlMapper).updateTargetSchedule(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getNextRunAt()).isNull();
    }

    @Test
    void updateScheduleDoesNotRequireCronWhenDisabled() {
        CrawlTarget target = target(10L);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        ArgumentCaptor<CrawlTarget> targetCaptor = ArgumentCaptor.forClass(CrawlTarget.class);

        service.updateSchedule(10L, new ScheduleRequest(false, 30, "CRON", null));

        verify(crawlMapper).updateTargetSchedule(targetCaptor.capture());
        assertThat(targetCaptor.getValue().isScheduleEnabled()).isFalse();
        assertThat(targetCaptor.getValue().getScheduleCron()).isNull();
        assertThat(targetCaptor.getValue().getNextRunAt()).isNull();
    }

    @Test
    void previewScheduleReturnsNextFiveCronRuns() {
        var response =
                service.previewSchedule(new ScheduleRequest(true, 1440, "CRON", "0 */10 * * * *"));

        assertThat(response.scheduleEnabled()).isTrue();
        assertThat(response.scheduleMode()).isEqualTo("CRON");
        assertThat(response.nextRunTimes()).hasSize(5);
        assertThat(response.nextRunTimes().get(1)).isAfter(response.nextRunTimes().get(0));
    }

    @Test
    void previewScheduleReturnsEmptyListWhenDisabled() {
        var response = service.previewSchedule(new ScheduleRequest(false, 30, "CRON", null));

        assertThat(response.scheduleEnabled()).isFalse();
        assertThat(response.nextRunTimes()).isEmpty();
    }

    @Test
    void runScheduledUsesCronNextRun() {
        CrawlTarget target = target(10L);
        target.setUrl("https://example.com/help");
        target.setScheduleEnabled(true);
        target.setScheduleMode("CRON");
        target.setScheduleCron("0 */10 * * * *");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize("https://example.com/help"))
                .thenReturn(URI.create("https://example.com/help"));
        when(robotsService.check(URI.create("https://example.com/help")))
                .thenReturn(
                        new CrawlRobotsService.RobotsDecision(true, "robots.txt allows crawl."));
        when(crawlFetcher.fetch("https://example.com/help"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://example.com/help",
                                "<html><body>Scheduled content</body></html>"));

        service.runScheduled(10L);

        verify(crawlMapper).updateNextRunAt(org.mockito.Mockito.eq(10L), any(OffsetDateTime.class));
    }

    @Test
    void reviewFailureMarksFailedRunReviewed() {
        CrawlRunLog reviewed = new CrawlRunLog();
        reviewed.setId(7L);
        reviewed.setReviewed(true);
        reviewed.setReviewedBy(10L);
        when(crawlMapper.reviewRunLog(7L, 10L, "checked")).thenReturn(1);
        when(crawlMapper.findRunLogById(7L)).thenReturn(reviewed);

        var response =
                service.reviewFailure(
                        7L,
                        new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ReviewRequest("checked"),
                        10L);

        assertThat(response.isReviewed()).isTrue();
        assertThat(response.getReviewedBy()).isEqualTo(10L);
    }

    @Test
    void deleteExpiredSupportsDryRunAndMinimumRetention() {
        when(crawlMapper.countRunLogsBefore(any(OffsetDateTime.class))).thenReturn(5L);
        when(crawlMapper.countDocumentsBefore(any(OffsetDateTime.class))).thenReturn(2L);

        var response = service.deleteExpired(1, 2, true);

        assertThat(response.runRetentionDays()).isEqualTo(30);
        assertThat(response.documentRetentionDays()).isEqualTo(30);
        assertThat(response.wouldDeleteRunLogs()).isEqualTo(5);
        assertThat(response.wouldDeleteDocuments()).isEqualTo(2);
        assertThat(response.deletedRunLogs()).isZero();
        assertThat(response.deletedDocuments()).isZero();
        org.mockito.Mockito.verify(crawlMapper, org.mockito.Mockito.never())
                .deleteRunLogsBefore(any());
        org.mockito.Mockito.verify(crawlMapper, org.mockito.Mockito.never())
                .deleteDocumentsBefore(any());
    }

    @Test
    void deleteExpiredDeletesWhenNotDryRun() {
        when(crawlMapper.countRunLogsBefore(any(OffsetDateTime.class))).thenReturn(5L);
        when(crawlMapper.countDocumentsBefore(any(OffsetDateTime.class))).thenReturn(2L);
        when(crawlMapper.deleteRunLogsBefore(any(OffsetDateTime.class))).thenReturn(4);
        when(crawlMapper.deleteDocumentsBefore(any(OffsetDateTime.class))).thenReturn(1);

        var response = service.deleteExpired(365, 90, false);

        assertThat(response.deletedRunLogs()).isEqualTo(4);
        assertThat(response.deletedDocuments()).isEqualTo(1);
    }

    private CrawlTarget target(Long id) {
        CrawlTarget target = new CrawlTarget();
        target.setId(id);
        target.setUrl("https://example.com");
        target.setEnabled(true);
        target.setScheduleMode("INTERVAL");
        target.setScheduleIntervalMinutes(1440);
        return target;
    }
}
