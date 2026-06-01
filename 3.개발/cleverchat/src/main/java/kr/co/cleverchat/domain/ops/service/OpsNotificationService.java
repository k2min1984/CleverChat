package kr.co.cleverchat.domain.ops.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NotificationChannelRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NotificationTestResponse;
import kr.co.cleverchat.domain.ops.mapper.OpsMapper;
import kr.co.cleverchat.domain.ops.model.NotificationChannel;
import kr.co.cleverchat.domain.ops.model.NotificationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpsNotificationService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_RATE_LIMIT_PER_HOUR = 60;
    private static final int DEFAULT_EVENT_LIMIT = 100;
    private static final int MAX_EVENT_LIMIT = 200;
    private static final int MAX_ATTEMPTS = 3;
    private static final String TYPE_WEBHOOK = "WEBHOOK";
    private static final String TYPE_SLACK_WEBHOOK = "SLACK_WEBHOOK";
    private static final String TYPE_EMAIL_SMTP = "EMAIL_SMTP";

    private final OpsMapper opsMapper;
    private final Environment environment;
    private final NotificationWebhookSender webhookSender;
    private final NotificationEmailSender emailSender;
    private final Clock clock;

    @Autowired
    public OpsNotificationService(
            OpsMapper opsMapper,
            Environment environment,
            NotificationWebhookSender webhookSender,
            NotificationEmailSender emailSender) {
        this(opsMapper, environment, webhookSender, emailSender, Clock.system(SEOUL));
    }

    OpsNotificationService(
            OpsMapper opsMapper,
            Environment environment,
            NotificationWebhookSender webhookSender,
            NotificationEmailSender emailSender,
            Clock clock) {
        this.opsMapper = opsMapper;
        this.environment = environment;
        this.webhookSender = webhookSender;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NotificationChannel> channels(Boolean enabled) {
        return opsMapper.findNotificationChannels(enabled);
    }

    @Transactional
    @Audited(action = "NOTIFICATION_CHANNEL_CREATE", targetType = "NOTIFICATION_CHANNEL")
    @RequireRole("OPERATOR")
    public NotificationChannel createChannel(NotificationChannelRequest request, Long actorId) {
        NotificationChannel channel = toChannel(null, request, actorId);
        opsMapper.insertNotificationChannel(channel);
        return opsMapper.findNotificationChannelById(channel.getNotificationChannelNo());
    }

    @Transactional
    @Audited(action = "NOTIFICATION_CHANNEL_UPDATE", targetType = "NOTIFICATION_CHANNEL")
    @RequireRole("OPERATOR")
    public NotificationChannel updateChannel(
            Long id, NotificationChannelRequest request, Long actorId) {
        NotificationChannel channel = toChannel(id, request, actorId);
        int updated = opsMapper.updateNotificationChannel(channel);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Notification channel not found.");
        }
        return opsMapper.findNotificationChannelById(id);
    }

    @Transactional(readOnly = true)
    public List<NotificationEvent> events(String status, Boolean reviewed, Integer limit) {
        return opsMapper.findNotificationEvents(
                blankToNull(status), reviewed, normalizeLimit(limit));
    }

    @Transactional
    @RequireRole("OPERATOR")
    public NotificationEvent reviewEvent(Long id, Long reviewedBy) {
        int updated = opsMapper.reviewNotificationEvent(id, reviewedBy);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Notification event not found.");
        }
        return opsMapper.findNotificationEventById(id);
    }

    @Transactional
    @RequireRole("OPERATOR")
    public NotificationTestResponse testChannel(Long channelId) {
        NotificationChannel channel = requireChannel(channelId);
        NotificationEvent event =
                newEvent(
                        testEventType(channel),
                        "NOTIFICATION_CHANNEL",
                        channelId + ":" + OffsetDateTime.now(clock).toInstant().toEpochMilli(),
                        "INFO",
                        "Webhook test for channel " + channel.getName());
        opsMapper.insertNotificationEvent(event);
        NotificationEvent delivered = deliverEvent(event, List.of(channel));
        return new NotificationTestResponse(
                delivered.getNotificationEventNo(),
                "SENT".equals(delivered.getStatus()),
                delivered.getStatus(),
                delivered.getLastError() == null ? "SENT" : delivered.getLastError());
    }

    @Transactional
    public void notifyCrawlFailure(CrawlRunLog runLog) {
        if (runLog == null || runLog.getCrawlRunLogNo() == null) {
            return;
        }
        NotificationEvent event =
                findOrCreateOpenEvent(
                        "CRAWL_RUN_FAILED",
                        "CRAWL_RUN_LOG",
                        String.valueOf(runLog.getCrawlRunLogNo()),
                        "ERROR",
                        "Crawl run failed: #" + runLog.getCrawlRunLogNo());
        if ("SENT".equals(event.getStatus())) {
            return;
        }
        deliverEvent(event, opsMapper.findNotificationChannels(true));
    }

    @Scheduled(fixedDelayString = "${cleverchat.notification.retry.fixed-delay-ms:60000}")
    @Transactional
    public int retryDueEvents() {
        List<NotificationEvent> events =
                opsMapper.findDueNotificationEvents(OffsetDateTime.now(clock), MAX_ATTEMPTS, 20);
        List<NotificationChannel> channels = opsMapper.findNotificationChannels(true);
        for (NotificationEvent event : events) {
            deliverEvent(event, channels);
        }
        return events.size();
    }

    private NotificationEvent deliverEvent(
            NotificationEvent event, List<NotificationChannel> channels) {
        NotificationEvent current = opsMapper.findNotificationEventById(event.getNotificationEventNo());
        int attempts = current.getAttemptCount() == null ? 0 : current.getAttemptCount();
        if (attempts >= MAX_ATTEMPTS) {
            return current;
        }
        NotificationChannel channel = firstEnabledChannel(channels);
        if (channel == null) {
            updateDelivery(current, "FAILED", attempts + 1, "NO_ENABLED_CHANNEL");
            return opsMapper.findNotificationEventById(current.getNotificationEventNo());
        }
        if (!"Y".equals(channel.getUseYn())) {
            updateDelivery(current, "FAILED", attempts + 1, "CHANNEL_DISABLED");
            return opsMapper.findNotificationEventById(current.getNotificationEventNo());
        }
        DeliveryResult result;
        if (TYPE_EMAIL_SMTP.equals(channel.getType())) {
            RecipientResolution recipients = recipients(channel.getEndpointEnvKey());
            if (recipients.missing()) {
                updateDelivery(current, "FAILED", attempts + 1, "EMAIL_RECIPIENT_ENV_MISSING");
                return opsMapper.findNotificationEventById(current.getNotificationEventNo());
            }
            result = sendEmail(current, recipients.values());
        } else {
            EndpointResolution endpoint = endpoint(channel.getEndpointEnvKey());
            if (endpoint.missing()) {
                endpoint = endpoint(channel.getPreviousEndpointEnvKey());
                if (endpoint.missing()) {
                    updateDelivery(current, "FAILED", attempts + 1, "ENDPOINT_ENV_MISSING");
                    return opsMapper.findNotificationEventById(current.getNotificationEventNo());
                }
            }
            result = sendWebhook(channel, current, endpoint.value());
            if (!result.success() && channel.getPreviousEndpointEnvKey() != null) {
                EndpointResolution previousEndpoint = endpoint(channel.getPreviousEndpointEnvKey());
                if (!previousEndpoint.missing()) {
                    result = sendWebhook(channel, current, previousEndpoint.value());
                }
            }
        }
        if (result.success()) {
            updateDelivery(current, "SENT", attempts + 1, null);
        } else {
            String status = attempts + 1 >= MAX_ATTEMPTS ? "FAILED" : "RETRY";
            updateDelivery(current, status, attempts + 1, result.message());
        }
        return opsMapper.findNotificationEventById(current.getNotificationEventNo());
    }

    private NotificationEvent findOrCreateOpenEvent(
            String eventType, String sourceType, String sourceId, String severity, String summary) {
        NotificationEvent existing =
                opsMapper.findOpenNotificationEvent(eventType, sourceType, sourceId);
        if (existing != null) {
            return existing;
        }
        NotificationEvent event = newEvent(eventType, sourceType, sourceId, severity, summary);
        opsMapper.insertNotificationEvent(event);
        return event;
    }

    private NotificationEvent newEvent(
            String eventType, String sourceType, String sourceId, String severity, String summary) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(eventType);
        event.setSourceType(sourceType);
        event.setSourceId(sourceId);
        event.setSeverity(severity);
        event.setSummary(truncate(summary, 500));
        event.setStatus("PENDING");
        event.setAttemptCount(0);
        event.setReviewed(false);
        return event;
    }

    private void updateDelivery(
            NotificationEvent event, String status, int attempts, String lastError) {
        OffsetDateTime nextRetryAt =
                "RETRY".equals(status)
                        ? OffsetDateTime.now(clock).plusMinutes(Math.min(30, attempts * 5L))
                        : null;
        opsMapper.updateNotificationDelivery(
                event.getNotificationEventNo(), status, attempts, truncate(lastError, 500), nextRetryAt);
    }

    private NotificationWebhookPayload payload(NotificationEvent event) {
        return new NotificationWebhookPayload(
                event.getNotificationEventNo(),
                event.getEventType(),
                event.getSeverity(),
                event.getSummary(),
                "/admin/notifications",
                event.getFrstRegDt() == null ? OffsetDateTime.now(clock) : event.getFrstRegDt());
    }

    private NotificationEmailPayload emailPayload(NotificationEvent event) {
        return new NotificationEmailPayload(
                event.getNotificationEventNo(),
                event.getEventType(),
                event.getSeverity(),
                event.getSummary(),
                "/admin/notifications",
                event.getFrstRegDt() == null ? OffsetDateTime.now(clock) : event.getFrstRegDt());
    }

    private SlackWebhookPayload slackPayload(NotificationEvent event) {
        String text = "[" + event.getSeverity() + "] " + event.getSummary();
        return new SlackWebhookPayload(
                text,
                List.of(
                        Map.of(
                                "type",
                                "section",
                                "text",
                                Map.of(
                                        "type",
                                        "mrkdwn",
                                        "text",
                                        "*" + event.getEventType() + "*\n" + text)),
                        Map.of(
                                "type",
                                "context",
                                "elements",
                                List.of(
                                        Map.of(
                                                "type",
                                                "mrkdwn",
                                                "text",
                                                "Event #"
                                                        + event.getNotificationEventNo()
                                                        + " | /admin/notifications")))));
    }

    private DeliveryResult sendWebhook(
            NotificationChannel channel, NotificationEvent event, String endpoint) {
        Object payload =
                TYPE_SLACK_WEBHOOK.equals(channel.getType()) ? slackPayload(event) : payload(event);
        NotificationWebhookSender.DeliveryResult result = webhookSender.send(endpoint, payload);
        return new DeliveryResult(result.success(), result.message());
    }

    private DeliveryResult sendEmail(NotificationEvent event, List<String> recipients) {
        NotificationEmailSender.DeliveryResult result =
                emailSender.send(recipients, emailPayload(event));
        return new DeliveryResult(result.success(), result.message());
    }

    private NotificationChannel firstEnabledChannel(List<NotificationChannel> channels) {
        return channels.stream()
                .filter(channel -> "Y".equals(channel.getUseYn()))
                .filter(
                        channel ->
                                TYPE_WEBHOOK.equals(channel.getType())
                                        || TYPE_SLACK_WEBHOOK.equals(channel.getType())
                                        || TYPE_EMAIL_SMTP.equals(channel.getType()))
                .findFirst()
                .orElse(null);
    }

    private NotificationChannel requireChannel(Long id) {
        NotificationChannel channel = opsMapper.findNotificationChannelById(id);
        if (channel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Notification channel not found.");
        }
        return channel;
    }

    private NotificationChannel toChannel(
            Long id, NotificationChannelRequest request, Long actorId) {
        String type =
                request.type() == null || request.type().isBlank()
                        ? TYPE_WEBHOOK
                        : request.type().trim().toUpperCase();
        if (!TYPE_WEBHOOK.equals(type)
                && !TYPE_SLACK_WEBHOOK.equals(type)
                && !TYPE_EMAIL_SMTP.equals(type)) {
            throw new IllegalArgumentException("Unsupported notification channel type.");
        }
        NotificationChannel channel = new NotificationChannel();
        channel.setNotificationChannelNo(id);
        channel.setName(trimRequired(request.name()));
        channel.setType(type);
        channel.setUseYn("N".equals(request.useYn()) ? "N" : "Y");
        channel.setEndpointEnvKey(trimRequired(request.endpointEnvKey()).toUpperCase());
        channel.setPreviousEndpointEnvKey(
                TYPE_EMAIL_SMTP.equals(type)
                                || blankToNull(request.previousEndpointEnvKey()) == null
                        ? null
                        : request.previousEndpointEnvKey().trim().toUpperCase());
        channel.setRateLimitPerHour(
                request.rateLimitPerHour() == null
                        ? DEFAULT_RATE_LIMIT_PER_HOUR
                        : request.rateLimitPerHour());
        channel.setFrstRegrEmpno(actorId);
        channel.setLstChgrEmpno(actorId);
        return channel;
    }

    private EndpointResolution endpoint(String envKey) {
        if (envKey == null || envKey.isBlank()) {
            return new EndpointResolution(null);
        }
        String value = environment.getProperty(envKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        return new EndpointResolution(blankToNull(value));
    }

    private RecipientResolution recipients(String envKey) {
        if (envKey == null || envKey.isBlank()) {
            return new RecipientResolution(List.of());
        }
        String value = environment.getProperty(envKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        if (value == null || value.isBlank()) {
            return new RecipientResolution(List.of());
        }
        List<String> recipients =
                Arrays.stream(value.split("[,;\\s]+"))
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .toList();
        return new RecipientResolution(recipients);
    }

    private String testEventType(NotificationChannel channel) {
        if (TYPE_EMAIL_SMTP.equals(channel.getType())) {
            return "TEST_EMAIL_SMTP";
        }
        return TYPE_SLACK_WEBHOOK.equals(channel.getType()) ? "TEST_SLACK_WEBHOOK" : "TEST_WEBHOOK";
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_EVENT_LIMIT;
        }
        return Math.min(limit, MAX_EVENT_LIMIT);
    }

    private String trimRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value is blank.");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record EndpointResolution(String value) {
        boolean missing() {
            return value == null;
        }
    }

    private record RecipientResolution(List<String> values) {
        boolean missing() {
            return values == null || values.isEmpty();
        }
    }

    private record DeliveryResult(boolean success, String message) {}
}
