package kr.co.cleverchat.domain.ops.service;

import java.time.OffsetDateTime;

public record NotificationEmailPayload(
        Long eventId,
        String eventType,
        String severity,
        String summary,
        String adminUrl,
        OffsetDateTime createdAt) {}
