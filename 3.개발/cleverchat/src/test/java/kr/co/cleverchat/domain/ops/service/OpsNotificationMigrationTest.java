package kr.co.cleverchat.domain.ops.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpsNotificationMigrationTest {

    private static final Path V15_MIGRATION =
            Path.of("src/main/resources/db/migration/V15__notification_adapter.sql");

    @Test
    void notificationAdapterMigrationAddsSlackTypeAndPreviousEnvKey() throws IOException {
        String sql = Files.readString(V15_MIGRATION, StandardCharsets.UTF_8);

        assertThat(sql).contains("previous_endpoint_env_key");
        assertThat(sql).contains("SLACK_WEBHOOK");
        assertThat(sql).contains("DROP CONSTRAINT IF EXISTS ck_notification_channel_type");
        assertThat(sql).contains("CHECK (type IN ('WEBHOOK', 'SLACK_WEBHOOK'))");
    }

    @Test
    void notificationEmailMigrationAddsEmailSmtpType() throws IOException {
        String sql =
                Files.readString(
                        Path.of(
                                "src/main/resources/db/migration/V16__notification_email_adapter.sql"),
                        StandardCharsets.UTF_8);

        assertThat(sql).contains("EMAIL_SMTP");
        assertThat(sql).contains("DROP CONSTRAINT IF EXISTS ck_notification_channel_type");
        assertThat(sql).contains("CHECK (type IN ('WEBHOOK', 'SLACK_WEBHOOK', 'EMAIL_SMTP'))");
    }
}
