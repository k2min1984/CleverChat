package kr.co.cleverchat.domain.crawl.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RetentionResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.SchedulePreviewResponse;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AdmCrawlApiControllerTest {

    @Mock CrawlService crawlService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdmCrawlApiController(crawlService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new CurrentUserResolver())
                        .build();
    }

    @Test
    void createTargetReturnsEnvelope() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setId(1L);
        target.setUrl("https://example.com/help");
        target.setEnabled(true);
        when(crawlService.createTarget(org.mockito.Mockito.any(), org.mockito.Mockito.eq(10L)))
                .thenReturn(target);

        mockMvc.perform(
                        post("/admin/api/crawl-targets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"url\":\"https://example.com/help\",\"label\":\"Help\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void runReturnsEnvelope() throws Exception {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setStatus("SUCCESS");
        CrawlDocument document = new CrawlDocument();
        document.setId(9L);
        when(crawlService.run(1L)).thenReturn(new RunResponse(runLog, document));

        mockMvc.perform(post("/admin/api/crawl-targets/1/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.run.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.document.id").value(9));
    }

    @Test
    void documentsReturnEnvelope() throws Exception {
        CrawlDocument document = new CrawlDocument();
        document.setId(9L);
        when(crawlService.documents(1L)).thenReturn(List.of(document));

        mockMvc.perform(get("/admin/api/crawl-documents").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(9));
    }

    @Test
    void updateScheduleReturnsEnvelope() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setId(1L);
        target.setScheduleEnabled(true);
        target.setScheduleIntervalMinutes(60);
        target.setScheduleMode("INTERVAL");
        when(crawlService.updateSchedule(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any()))
                .thenReturn(target);

        mockMvc.perform(
                        put("/admin/api/crawl-targets/1/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scheduleEnabled\":true,\"scheduleIntervalMinutes\":60,\"scheduleMode\":\"INTERVAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleEnabled").value(true))
                .andExpect(jsonPath("$.data.scheduleIntervalMinutes").value(60));
    }

    @Test
    void updateScheduleAcceptsCronMode() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setId(1L);
        target.setScheduleEnabled(true);
        target.setScheduleMode("CRON");
        target.setScheduleCron("0 */10 * * * *");
        when(crawlService.updateSchedule(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any()))
                .thenReturn(target);

        mockMvc.perform(
                        put("/admin/api/crawl-targets/1/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scheduleEnabled\":true,\"scheduleMode\":\"CRON\",\"scheduleCron\":\"0 */10 * * * *\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleMode").value("CRON"))
                .andExpect(jsonPath("$.data.scheduleCron").value("0 */10 * * * *"));
    }

    @Test
    void updateScheduleRejectsInvalidMode() throws Exception {
        mockMvc.perform(
                        put("/admin/api/crawl-targets/1/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scheduleEnabled\":true,\"scheduleMode\":\"EVERY_SECOND\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateScheduleRejectsOutOfRangeInterval() throws Exception {
        mockMvc.perform(
                        put("/admin/api/crawl-targets/1/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scheduleEnabled\":true,\"scheduleIntervalMinutes\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void previewScheduleReturnsEnvelope() throws Exception {
        when(crawlService.previewSchedule(org.mockito.Mockito.any()))
                .thenReturn(
                        new SchedulePreviewResponse(
                                true,
                                "CRON",
                                List.of(
                                        OffsetDateTime.parse("2026-05-29T10:30:00+09:00"),
                                        OffsetDateTime.parse("2026-05-29T10:40:00+09:00"))));

        mockMvc.perform(
                        post("/admin/api/crawl-targets/schedule/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scheduleEnabled\":true,\"scheduleMode\":\"CRON\",\"scheduleCron\":\"0 */10 * * * *\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleMode").value("CRON"))
                .andExpect(jsonPath("$.data.nextRunTimes[0]").exists());
    }

    @Test
    void runsReturnEnvelope() throws Exception {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setId(3L);
        runLog.setStatus("FAILED");
        runLog.setFailureCode("HTTP_ERROR");
        when(crawlService.runLogs(1L, "FAILED", "HTTP_ERROR", 20)).thenReturn(List.of(runLog));

        mockMvc.perform(
                        get("/admin/api/crawl-runs")
                                .param("targetId", "1")
                                .param("status", "FAILED")
                                .param("failureCode", "HTTP_ERROR")
                                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].failureCode").value("HTTP_ERROR"));
    }

    @Test
    void runsRejectsInvalidLimitAndStatus() throws Exception {
        mockMvc.perform(
                        get("/admin/api/crawl-runs")
                                .param("status", "DROP_TABLE")
                                .param("limit", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void failuresReturnEnvelope() throws Exception {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setId(4L);
        runLog.setStatus("FAILED");
        runLog.setFailureCode("ROBOTS_BLOCKED");
        when(crawlService.failedRunLogs(false, "ROBOTS_BLOCKED", 10)).thenReturn(List.of(runLog));

        mockMvc.perform(
                        get("/admin/api/crawl-runs/failures")
                                .param("reviewed", "false")
                                .param("failureCode", "ROBOTS_BLOCKED")
                                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data[0].failureCode").value("ROBOTS_BLOCKED"));
    }

    @Test
    void reviewFailureReturnsEnvelope() throws Exception {
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setId(4L);
        runLog.setReviewed(true);
        when(crawlService.reviewFailure(
                        org.mockito.Mockito.eq(4L),
                        org.mockito.Mockito.any(),
                        org.mockito.Mockito.eq(10L)))
                .thenReturn(runLog);

        mockMvc.perform(
                        put("/admin/api/crawl-runs/4/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewed").value(true));
    }

    @Test
    void reviewFailureRejectsLongComment() throws Exception {
        String longComment = "x".repeat(1001);

        mockMvc.perform(
                        put("/admin/api/crawl-runs/4/review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"" + longComment + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteExpiredReturnsEnvelope() throws Exception {
        when(crawlService.deleteExpired(365, 90, true))
                .thenReturn(
                        new RetentionResponse(
                                365,
                                90,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now(),
                                3,
                                2,
                                0,
                                0,
                                true));

        mockMvc.perform(
                        delete("/admin/api/crawl-runs/expired")
                                .param("runRetentionDays", "365")
                                .param("documentRetentionDays", "90")
                                .param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.wouldDeleteRunLogs").value(3));
    }

    @Test
    void deleteExpiredRejectsTooSmallRetentionDays() throws Exception {
        mockMvc.perform(
                        delete("/admin/api/crawl-runs/expired")
                                .param("runRetentionDays", "29")
                                .param("documentRetentionDays", "90")
                                .param("dryRun", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private static class CurrentUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return new AdminSession(
                    10L, "admin", "Admin", Set.of("ADMIN"), false, LocalDateTime.now());
        }
    }
}
