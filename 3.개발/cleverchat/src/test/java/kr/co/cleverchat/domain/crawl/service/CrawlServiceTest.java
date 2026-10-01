package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ScheduleRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlJob;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
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
                                                "Y",
                                                false,
                                                1440,
                                                "INTERVAL",
                                                null,
                                                null,
                                                null),
                                        10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void runEnqueuesManualJobWithoutStaticFetch() {
        CrawlTarget target = target(10L);
        CrawlJob job = job(77L, "MANUAL", "PENDING");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(crawlMapper.enqueueJob(10L, "MANUAL", null)).thenReturn(job);

        var response = service.run(10L);

        assertThat(response.enqueued()).isTrue();
        assertThat(response.job().getCrawlJobNo()).isEqualTo(77L);
        verify(crawlMapper).enqueueJob(10L, "MANUAL", null);
        org.mockito.Mockito.verifyNoInteractions(crawlFetcher);
        org.mockito.Mockito.verifyNoInteractions(robotsService);
    }

    @Test
    void runReportsDuplicateActiveJobAsNotEnqueued() {
        when(crawlMapper.findTargetById(10L)).thenReturn(target(10L));
        when(crawlMapper.enqueueJob(10L, "MANUAL", null)).thenReturn(null);

        var response = service.run(10L);

        assertThat(response.enqueued()).isFalse();
        assertThat(response.job()).isNull();
    }

    @Test
    void runRejectsDisabledTarget() {
        CrawlTarget target = target(10L);
        target.setUseYn("N");
        when(crawlMapper.findTargetById(10L)).thenReturn(target);

        assertThatThrownBy(() -> service.run(10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    @Test
    void discoveryContinuesAfterAnUnreachableChildUrl() {
        CrawlTarget target = target(10L);
        URI seed = URI.create("https://example.com/");
        URI missing = URI.create("https://example.com/missing");
        URI valid = URI.create("https://example.com/valid");
        target.setUrl(seed.toString());
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize(seed.toString())).thenReturn(seed);
        when(urlPolicy.validateAndNormalize(missing.toString())).thenReturn(missing);
        when(urlPolicy.validateAndNormalize(valid.toString())).thenReturn(valid);
        when(robotsService.check(seed))
                .thenReturn(new CrawlRobotsService.RobotsDecision(true, "allowed"));
        when(crawlFetcher.fetch(seed.toString()))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                seed.toString(),
                                "<a href='/missing'>missing</a><a href='/valid'>valid</a>"));
        when(crawlFetcher.fetch(missing.toString()))
                .thenThrow(new IllegalStateException("HTTP 404"));
        when(crawlFetcher.fetch(valid.toString()))
                .thenReturn(new CrawlFetcher.FetchedPage(200, valid.toString(), "<main>ok</main>"));

        List<URI> discovered = service.discoverUrlsForJob(10L);

        assertThat(discovered).containsExactly(seed, missing, valid);
        verify(crawlFetcher).fetch(valid.toString());
    }

    @Test
    void kepcoRootDiscoveryUsesOfficialSitemapAndAddsNestedBoards() {
        URI seed = URI.create("https://www.kepco.co.kr/");
        URI staticMenu = URI.create("https://www.kepco.co.kr/home/about/conts.do");
        URI locations = URI.create("https://www.kepco.co.kr/home/about/locations.do");
        URI southSeoul =
                URI.create(
                        "https://www.kepco.co.kr/home/about/locations/southseoul/headquarters.do?branchNo=11");
        URI boardList =
                URI.create("https://www.kepco.co.kr/home/disclosure/addisclosure/boardList.do");
        URI subList =
                URI.create(
                        "https://www.kepco.co.kr/home/disclosure/addisclosure/boardSubList.do?boardMngNo=8&pBoardNo=2282");
        URI relatedSite = URI.create("https://www.kepco.co.kr/kemri/news/notice/boardList.do");
        CrawlTarget target = target(10L);
        target.setUrl(seed.toString());
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(urlPolicy.validateAndNormalize(seed.toString())).thenReturn(seed);
        when(urlPolicy.validateAndNormalize(staticMenu.toString())).thenReturn(staticMenu);
        when(urlPolicy.validateAndNormalize(locations.toString())).thenReturn(locations);
        when(urlPolicy.validateAndNormalize(southSeoul.toString())).thenReturn(southSeoul);
        when(urlPolicy.validateAndNormalize(boardList.toString())).thenReturn(boardList);
        when(urlPolicy.validateAndNormalize(subList.toString())).thenReturn(subList);
        when(urlPolicy.validateAndNormalize(relatedSite.toString())).thenReturn(relatedSite);
        when(robotsService.check(seed))
                .thenReturn(new CrawlRobotsService.RobotsDecision(true, "allowed"));
        when(crawlFetcher.fetch("https://www.kepco.co.kr/home/index.do"))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                "https://www.kepco.co.kr/home/index.do",
                                "<div class='sitemap-container'>"
                                        + "<a href='/home/about/conts.do'>about</a>"
                                        + "<a href='/home/about/locations.do'>locations</a>"
                                        + "<a href='/home/disclosure/addisclosure/boardList.do'>board</a>"
                                        + "<a href='/kemri/news/notice/boardList.do'>related site</a>"
                                        + "</div>"));
        when(crawlFetcher.fetch(boardList.toString()))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                boardList.toString(),
                                "<main><a href=\"javascript:fn_SubList('8','2282');\">nested</a></main>"));
        when(crawlFetcher.fetch(locations.toString()))
                .thenReturn(
                        new CrawlFetcher.FetchedPage(
                                200,
                                locations.toString(),
                                "<main><a href=\"javascript:fn_locationsBranch('11','3850','southseoul');\">south seoul</a></main>"));

        List<URI> discovered = service.discoverUrlsForJob(10L);

        assertThat(discovered)
                .containsExactly(seed, staticMenu, locations, boardList, southSeoul, subList);
        assertThat(discovered).noneMatch(uri -> uri.getPath().startsWith("/kemri/"));
    }

    @Test
    void runScheduledEnqueuesScheduleJobAndAlwaysSchedulesNextRun() {
        CrawlTarget target = target(10L);
        target.setScheduleEnabled(true);
        target.setScheduleIntervalMinutes(30);
        when(crawlMapper.findTargetById(10L)).thenReturn(target);
        when(crawlMapper.enqueueJob(10L, "SCHEDULE", null))
                .thenReturn(job(88L, "SCHEDULE", "PENDING"));

        var response = service.runScheduled(10L);

        assertThat(response.enqueued()).isTrue();
        assertThat(response.job().getTriggerType()).isEqualTo("SCHEDULE");
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
    void previewScheduleReturnsNextFiveCronRuns() {
        var response =
                service.previewSchedule(new ScheduleRequest(true, 1440, "CRON", "0 */10 * * * *"));

        assertThat(response.scheduleEnabled()).isTrue();
        assertThat(response.scheduleMode()).isEqualTo("CRON");
        assertThat(response.nextRunTimes()).hasSize(5);
        assertThat(response.nextRunTimes().get(1)).isAfter(response.nextRunTimes().get(0));
    }

    @Test
    void reviewFailureMarksFailedRunReviewed() {
        CrawlRunLog reviewed = new CrawlRunLog();
        reviewed.setCrawlRunLogNo(7L);
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

    @Test
    void staticPageCrawlPreservesBlockAndLineBreakStructure() {
        when(crawlMapper.findTargetById(10L)).thenReturn(target(10L));
        URI uri = URI.create("https://example.com/content");
        String html =
                "<html><head><title>안내</title></head><body><main>"
                        + "<h1>제목</h1><p>첫 번째 문단<br>줄 안쪽 내용</p>"
                        + "<p>두 번째 문단</p><ul><li>항목 하나</li><li>항목 둘</li></ul>"
                        + "</main></body></html>";
        when(robotsService.check(uri))
                .thenReturn(new CrawlRobotsService.RobotsDecision(true, "allowed"));
        when(crawlFetcher.fetch(uri.toString()))
                .thenReturn(new CrawlFetcher.FetchedPage(200, uri.toString(), html));
        ArgumentCaptor<CrawlDocument> documentCaptor = ArgumentCaptor.forClass(CrawlDocument.class);

        service.crawlStaticPageForJob(10L, uri, false);

        verify(crawlMapper).insertDocument(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getContent())
                .contains("제목\n", "첫 번째 문단", "\n줄 안쪽 내용", "\n두 번째 문단", "\n항목 하나", "\n항목 둘")
                .doesNotContain("제목 첫 번째 문단");
    }

    private CrawlTarget target(Long id) {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(id);
        target.setUrl("https://example.com");
        target.setUseYn("Y");
        target.setScheduleMode("INTERVAL");
        target.setScheduleIntervalMinutes(1440);
        return target;
    }

    private CrawlJob job(Long id, String triggerType, String status) {
        CrawlJob job = new CrawlJob();
        job.setCrawlJobNo(id);
        job.setTargetNo(10L);
        job.setTriggerType(triggerType);
        job.setStatus(status);
        return job;
    }
}
