package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StructuredLogWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuredLogWriter writer = new StructuredLogWriter(objectMapper);

    @Test
    void toJsonCreatesValidStructuredJson() throws Exception {
        String json =
                writer.toJson(
                        "api_access",
                        Map.of(
                                "requestId", "req-1",
                                "path", "/admin/api/scenarios",
                                "status", 200));

        var node = objectMapper.readTree(json);

        assertThat(node.get("event").asText()).isEqualTo("api_access");
        assertThat(node.get("requestId").asText()).isEqualTo("req-1");
        assertThat(node.get("path").asText()).isEqualTo("/admin/api/scenarios");
        assertThat(node.get("status").asInt()).isEqualTo(200);
        assertThat(node.has("timestamp")).isTrue();
    }

    @Test
    void hashDoesNotExposeOriginalValue() {
        String hash = OpsLogHasher.sha256("127.0.0.1");

        assertThat(hash).hasSize(64);
        assertThat(hash).doesNotContain("127.0.0.1");
    }

    @Test
    void slowQueryJsonDoesNotNeedSqlOrParameterFields() throws Exception {
        String json =
                writer.toJson(
                        "slow_query",
                        Map.of(
                                "requestId",
                                "req-1",
                                "statementId",
                                "Mapper.select",
                                "durationMs",
                                1200,
                                "success",
                                true));

        var node = objectMapper.readTree(json);

        assertThat(node.get("event").asText()).isEqualTo("slow_query");
        assertThat(node.has("sql")).isFalse();
        assertThat(node.has("parameter")).isFalse();
        assertThat(json).doesNotContain("password", "resident", "card");
    }
}
