package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LogCollectionReadinessTest {

    @Test
    void logbackKeepsExternalCollectionChannels() throws IOException {
        String logback = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        assertThat(logback)
                .contains(
                        "${LOG_DIR}/app.log",
                        "${LOG_DIR}/api.log",
                        "${LOG_DIR}/security.log",
                        "${LOG_DIR}/error.log",
                        "${LOG_DIR}/slow-query.log",
                        "${LOG_DIR:-/var/log/cleverchat}");
        assertThat(logback)
                .contains(
                        "kr.co.cleverchat.ops.api",
                        "kr.co.cleverchat.ops.security",
                        "kr.co.cleverchat.ops.slowquery");
    }

    @Test
    void filebeatSampleCollectsAllChannelsWithoutSecretsOrRawPayloadGuidance() throws IOException {
        String sample = Files.readString(Path.of("deploy/filebeat-cleverchat.yml"));
        String normalized = sample.toLowerCase();

        assertThat(sample)
                .contains(
                        "/var/log/cleverchat/app.log",
                        "/var/log/cleverchat/api.log",
                        "/var/log/cleverchat/security.log",
                        "/var/log/cleverchat/error.log",
                        "/var/log/cleverchat/slow-query.log",
                        "log_channel: app",
                        "log_channel: api",
                        "log_channel: security",
                        "log_channel: error",
                        "log_channel: slow-query",
                        "ndjson:",
                        "output.console:");
        assertThat(normalized)
                .doesNotContain(
                        "request.body",
                        "response.body",
                        "query_string",
                        "raw_ip:",
                        "raw_user_agent:",
                        "authorization:",
                        "api_key:",
                        "password:");
    }

    @Test
    void manifestDocumentsJsonChannelsAndSensitiveDataBoundary() throws IOException {
        String manifest =
                Files.readString(Path.of("../../2.설계/06.운영메모/M6_log_collection_manifest.md"));
        String normalized = manifest.toLowerCase();

        assertThat(manifest)
                .contains(
                        "app.log",
                        "api.log",
                        "security.log",
                        "error.log",
                        "slow-query.log",
                        "requestId",
                        "remoteAddrHash",
                        "userAgentHash",
                        "field-encryption key material");
        assertThat(normalized)
                .contains(
                        "request bodies",
                        "response bodies",
                        "query strings",
                        "raw ip",
                        "raw user-agent");
    }
}
