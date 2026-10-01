package kr.co.cleverchat.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConfigurationSafetyTest {

    @Test
    void defaultConfigurationDoesNotSelectRemoteDevelopmentProfile() throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml).doesNotContain("SPRING_PROFILES_ACTIVE:dev", "dev.c2r.co.kr");
    }

    @Test
    void developmentDatabaseRequiresExplicitCredentialsAndDoesNotSeedAdminByDefault()
            throws Exception {
        String yaml =
                Files.readString(
                        Path.of("src/main/resources/application-dev.yml"), StandardCharsets.UTF_8);

        assertThat(yaml)
                .contains(
                        "url: ${DB_URL}",
                        "username: ${DB_USER}",
                        "password: ${DB_PASSWORD}",
                        "default-admin-seed-enabled: ${DEFAULT_ADMIN_SEED_ENABLED:false}")
                .doesNotContain("dev.c2r.co.kr", "password: ${DB_PASSWORD:cleverchat}");
    }
}
