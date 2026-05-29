package kr.co.cleverchat.domain.ops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NoticeRequest;
import kr.co.cleverchat.domain.ops.mapper.OpsMapper;
import kr.co.cleverchat.domain.ops.model.AuditLog;
import kr.co.cleverchat.domain.ops.model.Notice;
import kr.co.cleverchat.domain.ops.model.OpsMetricRow;
import org.junit.jupiter.api.Test;

class OpsServiceTest {

    private final OpsMapper opsMapper = org.mockito.Mockito.mock(OpsMapper.class);
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-05-27T04:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final OpsService service = new OpsService(opsMapper, clock);

    @Test
    void statisticsSummaryFillsKnownMetricsWithZeroDefaults() {
        OpsMetricRow row = new OpsMetricRow();
        row.setMetricKey("chatSessions");
        row.setTodayCount(3);
        row.setLast7DaysCount(10);
        when(opsMapper.statisticsSummary(
                        OffsetDateTime.parse("2026-05-27T00:00:00+09:00"),
                        OffsetDateTime.parse("2026-05-21T00:00:00+09:00")))
                .thenReturn(List.of(row));

        var summary = service.statisticsSummary();

        assertThat(summary.generatedAt())
                .isEqualTo(OffsetDateTime.parse("2026-05-27T13:00:00+09:00"));
        assertThat(summary.metrics()).hasSize(9);
        assertThat(summary.metrics().get(0).key()).isEqualTo("chatSessions");
        assertThat(summary.metrics().get(0).today()).isEqualTo(3);
        assertThat(summary.metrics())
                .filteredOn(metric -> metric.key().equals("searches"))
                .singleElement()
                .satisfies(
                        metric -> {
                            assertThat(metric.today()).isZero();
                            assertThat(metric.last7Days()).isZero();
                        });
    }

    @Test
    void auditLogsTrimsFiltersAndCapsLimit() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00+09:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-05-27T23:59:00+09:00");
        when(opsMapper.findAuditLogs("admin", "CREATE", "SCENARIO", from, to, 200))
                .thenReturn(List.of(auditLog));

        var logs = service.auditLogs(" admin ", "CREATE", "SCENARIO", from, to, 500);

        assertThat(logs).containsExactly(auditLog);
    }

    @Test
    void auditLogsUsesDefaultLimitForInvalidLimit() {
        when(opsMapper.findAuditLogs(null, null, null, null, null, 50)).thenReturn(List.of());

        var logs = service.auditLogs(" ", null, "", null, null, 0);

        assertThat(logs).isEmpty();
    }

    @Test
    void visibleNoticesUsesCurrentTimeAndSmallLimit() {
        Notice notice = notice(1L, "Visible");
        when(opsMapper.findVisibleNotices(OffsetDateTime.parse("2026-05-27T13:00:00+09:00"), 3))
                .thenReturn(List.of(notice));

        var notices = service.visibleNotices();

        assertThat(notices).containsExactly(notice);
    }

    @Test
    void createNoticeTrimsDefaultsAndStoresActor() {
        Notice stored = notice(7L, "Title");
        doAnswer(
                        invocation -> {
                            Notice notice = invocation.getArgument(0);
                            notice.setId(7L);
                            return null;
                        })
                .when(opsMapper)
                .insertNotice(org.mockito.ArgumentMatchers.any(Notice.class));
        when(opsMapper.findNoticeById(7L)).thenReturn(stored);

        var result =
                service.createNotice(
                        new NoticeRequest(
                                " Title ",
                                " Content ",
                                null,
                                OffsetDateTime.parse("2026-05-27T09:00:00+09:00"),
                                OffsetDateTime.parse("2026-05-28T09:00:00+09:00"),
                                null),
                        3L);

        assertThat(result).isSameAs(stored);
        org.mockito.ArgumentCaptor<Notice> captor =
                org.mockito.ArgumentCaptor.forClass(Notice.class);
        verify(opsMapper).insertNotice(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Title");
        assertThat(captor.getValue().getContent()).isEqualTo("Content");
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().getPriority()).isEqualTo(100);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(3L);
    }

    @Test
    void updateNoticeRejectsInvalidPeriod() {
        NoticeRequest request =
                new NoticeRequest(
                        "Title",
                        "Content",
                        true,
                        OffsetDateTime.parse("2026-05-29T09:00:00+09:00"),
                        OffsetDateTime.parse("2026-05-28T09:00:00+09:00"),
                        100);

        assertThatThrownBy(() -> service.updateNotice(1L, request, 3L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disableNoticeReturnsUpdatedNotice() {
        Notice notice = notice(9L, "Disabled");
        when(opsMapper.disableNotice(9L, 3L)).thenReturn(1);
        when(opsMapper.findNoticeById(9L)).thenReturn(notice);

        var result = service.disableNotice(9L, 3L);

        assertThat(result).isSameAs(notice);
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
}
