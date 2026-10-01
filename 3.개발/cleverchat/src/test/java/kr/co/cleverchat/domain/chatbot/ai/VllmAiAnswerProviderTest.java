package kr.co.cleverchat.domain.chatbot.ai;

import static kr.co.cleverchat.domain.settings.RuntimeSetting.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import kr.co.cleverchat.domain.settings.*;
import org.junit.jupiter.api.*;

class VllmAiAnswerProviderTest {
    private HttpServer server;
    private final ObjectMapper json = new ObjectMapper();
    private final RuntimeSettingsService settings = mock(RuntimeSettingsService.class);
    private final Map<String, Object> values = new HashMap<>();
    private VllmAiAnswerProvider provider;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        for (var field : RuntimeSetting.values()) values.put(field.name(), field.defaultValue);
        values.put(AI_ENABLED.name(), true);
        values.put(AI_BASE_URL.name(), "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        values.put(AI_MODEL.name(), "fixture-model");
        when(settings.current())
                .thenAnswer(call -> new RuntimeSettingsService.Snapshot(Map.copyOf(values)));
        provider = new VllmAiAnswerProvider(settings, json, "fixture-key");
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private void endpoint(String path, int status, String body) {
        server.createContext(
                path,
                exchange -> {
                    requestBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                });
    }

    private AiAnswerPrompt prompt() {
        return new AiAnswerPrompt(
                "사용 방법은?",
                null,
                List.of(
                        new AiAnswerCitation(
                                "CRAWL_DOCUMENT",
                                null,
                                "이용 안내",
                                1L,
                                "https://example.com",
                                "CRAWL_DOCUMENT",
                                "월요일에 신청합니다.")));
    }

    @Test
    void sendsGroundedChatRequestAndReturnsAnswerWithCitations() throws Exception {
        endpoint(
                "/v1/chat/completions",
                200,
                "{\"choices\":[{\"message\":{\"content\":\"월요일에 신청하세요. [1]\"}}]}");
        var response = provider.generate(prompt());
        assertThat(response.generated()).isTrue();
        assertThat(response.answer()).contains("월요일");
        assertThat(response.citations()).hasSize(1);
        assertThat(authorization.get()).isEqualTo("Bearer fixture-key");
        var sent = json.readTree(requestBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("fixture-model");
        assertThat(sent.path("stream").asBoolean()).isFalse();
        assertThat(sent.path("messages").path(1).path("content").asText())
                .contains("월요일에 신청합니다.", "사용 방법은?");
    }

    @Test
    void connectionCheckConfirmsConfiguredModel() {
        endpoint("/v1/models", 200, "{\"data\":[{\"id\":\"fixture-model\"}]}");
        assertThat(provider.checkConnection()).contains("모델");
        values.put(AI_MODEL.name(), "missing");
        assertThatThrownBy(provider::checkConnection).hasMessageContaining("찾지 못했습니다");
    }

    @Test
    void remoteFailureFallsBackToExistingSearchWithoutLeakingResponseBody() {
        endpoint("/v1/chat/completions", 503, "sensitive remote diagnostic");
        assertThatThrownBy(() -> provider.generate(prompt()))
                .hasMessageContaining("503")
                .hasMessageNotContaining("sensitive");
        var service = new AiAnswerSuggestionService(true, provider);
        var result = new kr.co.cleverchat.domain.search.model.SearchResultItem();
        result.setMatchedField("CRAWL_DOCUMENT");
        result.setSnippet("근거");
        assertThat(service.suggest("질문", List.of(result), null)).isEmpty();
    }

    @Test
    void disabledAiDoesNotCallServer() {
        values.put(AI_ENABLED.name(), false);
        assertThat(provider.generate(prompt()).generated()).isFalse();
        assertThat(requestBody.get()).isNull();
    }
}
