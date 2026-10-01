package kr.co.cleverchat.domain.settings;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import kr.co.cleverchat.domain.chatbot.config.ChatSearchProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.servlet.context-path=/cleverchat", "cleverchat.crawl.worker.enabled=false",
            "cleverchat.crawl.worker.poll-delay-ms=1000", "cleverchat.crawl.browser.enabled=false",
            "cleverchat.gateway.shared-secret=http-fixture-secret"
        })
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@Tag("integration")
class RuntimeSettingsHttpIntegrationTest {
    @Container static PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", database::getJdbcUrl);
        p.add("spring.datasource.username", database::getUsername);
        p.add("spring.datasource.password", database::getPassword);
    }

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ChatSearchProperties search;
    @TempDir Path folder;
    private HttpClient client;

    @Test
    void actualHttpSessionSettingsCrawlAiAndSsoWorkUnderServicePath() throws Exception {
        client =
                HttpClient.newBuilder()
                        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
        Long user =
                jdbc.queryForObject(
                        "INSERT INTO tb_user(username,password_hash,display_name,use_yn,must_change_password) VALUES(?,?,?,'Y',false) RETURNING user_no",
                        Long.class,
                        "runtime-http",
                        passwords.encode("fixture-password"),
                        "HTTP QA");
        jdbc.update(
                "INSERT INTO tb_user_role(user_no,role_no) SELECT ?,role_no FROM tb_role WHERE code='ADMIN'",
                user);
        check(
                send(
                        "/login",
                        Map.of("username", "runtime-http", "password", "fixture-password"),
                        Map.of()),
                302);
        String page = check(send("/admin/system-settings", null, Map.of()), 200).body();
        assertThat(page)
                .contains(
                        "/cleverchat/asset/common/app-url.js",
                        "settings-tab-storage",
                        "settings-tab-ai");
        var storage = form(page, "/admin/system-settings", "storage");
        storage.put("crawlExportDirectory", folder.toString());
        check(send("/admin/system-settings", storage, Map.of()), 302);
        page = check(send("/admin/system-settings", null, Map.of()), 200).body();
        var searchForm = form(page, "/admin/system-settings/runtime/search", null);
        searchForm.put("SEARCH_DISPLAY", "2");
        check(send("/admin/system-settings/runtime/search", searchForm, Map.of()), 302);
        assertThat(search.getDisplayMax()).isEqualTo(2);

        HttpServer fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger generations = new AtomicInteger();
        fixture.createContext(
                "/",
                exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    String body;
                    String type;
                    if (path.equals("/robots.txt")) {
                        body = "User-agent: *\nAllow: /\n";
                        type = "text/plain";
                    } else if (path.equals("/v1/models")) {
                        body = "{\"data\":[{\"id\":\"fixture-model\"}]}";
                        type = "application/json";
                    } else if (path.equals("/v1/chat/completions")) {
                        generations.incrementAndGet();
                        body =
                                "{\"choices\":[{\"message\":{\"content\":\"QA AI: 월요일에 접수합니다. [1]\"}}]}";
                        type = "application/json";
                    } else {
                        body =
                                "<html><head><title>런타임설정검증</title></head><body><main><h1>런타임설정검증</h1><p>런타임설정검증 자료입니다. 접수는 월요일에 진행합니다.</p></main></body></html>";
                        type = "text/html";
                    }
                    exchange.getRequestBody().readAllBytes();
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                });
        fixture.start();
        try {
            String fixtureBase = "http://127.0.0.1:" + fixture.getAddress().getPort();
            page = check(send("/admin/system-settings", null, Map.of()), 200).body();
            var ai = form(page, "/admin/system-settings/runtime/ai", null);
            ai.put("AI_ENABLED", "true");
            ai.put("AI_BASE_URL", fixtureBase + "/v1");
            ai.put("AI_MODEL", "fixture-model");
            check(send("/admin/system-settings/runtime/ai", ai, Map.of()), 302);
            page = check(send("/admin/system-settings", null, Map.of()), 200).body();
            check(
                    send(
                            "/admin/system-settings/ai/test",
                            form(page, "/admin/system-settings/ai/test", null),
                            Map.of()),
                    302);
            String confirmation = check(send("/admin/system-settings", null, Map.of()), 200).body();
            assertThat(confirmation).contains("모델 목록에서 확인");

            var crawl = form(confirmation, "/admin/system-settings/runtime/crawl", null);
            crawl.put("CRAWL_ENABLED", "true");
            check(send("/admin/system-settings/runtime/crawl", crawl, Map.of()), 302);
            page = check(send("/admin/system-settings", null, Map.of()), 200).body();
            var create = csrf(page);
            create.putAll(
                    Map.of(
                            "url",
                            fixtureBase + "/article",
                            "label",
                            "런타임설정검증",
                            "useYn",
                            "Y",
                            "jsonExportEnabled",
                            "true",
                            "jsonExportDirectory",
                            ""));
            var created = check(send("/admin/crawl-targets", create, Map.of()), 302);
            String detailPath =
                    URI.create(created.headers().firstValue("Location").orElseThrow())
                            .getPath()
                            .replaceFirst("^/cleverchat", "");
            String detail = check(send(detailPath, null, Map.of()), 200).body();
            check(send(detailPath + "/run", csrf(detail), Map.of()), 302);
            org.awaitility.Awaitility.await()
                    .atMost(Duration.ofSeconds(25))
                    .untilAsserted(
                            () ->
                                    assertThat(
                                                    jdbc.queryForObject(
                                                            "SELECT status FROM tb_crawl_job ORDER BY crawl_job_no DESC LIMIT 1",
                                                            String.class))
                                            .isEqualTo("SUCCESS"));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM tb_crawl_export_file", Integer.class))
                    .isEqualTo(1);
            try (var files = Files.walk(folder)) {
                List<Path> snapshots = files.filter(p -> p.toString().endsWith(".json")).toList();
                assertThat(snapshots).hasSize(1);
                assertThat(Files.readString(snapshots.get(0))).contains("런타임설정검증");
            }
            HttpRequest chat =
                    HttpRequest.newBuilder(URI.create(base() + "/chat/api/sessions/auto"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"런타임설정검증\"}"))
                            .build();
            String answer =
                    check(
                                    client.send(
                                            chat,
                                            HttpResponse.BodyHandlers.ofString(
                                                    StandardCharsets.UTF_8)),
                                    200)
                            .body();
            assertThat(answer).contains("QA AI:");
            assertThat(generations.get()).isPositive();
        } finally {
            fixture.stop(0);
        }

        page = check(send("/admin/system-settings", null, Map.of()), 200).body();
        var linked = form(page, "/admin/system-settings", "connection");
        linked.put("operationMode", "GATEWAY");
        linked.put("gatewayUrl", "http://127.0.0.1:9999");
        check(send("/admin/system-settings", linked, Map.of()), 302);
        assertThat(check(send("/login", null, Map.of()), 200).body())
                .contains("127.0.0.1:9999")
                .doesNotContain("name=\"password\"");
        check(
                send(
                        "/login",
                        Map.of("username", "runtime-http", "password", "fixture-password"),
                        Map.of()),
                401);
        Map<String, String> headers =
                Map.of(
                        "X-Clever-Gw-Secret",
                        "http-fixture-secret",
                        "X-Clever-User",
                        "HTTP-SSO-ADMIN",
                        "X-Clever-Roles",
                        "cleverchat=ADMIN");
        page = check(send("/admin/system-settings", null, headers), 200).body();
        assertThat(page).contains("SSO 인증");
        check(
                send(
                        "/admin/system-settings",
                        null,
                        Map.of(
                                "X-Clever-Gw-Secret",
                                "invalid",
                                "X-Clever-User",
                                "HTTP-SSO-ADMIN",
                                "X-Clever-Roles",
                                "cleverchat=ADMIN")),
                401);
        page = check(send("/admin/system-settings", null, headers), 200).body();
        var independent = form(page, "/admin/system-settings", "connection");
        independent.put("operationMode", "LOCAL");
        check(send("/admin/system-settings", independent, headers), 302);
        check(
                send(
                        "/login",
                        Map.of("username", "runtime-http", "password", "fixture-password"),
                        Map.of()),
                302);
        check(send("/admin/system-settings", null, Map.of()), 200);
    }

    private String base() {
        return "http://127.0.0.1:" + port + "/cleverchat";
    }

    private HttpResponse<String> send(
            String path, Map<String, String> form, Map<String, String> headers) throws Exception {
        var request =
                HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(30));
        headers.forEach(request::header);
        if (form == null) request.GET();
        else {
            String body =
                    form.entrySet().stream()
                            .map(
                                    e ->
                                            URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                                    + "="
                                                    + URLEncoder.encode(
                                                            e.getValue(), StandardCharsets.UTF_8))
                            .collect(java.util.stream.Collectors.joining("&"));
            request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(
                request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> check(HttpResponse<String> response, int code) {
        assertThat(response.statusCode()).as("HTTP status for %s", response.uri()).isEqualTo(code);
        return response;
    }

    private Map<String, String> csrf(String html) {
        var document = Jsoup.parse(html);
        var result = new LinkedHashMap<String, String>();
        for (String key : List.of("csrfToken", "csrfFormId"))
            result.put(key, document.selectFirst("input[name=" + key + "]").val());
        return result;
    }

    private Map<String, String> form(String html, String path, String tab) {
        for (Element form : Jsoup.parse(html).select("form")) {
            if (!form.attr("action").equals("/cleverchat" + path)) continue;
            if (tab != null
                    && (form.selectFirst("input[name=tab]") == null
                            || !form.selectFirst("input[name=tab]").val().equals(tab))) continue;
            var values = new LinkedHashMap<String, String>();
            for (Element input : form.select("input[name]")) {
                if (input.attr("type").equals("radio") && !input.hasAttr("checked")) continue;
                values.put(input.attr("name"), input.val());
            }
            for (Element select : form.select("select[name]"))
                values.put(select.attr("name"), select.selectFirst("option[selected]").val());
            return values;
        }
        throw new AssertionError("Missing form: " + path + "/" + tab);
    }
}
