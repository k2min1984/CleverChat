package kr.co.cleverchat.domain.ops.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.MetricSummary;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NoticeRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NotificationChannelRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.StatisticsSummary;
import kr.co.cleverchat.domain.ops.model.AuditLog;
import kr.co.cleverchat.domain.ops.model.Notice;
import kr.co.cleverchat.domain.ops.model.NotificationChannel;
import kr.co.cleverchat.domain.ops.model.NotificationEvent;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AdmOpsApiControllerTest {

    @Mock OpsService opsService;
    @Mock OpsNotificationService notificationService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AdmOpsApiController(opsService, notificationService))
                        .setCustomArgumentResolvers(new CurrentUserResolver())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void statisticsSummaryReturnsEnvelope() throws Exception {
        when(opsService.statisticsSummary())
                .thenReturn(
                        new StatisticsSummary(
                                OffsetDateTime.parse("2026-05-27T13:00:00+09:00"),
                                List.of(new MetricSummary("chatSessions", "Chat sessions", 2, 5))));

        mockMvc.perform(get("/admin/api/statistics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metrics[0].key").value("chatSessions"))
                .andExpect(jsonPath("$.data.metrics[0].today").value(2));
    }

    @Test
    void auditLogsReturnsEnvelopeWithFilters() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(7L);
        log.setActor("admin");
        log.setAction("CREATE");
        OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00+09:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-05-27T23:59:00+09:00");
        when(opsService.auditLogs("admin", "CREATE", "SCENARIO", from, to, 20))
                .thenReturn(List.of(log));

        mockMvc.perform(
                        get("/admin/api/audit-logs")
                                .param("actor", "admin")
                                .param("action", "CREATE")
                                .param("targetType", "SCENARIO")
                                .param("from", "2026-05-01T00:00:00+09:00")
                                .param("to", "2026-05-27T23:59:00+09:00")
                                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].actor").value("admin"));
    }

    @Test
    void auditLogsRejectsInvalidLimitAndLongActor() throws Exception {
        mockMvc.perform(
                        get("/admin/api/audit-logs")
                                .param("actor", "x".repeat(101))
                                .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void noticesReturnsEnvelope() throws Exception {
        Notice notice = notice(1L, "Notice");
        when(opsService.notices(true, 20)).thenReturn(List.of(notice));

        mockMvc.perform(get("/admin/api/notices").param("enabled", "true").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Notice"));
    }

    @Test
    void createNoticeReturnsEnvelope() throws Exception {
        NoticeRequest request = new NoticeRequest("Notice", "Content", true, null, null, 100);
        when(opsService.createNotice(request, 7L)).thenReturn(notice(2L, "Notice"));

        mockMvc.perform(
                        post("/admin/api/notices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void updateNoticeReturnsEnvelope() throws Exception {
        NoticeRequest request = new NoticeRequest("Updated", "Content", false, null, null, 20);
        when(opsService.updateNotice(3L, request, 7L)).thenReturn(notice(3L, "Updated"));

        mockMvc.perform(
                        put("/admin/api/notices/3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"));
    }

    @Test
    void disableNoticeSoftDisables() throws Exception {
        when(opsService.disableNotice(3L, 7L)).thenReturn(notice(3L, "Disabled"));

        mockMvc.perform(delete("/admin/api/notices/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(opsService).disableNotice(3L, 7L);
    }

    @Test
    void noticeValidationRejectsLongTitleAndBadLimit() throws Exception {
        mockMvc.perform(
                        post("/admin/api/notices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new NoticeRequest(
                                                        "x".repeat(201),
                                                        "Content",
                                                        true,
                                                        null,
                                                        null,
                                                        100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/admin/api/notices").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void notificationChannelsReturnsEnvelope() throws Exception {
        NotificationChannel channel = channel(1L, "Ops webhook");
        when(notificationService.channels(true)).thenReturn(List.of(channel));

        mockMvc.perform(get("/admin/api/notifications/channels").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Ops webhook"));
    }

    @Test
    void createNotificationChannelReturnsEnvelope() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest("Ops", "WEBHOOK", true, "OPS_WEBHOOK_URL", null, 60);
        when(notificationService.createChannel(request, 7L)).thenReturn(channel(2L, "Ops"));

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void createSlackNotificationChannelReturnsEnvelope() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest(
                        "Slack",
                        "SLACK_WEBHOOK",
                        true,
                        "OPS_SLACK_WEBHOOK_URL",
                        "OPS_SLACK_WEBHOOK_OLD_URL",
                        60);
        NotificationChannel channel = channel(3L, "Slack");
        channel.setType("SLACK_WEBHOOK");
        channel.setPreviousEndpointEnvKey("OPS_SLACK_WEBHOOK_OLD_URL");
        when(notificationService.createChannel(request, 7L)).thenReturn(channel);

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("SLACK_WEBHOOK"))
                .andExpect(
                        jsonPath("$.data.previousEndpointEnvKey")
                                .value("OPS_SLACK_WEBHOOK_OLD_URL"));
    }

    @Test
    void createEmailNotificationChannelReturnsEnvelope() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest(
                        "Email", "EMAIL_SMTP", true, "OPS_ALERT_EMAIL_TO", null, 60);
        NotificationChannel channel = channel(4L, "Email");
        channel.setType("EMAIL_SMTP");
        channel.setEndpointEnvKey("OPS_ALERT_EMAIL_TO");
        when(notificationService.createChannel(request, 7L)).thenReturn(channel);

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("EMAIL_SMTP"))
                .andExpect(jsonPath("$.data.endpointEnvKey").value("OPS_ALERT_EMAIL_TO"));
    }

    @Test
    void notificationChannelValidationRejectsBadEnvKey() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest("Ops", "WEBHOOK", true, "bad-key", null, 60);

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void notificationChannelValidationRejectsBadPreviousEnvKey() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest(
                        "Ops", "SLACK_WEBHOOK", true, "OPS_WEBHOOK_URL", "bad-key", 60);

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void notificationChannelValidationRejectsUnsupportedType() throws Exception {
        NotificationChannelRequest request =
                new NotificationChannelRequest("Ops", "EMAIL", true, "OPS_WEBHOOK_URL", null, 60);
        when(notificationService.createChannel(request, 7L))
                .thenThrow(new IllegalArgumentException("Unsupported notification channel type."));

        mockMvc.perform(
                        post("/admin/api/notifications/channels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void notificationEventsAndReviewReturnEnvelope() throws Exception {
        NotificationEvent event = new NotificationEvent();
        event.setId(9L);
        event.setEventType("CRAWL_RUN_FAILED");
        event.setStatus("FAILED");
        when(notificationService.events("FAILED", false, 20)).thenReturn(List.of(event));
        when(notificationService.reviewEvent(9L, 7L)).thenReturn(event);

        mockMvc.perform(
                        get("/admin/api/notifications/events")
                                .param("status", "FAILED")
                                .param("reviewed", "false")
                                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(9));

        mockMvc.perform(put("/admin/api/notifications/events/9/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private Notice notice(Long id, String title) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(title);
        notice.setContent("Content");
        notice.setEnabled(true);
        notice.setPriority(100);
        return notice;
    }

    private NotificationChannel channel(Long id, String name) {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(id);
        channel.setName(name);
        channel.setType("WEBHOOK");
        channel.setEnabled(true);
        channel.setEndpointEnvKey("OPS_WEBHOOK_URL");
        channel.setRateLimitPerHour(60);
        return channel;
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
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return new AdminSession(
                    7L, "admin", "Admin", Set.of("ADMIN"), false, LocalDateTime.now());
        }
    }
}
