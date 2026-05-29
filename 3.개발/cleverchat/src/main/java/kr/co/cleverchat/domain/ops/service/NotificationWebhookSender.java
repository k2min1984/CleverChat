package kr.co.cleverchat.domain.ops.service;

public interface NotificationWebhookSender {

    DeliveryResult send(String endpointUrl, Object payload);

    record DeliveryResult(boolean success, String message) {}
}
