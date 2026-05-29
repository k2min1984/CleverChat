package kr.co.cleverchat.domain.ops.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.ops.model.AuditLog;
import kr.co.cleverchat.domain.ops.model.Notice;
import kr.co.cleverchat.domain.ops.model.NotificationChannel;
import kr.co.cleverchat.domain.ops.model.NotificationEvent;
import kr.co.cleverchat.domain.ops.model.OpsMetricRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OpsMapper {

    List<OpsMetricRow> statisticsSummary(
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("sevenDaysStart") OffsetDateTime sevenDaysStart);

    List<AuditLog> findAuditLogs(
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("limit") int limit);

    List<Notice> findNotices(@Param("enabled") Boolean enabled, @Param("limit") int limit);

    List<Notice> findVisibleNotices(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    Notice findNoticeById(@Param("id") Long id);

    void insertNotice(Notice notice);

    int updateNotice(Notice notice);

    int disableNotice(@Param("id") Long id, @Param("updatedBy") Long updatedBy);

    List<NotificationChannel> findNotificationChannels(@Param("enabled") Boolean enabled);

    NotificationChannel findNotificationChannelById(@Param("id") Long id);

    void insertNotificationChannel(NotificationChannel channel);

    int updateNotificationChannel(NotificationChannel channel);

    List<NotificationEvent> findNotificationEvents(
            @Param("status") String status,
            @Param("reviewed") Boolean reviewed,
            @Param("limit") int limit);

    List<NotificationEvent> findDueNotificationEvents(
            @Param("now") OffsetDateTime now,
            @Param("maxAttempts") int maxAttempts,
            @Param("limit") int limit);

    NotificationEvent findNotificationEventById(@Param("id") Long id);

    NotificationEvent findOpenNotificationEvent(
            @Param("eventType") String eventType,
            @Param("sourceType") String sourceType,
            @Param("sourceId") String sourceId);

    void insertNotificationEvent(NotificationEvent event);

    int updateNotificationDelivery(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("attemptCount") int attemptCount,
            @Param("lastError") String lastError,
            @Param("nextRetryAt") OffsetDateTime nextRetryAt);

    int reviewNotificationEvent(@Param("id") Long id, @Param("reviewedBy") Long reviewedBy);
}
