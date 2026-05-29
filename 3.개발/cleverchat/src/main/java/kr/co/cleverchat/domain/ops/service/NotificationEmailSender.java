package kr.co.cleverchat.domain.ops.service;

import java.util.List;

public interface NotificationEmailSender {

    DeliveryResult send(List<String> recipients, NotificationEmailPayload payload);

    record DeliveryResult(boolean success, String message) {}
}
