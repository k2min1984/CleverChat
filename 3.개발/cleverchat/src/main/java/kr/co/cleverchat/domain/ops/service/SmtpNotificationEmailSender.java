package kr.co.cleverchat.domain.ops.service;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    private final JavaMailSender mailSender;
    private final Environment environment;
    private final String from;

    @Autowired
    public SmtpNotificationEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Environment environment,
            @Value("${cleverchat.notification.email.from:no-reply@localhost}") String from) {
        this(mailSenderProvider.getIfAvailable(), environment, from);
    }

    SmtpNotificationEmailSender(JavaMailSender mailSender, Environment environment, String from) {
        this.mailSender = mailSender;
        this.environment = environment;
        this.from = from;
    }

    @Override
    public DeliveryResult send(List<String> recipients, NotificationEmailPayload payload) {
        if (mailSender == null || blank(environment.getProperty("spring.mail.host"))) {
            return new DeliveryResult(false, "SMTP_CONFIG_MISSING");
        }
        if (recipients == null || recipients.isEmpty()) {
            return new DeliveryResult(false, "EMAIL_RECIPIENT_MISSING");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject("[CleverChat][" + payload.severity() + "] " + payload.eventType());
            message.setText(body(payload));
            mailSender.send(message);
            return new DeliveryResult(true, "SENT");
        } catch (Exception e) {
            return new DeliveryResult(false, "EMAIL_SEND_FAILED");
        }
    }

    private String body(NotificationEmailPayload payload) {
        return String.join(
                System.lineSeparator(),
                "CleverChat notification",
                "Event ID: " + payload.eventId(),
                "Event type: " + payload.eventType(),
                "Severity: " + payload.severity(),
                "Summary: " + payload.summary(),
                "Admin URL: " + payload.adminUrl(),
                "Created at: " + payload.createdAt());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
