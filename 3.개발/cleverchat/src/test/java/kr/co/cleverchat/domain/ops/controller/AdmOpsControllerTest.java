package kr.co.cleverchat.domain.ops.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.StatisticsSummary;
import kr.co.cleverchat.domain.ops.model.NotificationChannel;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmOpsControllerTest {

    @Mock OpsService opsService;
    @Mock OpsNotificationService notificationService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AdmOpsController(opsService, notificationService))
                        .build();
    }

    @Test
    void statisticsRendersModel() throws Exception {
        when(opsService.statisticsSummary())
                .thenReturn(new StatisticsSummary(OffsetDateTime.now(), List.of()));

        mockMvc.perform(get("/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/ops/statistics"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    void auditLogsRendersModel() throws Exception {
        when(opsService.auditLogs("admin", null, null, null, null, 50)).thenReturn(List.of());

        mockMvc.perform(get("/admin/audit-logs").param("actor", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/ops/auditLogList"))
                .andExpect(model().attributeExists("logs"))
                .andExpect(model().attribute("actor", "admin"));
    }

    @Test
    void noticesRendersModel() throws Exception {
        when(opsService.notices(null, 100)).thenReturn(List.of());

        mockMvc.perform(get("/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/ops/noticeList"))
                .andExpect(model().attributeExists("notices"));
    }

    @Test
    void notificationsRendersModel() throws Exception {
        NotificationChannel channel = new NotificationChannel();
        channel.setNotificationChannelNo(1L);
        channel.setName("Slack");
        channel.setType("SLACK_WEBHOOK");
        channel.setUseYn("Y");
        channel.setEndpointEnvKey("OPS_SLACK_WEBHOOK_URL");
        channel.setPreviousEndpointEnvKey("OPS_SLACK_WEBHOOK_OLD_URL");
        channel.setRateLimitPerHour(60);
        when(notificationService.channels(null)).thenReturn(List.of(channel));
        when(notificationService.events(null, null, 100)).thenReturn(List.of());

        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/ops/notificationList"))
                .andExpect(model().attributeExists("channels"))
                .andExpect(model().attributeExists("events"));
    }
}
