package kr.co.cleverchat.domain.ops.service;

import java.time.OffsetDateTime;

public record NotificationWebhookPayload(
        Long eventId,
        String eventType,
        String severity,
        String summary,
        String adminUrl,
        OffsetDateTime createdAt) {}
