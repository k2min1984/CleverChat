package kr.co.cleverchat.domain.ops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpNotificationEmailSenderTest {

    private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    private final Environment environment = org.mockito.Mockito.mock(Environment.class);
    private final SmtpNotificationEmailSender sender =
            new SmtpNotificationEmailSender(mailSender, environment, "no-reply@example.test");

    @Test
    void missingSmtpHostFailsWithoutSending() {
        when(environment.getProperty("spring.mail.host")).thenReturn(null);

        var result = sender.send(List.of("ops@example.test"), payload());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("SMTP_CONFIG_MISSING");
        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void sendsSanitizedEventEmail() {
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.example.test");

        var result = sender.send(List.of("ops@example.test"), payload());

        assertThat(result.success()).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ops@example.test");
        assertThat(message.getFrom()).isEqualTo("no-reply@example.test");
        assertThat(message.getSubject()).isEqualTo("[CleverChat][ERROR] CRAWL_RUN_FAILED");
        assertThat(message.getText()).contains("Event ID: 100");
        assertThat(message.getText()).doesNotContain("smtp.example.test");
    }

    @Test
    void sendFailureReturnsSanitizedError() {
        when(environment.getProperty("spring.mail.host")).thenReturn("smtp.example.test");
        org.mockito.Mockito.doThrow(new IllegalStateException("secret failure detail"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

        var result = sender.send(List.of("ops@example.test"), payload());

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("EMAIL_SEND_FAILED");
        assertThat(result.message()).doesNotContain("secret failure detail");
    }

    private NotificationEmailPayload payload() {
        return new NotificationEmailPayload(
                100L,
                "CRAWL_RUN_FAILED",
                "ERROR",
                "Crawl run failed: #11",
                "/admin/notifications",
                OffsetDateTime.parse("2026-05-28T10:00:00+09:00"));
    }
}
