package kr.co.cleverchat.domain.ops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class HttpNotificationWebhookSender implements NotificationWebhookSender {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpNotificationWebhookSender(ObjectMapper objectMapper) {
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public DeliveryResult send(String endpointUrl, Object payload) {
        try {
            URI uri = URI.create(endpointUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme())) {
                return new DeliveryResult(false, "INVALID_ENDPOINT");
            }
            HttpRequest request =
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(5))
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(payload)))
                            .build();
            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new DeliveryResult(true, "SENT");
            }
            return new DeliveryResult(false, "HTTP_" + response.statusCode());
        } catch (IllegalArgumentException e) {
            return new DeliveryResult(false, "INVALID_ENDPOINT");
        } catch (Exception e) {
            return new DeliveryResult(false, "SEND_FAILED");
        }
    }
}
