package kr.co.cleverchat.domain.ops.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.MetricSummary;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NoticeRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.StatisticsSummary;
import kr.co.cleverchat.domain.ops.mapper.OpsMapper;
import kr.co.cleverchat.domain.ops.model.AuditLog;
import kr.co.cleverchat.domain.ops.model.Notice;
import kr.co.cleverchat.domain.ops.model.OpsMetricRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_NOTICE_PRIORITY = 100;
    private static final int DEFAULT_VISIBLE_NOTICE_LIMIT = 3;

    private final OpsMapper opsMapper;
    private final Clock clock;

    @Autowired
    public OpsService(OpsMapper opsMapper) {
        this(opsMapper, Clock.system(SEOUL));
    }

    OpsService(OpsMapper opsMapper, Clock clock) {
        this.opsMapper = opsMapper;
        this.clock = clock;
    }

    public StatisticsSummary statisticsSummary() {
        LocalDate today = LocalDate.now(clock);
        OffsetDateTime todayStart = today.atStartOfDay(SEOUL).toOffsetDateTime();
        OffsetDateTime sevenDaysStart = today.minusDays(6).atStartOfDay(SEOUL).toOffsetDateTime();
        Map<String, MetricSummary> summaries = new LinkedHashMap<>();
        metricLabels()
                .forEach((key, label) -> summaries.put(key, new MetricSummary(key, label, 0, 0)));

        for (OpsMetricRow row : opsMapper.statisticsSummary(todayStart, sevenDaysStart)) {
            String label = metricLabels().getOrDefault(row.getMetricKey(), row.getMetricKey());
            summaries.put(
                    row.getMetricKey(),
                    new MetricSummary(
                            row.getMetricKey(),
                            label,
                            row.getTodayCount(),
                            row.getLast7DaysCount()));
        }

        return new StatisticsSummary(OffsetDateTime.now(clock), List.copyOf(summaries.values()));
    }

    public List<AuditLog> auditLogs(
            String actor,
            String action,
            String targetType,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer limit) {
        return opsMapper.findAuditLogs(
                blankToNull(actor),
                blankToNull(action),
                blankToNull(targetType),
                from,
                to,
                normalizeLimit(limit));
    }

    public List<Notice> notices(Boolean enabled, Integer limit) {
        return opsMapper.findNotices(enabled, normalizeLimit(limit));
    }

    public List<Notice> visibleNotices() {
        return opsMapper.findVisibleNotices(
                OffsetDateTime.now(clock), DEFAULT_VISIBLE_NOTICE_LIMIT);
    }

    @Audited(action = "NOTICE_CREATE", targetType = "NOTICE")
    @RequireRole("OPERATOR")
    public Notice createNotice(NoticeRequest request, Long actorId) {
        Notice notice = toNotice(null, request, actorId);
        opsMapper.insertNotice(notice);
        return opsMapper.findNoticeById(notice.getNoticeNo());
    }

    @Audited(action = "NOTICE_UPDATE", targetType = "NOTICE")
    @RequireRole("OPERATOR")
    public Notice updateNotice(Long id, NoticeRequest request, Long actorId) {
        Notice notice = toNotice(id, request, actorId);
        int updated = opsMapper.updateNotice(notice);
        if (updated == 0) {
            throw new IllegalArgumentException("Notice not found.");
        }
        return opsMapper.findNoticeById(id);
    }

    @Audited(action = "NOTICE_DISABLE", targetType = "NOTICE")
    @RequireRole("OPERATOR")
    public Notice disableNotice(Long id, Long actorId) {
        int updated = opsMapper.disableNotice(id, actorId);
        if (updated == 0) {
            throw new IllegalArgumentException("Notice not found.");
        }
        return opsMapper.findNoticeById(id);
    }

    private Notice toNotice(Long id, NoticeRequest request, Long actorId) {
        validateNoticePeriod(request);
        Notice notice = new Notice();
        notice.setNoticeNo(id);
        notice.setTitle(trimRequired(request.title()));
        notice.setContent(trimRequired(request.content()));
        notice.setUseYn("N".equals(request.useYn()) ? "N" : "Y");
        notice.setStartsAt(request.startsAt());
        notice.setEndsAt(request.endsAt());
        notice.setPriority(
                request.priority() == null ? DEFAULT_NOTICE_PRIORITY : request.priority());
        notice.setFrstRegrEmpno(actorId);
        notice.setLstChgrEmpno(actorId);
        return notice;
    }

    private void validateNoticePeriod(NoticeRequest request) {
        if (request.startsAt() != null
                && request.endsAt() != null
                && request.startsAt().isAfter(request.endsAt())) {
            throw new IllegalArgumentException("Notice period is invalid.");
        }
    }

    private String trimRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value is blank.");
        }
        return value.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, String> metricLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("chatSessions", "Chat sessions");
        labels.put("chatMessages", "Chat messages");
        labels.put("chatFailures", "Chat failures");
        labels.put("chatFeedback", "Chat feedback");
        labels.put("searches", "Searches");
        labels.put("searchPiiBlocks", "Search PII blocks");
        labels.put("crawlFailures", "Crawl failures");
        labels.put("unreviewedCrawlFailures", "Unreviewed crawl failures");
        labels.put("activeScenarios", "Active scenarios");
        return labels;
    }
}
