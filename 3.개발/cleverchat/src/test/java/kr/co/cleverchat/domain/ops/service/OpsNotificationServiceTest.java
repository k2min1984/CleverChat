package kr.co.cleverchat.domain.ops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NotificationChannelRequest;
import kr.co.cleverchat.domain.ops.mapper.OpsMapper;
import kr.co.cleverchat.domain.ops.model.NotificationChannel;
import kr.co.cleverchat.domain.ops.model.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

class OpsNotificationServiceTest {

    private final OpsMapper opsMapper = org.mockito.Mockito.mock(OpsMapper.class);
    private final Environment environment = org.mockito.Mockito.mock(Environment.class);
    private final NotificationWebhookSender webhookSender =
            org.mockito.Mockito.mock(NotificationWebhookSender.class);
    private final NotificationEmailSender emailSender =
            org.mockito.Mockito.mock(NotificationEmailSender.class);
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-05-28T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final OpsNotificationService service =
            new OpsNotificationService(opsMapper, environment, webhookSender, emailSender, clock);

    @Test
    void createChannelStoresEnvKeyNotSecret() {
        doAnswer(
                        invocation -> {
                            NotificationChannel channel = invocation.getArgument(0);
                            channel.setNotificationChannelNo(7L);
                            return null;
                        })
                .when(opsMapper)
                .insertNotificationChannel(any(NotificationChannel.class));
        NotificationChannel stored = channel(7L, "Ops", true);
        when(opsMapper.findNotificationChannelById(7L)).thenReturn(stored);

        var result =
                service.createChannel(
                        new NotificationChannelRequest(
                                " Ops ", null, "Y", "OPS_WEBHOOK_URL", null, null),
                        3L);

        assertThat(result).isSameAs(stored);
        ArgumentCaptor<NotificationChannel> captor =
                ArgumentCaptor.forClass(NotificationChannel.class);
        verify(opsMapper).insertNotificationChannel(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ops");
        assertThat(captor.getValue().getType()).isEqualTo("WEBHOOK");
        assertThat(captor.getValue().getEndpointEnvKey()).isEqualTo("OPS_WEBHOOK_URL");
        assertThat(captor.getValue().getRateLimitPerHour()).isEqualTo(60);
    }

    @Test
    void createSlackChannelStoresTypeAndPreviousEnvKey() {
        doAnswer(
                        invocation -> {
                            NotificationChannel channel = invocation.getArgument(0);
                            channel.setNotificationChannelNo(8L);
                            return null;
                        })
                .when(opsMapper)
                .insertNotificationChannel(any(NotificationChannel.class));
        NotificationChannel stored = channel(8L, "Slack", true);
        stored.setType("SLACK_WEBHOOK");
        stored.setPreviousEndpointEnvKey("OPS_SLACK_WEBHOOK_OLD_URL");
        when(opsMapper.findNotificationChannelById(8L)).thenReturn(stored);

        service.createChannel(
                new NotificationChannelRequest(
                        "Slack",
                        "slack_webhook",
                        "Y",
                        "OPS_SLACK_WEBHOOK_URL",
                        "OPS_SLACK_WEBHOOK_OLD_URL",
                        60),
                3L);

        ArgumentCaptor<NotificationChannel> captor =
                ArgumentCaptor.forClass(NotificationChannel.class);
        verify(opsMapper).insertNotificationChannel(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("SLACK_WEBHOOK");
        assertThat(captor.getValue().getEndpointEnvKey()).isEqualTo("OPS_SLACK_WEBHOOK_URL");
        assertThat(captor.getValue().getPreviousEndpointEnvKey())
                .isEqualTo("OPS_SLACK_WEBHOOK_OLD_URL");
    }

    @Test
    void createChannelRejectsUnsupportedType() {
        assertThatThrownBy(
                        () ->
                                service.createChannel(
                                        new NotificationChannelRequest(
                                                "Ops", "EMAIL", "Y", "OPS_WEBHOOK_URL", null, 60),
                                        3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported notification channel type");
    }

    @Test
    void createEmailChannelStoresTypeAndRecipientEnvKey() {
        doAnswer(
                        invocation -> {
                            NotificationChannel channel = invocation.getArgument(0);
                            channel.setNotificationChannelNo(9L);
                            return null;
                        })
                .when(opsMapper)
                .insertNotificationChannel(any(NotificationChannel.class));
        NotificationChannel stored = channel(9L, "Email", true);
        stored.setType("EMAIL_SMTP");
        stored.setEndpointEnvKey("OPS_ALERT_EMAIL_TO");
        when(opsMapper.findNotificationChannelById(9L)).thenReturn(stored);

        service.createChannel(
                new NotificationChannelRequest(
                        "Email", "email_smtp", "Y", "OPS_ALERT_EMAIL_TO", "IGNORED_OLD_KEY", 60),
                3L);

        ArgumentCaptor<NotificationChannel> captor =
                ArgumentCaptor.forClass(NotificationChannel.class);
        verify(opsMapper).insertNotificationChannel(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("EMAIL_SMTP");
        assertThat(captor.getValue().getEndpointEnvKey()).isEqualTo("OPS_ALERT_EMAIL_TO");
        assertThat(captor.getValue().getPreviousEndpointEnvKey()).isNull();
    }

    @Test
    void testChannelMarksSentWhenWebhookSucceeds() {
        NotificationChannel channel = channel(5L, "Ops", true);
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_WEBHOOK_URL"))
                .thenReturn("https://hooks.example.test/ops");
        when(webhookSender.send(any(), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isTrue();
        assertThat(response.status()).isEqualTo("SENT");
        verify(webhookSender)
                .send(org.mockito.ArgumentMatchers.eq("https://hooks.example.test/ops"), any());
    }

    @Test
    void missingEnvKeyFailsWithoutSendingSecret() {
        NotificationChannel channel = channel(5L, "Ops", true);
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_WEBHOOK_URL")).thenReturn(null);
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isFalse();
        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).isEqualTo("ENDPOINT_ENV_MISSING");
        verify(webhookSender, never()).send(any(), any());
    }

    @Test
    void previousEnvKeyIsUsedWhenPrimaryIsMissing() {
        NotificationChannel channel = channel(5L, "Ops", true);
        channel.setPreviousEndpointEnvKey("OPS_WEBHOOK_OLD_URL");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_WEBHOOK_URL")).thenReturn(null);
        when(environment.getProperty("OPS_WEBHOOK_OLD_URL"))
                .thenReturn("https://hooks.example.test/old");
        when(webhookSender.send(any(), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isTrue();
        verify(webhookSender)
                .send(org.mockito.ArgumentMatchers.eq("https://hooks.example.test/old"), any());
    }

    @Test
    void previousEnvKeyIsUsedWhenPrimaryDeliveryFails() {
        NotificationChannel channel = channel(5L, "Ops", true);
        channel.setPreviousEndpointEnvKey("OPS_WEBHOOK_OLD_URL");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_WEBHOOK_URL"))
                .thenReturn("https://hooks.example.test/new");
        when(environment.getProperty("OPS_WEBHOOK_OLD_URL"))
                .thenReturn("https://hooks.example.test/old");
        when(webhookSender.send(
                        org.mockito.ArgumentMatchers.eq("https://hooks.example.test/new"), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(false, "SEND_FAILED"));
        when(webhookSender.send(
                        org.mockito.ArgumentMatchers.eq("https://hooks.example.test/old"), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isTrue();
        verify(webhookSender)
                .send(org.mockito.ArgumentMatchers.eq("https://hooks.example.test/new"), any());
        verify(webhookSender)
                .send(org.mockito.ArgumentMatchers.eq("https://hooks.example.test/old"), any());
    }

    @Test
    void slackChannelSendsSlackPayload() {
        NotificationChannel channel = channel(5L, "Slack", true);
        channel.setType("SLACK_WEBHOOK");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_WEBHOOK_URL")).thenReturn("https://hooks.slack.test/ops");
        when(webhookSender.send(any(), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        service.testChannel(5L);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(webhookSender)
                .send(
                        org.mockito.ArgumentMatchers.eq("https://hooks.slack.test/ops"),
                        payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isInstanceOf(SlackWebhookPayload.class);
        SlackWebhookPayload payload = (SlackWebhookPayload) payloadCaptor.getValue();
        assertThat(payload.text()).contains("Webhook test for channel Slack");
        assertThat(payload.blocks()).isNotEmpty();
    }

    @Test
    void emailChannelSendsEmailPayload() {
        NotificationChannel channel = channel(5L, "Email", true);
        channel.setType("EMAIL_SMTP");
        channel.setEndpointEnvKey("OPS_ALERT_EMAIL_TO");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_ALERT_EMAIL_TO"))
                .thenReturn("ops@example.test,admin@example.test");
        when(emailSender.send(any(), any()))
                .thenReturn(new NotificationEmailSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isTrue();
        ArgumentCaptor<List<String>> recipientsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NotificationEmailPayload> payloadCaptor =
                ArgumentCaptor.forClass(NotificationEmailPayload.class);
        verify(emailSender).send(recipientsCaptor.capture(), payloadCaptor.capture());
        assertThat(recipientsCaptor.getValue())
                .containsExactly("ops@example.test", "admin@example.test");
        assertThat(payloadCaptor.getValue().eventType()).isEqualTo("TEST_EMAIL_SMTP");
        verify(webhookSender, never()).send(any(), any());
    }

    @Test
    void emailChannelFailsWhenRecipientEnvIsMissing() {
        NotificationChannel channel = channel(5L, "Email", true);
        channel.setType("EMAIL_SMTP");
        channel.setEndpointEnvKey("OPS_ALERT_EMAIL_TO");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_ALERT_EMAIL_TO")).thenReturn(null);
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isFalse();
        assertThat(response.message()).isEqualTo("EMAIL_RECIPIENT_ENV_MISSING");
        verify(emailSender, never()).send(any(), any());
        verify(webhookSender, never()).send(any(), any());
    }

    @Test
    void emailSenderFailureUsesSanitizedError() {
        NotificationChannel channel = channel(5L, "Email", true);
        channel.setType("EMAIL_SMTP");
        channel.setEndpointEnvKey("OPS_ALERT_EMAIL_TO");
        when(opsMapper.findNotificationChannelById(5L)).thenReturn(channel);
        when(environment.getProperty("OPS_ALERT_EMAIL_TO")).thenReturn("ops@example.test");
        when(emailSender.send(any(), any()))
                .thenReturn(new NotificationEmailSender.DeliveryResult(false, "EMAIL_SEND_FAILED"));
        wireEventPersistence();

        var response = service.testChannel(5L);

        assertThat(response.sent()).isFalse();
        assertThat(response.status()).isEqualTo("RETRY");
        assertThat(response.message()).isEqualTo("EMAIL_SEND_FAILED");
    }

    @Test
    void crawlFailureCreatesSingleEventAndSends() {
        NotificationChannel channel = channel(2L, "Ops", true);
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setCrawlRunLogNo(11L);
        when(opsMapper.findOpenNotificationEvent("CRAWL_RUN_FAILED", "CRAWL_RUN_LOG", "11"))
                .thenReturn(null);
        when(opsMapper.findNotificationChannels(true)).thenReturn(List.of(channel));
        when(environment.getProperty("OPS_WEBHOOK_URL"))
                .thenReturn("https://hooks.example.test/ops");
        when(webhookSender.send(any(), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));
        wireEventPersistence();

        service.notifyCrawlFailure(runLog);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(opsMapper).insertNotificationEvent(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CRAWL_RUN_FAILED");
        assertThat(captor.getValue().getSourceId()).isEqualTo("11");
        verify(opsMapper).updateNotificationDelivery(100L, "SENT", 1, null, null);
    }

    @Test
    void crawlFailureReusesOpenEvent() {
        NotificationEvent existing = event(100L, "FAILED", 1);
        when(opsMapper.findOpenNotificationEvent("CRAWL_RUN_FAILED", "CRAWL_RUN_LOG", "11"))
                .thenReturn(existing);
        when(opsMapper.findNotificationEventById(100L)).thenReturn(existing);
        when(opsMapper.findNotificationChannels(true)).thenReturn(List.of());
        CrawlRunLog runLog = new CrawlRunLog();
        runLog.setCrawlRunLogNo(11L);

        service.notifyCrawlFailure(runLog);

        verify(opsMapper, never()).insertNotificationEvent(any(NotificationEvent.class));
        verify(opsMapper).updateNotificationDelivery(100L, "FAILED", 2, "NO_ENABLED_CHANNEL", null);
    }

    @Test
    void retryDueEventsDeliversOnlyMapperSelectedEvents() {
        NotificationEvent retry = event(100L, "RETRY", 1);
        retry.setNextRetryAt(OffsetDateTime.parse("2026-05-28T09:55:00+09:00"));
        NotificationChannel channel = channel(2L, "Ops", true);
        when(opsMapper.findDueNotificationEvents(
                        OffsetDateTime.parse("2026-05-28T10:00:00+09:00"), 3, 20))
                .thenReturn(List.of(retry));
        when(opsMapper.findNotificationChannels(true)).thenReturn(List.of(channel));
        when(opsMapper.findNotificationEventById(100L)).thenReturn(retry);
        when(environment.getProperty("OPS_WEBHOOK_URL"))
                .thenReturn("https://hooks.example.test/ops");
        when(webhookSender.send(any(), any()))
                .thenReturn(new NotificationWebhookSender.DeliveryResult(true, "SENT"));

        int processed = service.retryDueEvents();

        assertThat(processed).isEqualTo(1);
        verify(opsMapper).updateNotificationDelivery(100L, "SENT", 2, null, null);
    }

    private void wireEventPersistence() {
        doAnswer(
                        invocation -> {
                            NotificationEvent event = invocation.getArgument(0);
                            event.setNotificationEventNo(100L);
                            event.setFrstRegDt(OffsetDateTime.parse("2026-05-28T10:00:00+09:00"));
                            when(opsMapper.findNotificationEventById(100L)).thenReturn(event);
                            return null;
                        })
                .when(opsMapper)
                .insertNotificationEvent(any(NotificationEvent.class));
        doAnswer(
                        invocation -> {
                            NotificationEvent event =
                                    event(
                                            100L,
                                            invocation.getArgument(1),
                                            invocation.getArgument(2));
                            event.setLastError(invocation.getArgument(3));
                            when(opsMapper.findNotificationEventById(100L)).thenReturn(event);
                            return 1;
                        })
                .when(opsMapper)
                .updateNotificationDelivery(
                        org.mockito.ArgumentMatchers.eq(100L),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.nullable(String.class),
                        org.mockito.ArgumentMatchers.any());
    }

    private NotificationChannel channel(Long id, String name, boolean enabled) {
        NotificationChannel channel = new NotificationChannel();
        channel.setNotificationChannelNo(id);
        channel.setName(name);
        channel.setType("WEBHOOK");
        channel.setUseYn(enabled ? "Y" : "N");
        channel.setEndpointEnvKey("OPS_WEBHOOK_URL");
        channel.setRateLimitPerHour(60);
        return channel;
    }

    private NotificationEvent event(Long id, String status, int attempts) {
        NotificationEvent event = new NotificationEvent();
        event.setNotificationEventNo(id);
        event.setEventType("CRAWL_RUN_FAILED");
        event.setSourceType("CRAWL_RUN_LOG");
        event.setSourceId("11");
        event.setSeverity("ERROR");
        event.setSummary("Crawl run failed: #11");
        event.setStatus(status);
        event.setAttemptCount(attempts);
        event.setFrstRegDt(OffsetDateTime.parse("2026-05-28T10:00:00+09:00"));
        return event;
    }
}
