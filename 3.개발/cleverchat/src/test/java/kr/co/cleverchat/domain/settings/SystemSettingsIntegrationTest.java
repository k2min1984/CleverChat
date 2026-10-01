package kr.co.cleverchat.domain.settings;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.*;
import java.time.*;
import java.util.Set;
import kr.co.cleverchat.domain.auth.security.*;
import kr.co.cleverchat.domain.crawl.model.*;
import kr.co.cleverchat.domain.crawl.service.CrawlJsonExporter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
        properties = {
            "cleverchat.crawl.worker.enabled=false",
            "cleverchat.crawl.browser.enabled=false",
            "cleverchat.gateway.shared-secret=fixture-secret"
        })
@ActiveProfiles("dev")
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
@Tag("integration")
class SystemSettingsIntegrationTest {
    @Container static PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired SystemAuthenticationFilter authentication;
    @Autowired JdbcTemplate jdbc;
    @Autowired SystemSettingsService settings;
    @Autowired RuntimeSettingsService runtime;
    @Autowired kr.co.cleverchat.domain.chatbot.config.ChatSearchProperties searchProperties;
    @Autowired CsrfTokenIssuer csrf;
    @Autowired CrawlJsonExporter exporter;
    @TempDir Path folder;

    @BeforeEach
    void standalone() {
        mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                                context)
                        .addFilters(authentication)
                        .build();
        jdbc.update(
                "UPDATE tb_system_settings SET operation_mode='LOCAL',gateway_url=NULL,service_id='cleverchat',crawl_export_directory=NULL,version=version+1,auth_version=auth_version+1");
        jdbc.update("UPDATE tb_system_settings SET runtime_settings='{}'::jsonb");
        runtime.invalidate();
    }

    private MockHttpSession admin(String role) {
        var session = new MockHttpSession();
        session.setAttribute(
                AdminSession.SESSION_KEY,
                new AdminSession(1L, "fixture", "관리자", Set.of(role), false, LocalDateTime.now()));
        csrf.issue(session);
        return session;
    }

    private MockHttpServletRequestBuilder sso(
            MockHttpServletRequestBuilder request, String emp, String role) {
        return request.header("X-Clever-Gw-Secret", "fixture-secret")
                .header("X-Clever-User", emp)
                .header("X-Clever-Name", "Gateway%20User")
                .header("X-Clever-Roles", "cleverchat=" + role);
    }

    private void linked() {
        jdbc.update(
                "UPDATE tb_system_settings SET operation_mode='GATEWAY',gateway_url='https://portal.example.com',auth_version=auth_version+1");
    }

    private MockHttpServletRequestBuilder runtimeRequest(
            String section, MockHttpSession session, java.util.Map<String, String> overrides) {
        var request =
                post("/admin/system-settings/runtime/" + section)
                        .session(session)
                        .param("version", String.valueOf(settings.current().version()))
                        .param(
                                "csrfToken",
                                (String)
                                        session.getAttribute(
                                                CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE))
                        .param(
                                "csrfFormId",
                                (String)
                                        session.getAttribute(
                                                CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE));
        for (var field : runtime.fields(section))
            request.param(
                    field.name(),
                    overrides.getOrDefault(
                            field.name(), runtime.current().values().get(field.name()).toString()));
        return request;
    }

    @Test
    void runtimeSaveRequiresAdminAndChangesActualSearchConsumer() throws Exception {
        mvc.perform(
                        runtimeRequest(
                                "search",
                                admin("OPERATOR"),
                                java.util.Map.of("SEARCH_DISPLAY", "2")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        runtimeRequest(
                                "search", admin("ADMIN"), java.util.Map.of("SEARCH_DISPLAY", "2")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/system-settings#search"));
        assertThat(searchProperties.getDisplayMax()).isEqualTo(2);
        assertThat(settings.current().operationMode()).isEqualTo("LOCAL");
        assertThat(runtime.current().integer(RuntimeSetting.LOGIN_FAILURES)).isEqualTo(5);
    }

    @Test
    void invalidRuntimeSettingsLeaveSavedConfigurationUntouched() throws Exception {
        mvc.perform(
                        runtimeRequest(
                                "search",
                                admin("ADMIN"),
                                java.util.Map.of("SEARCH_POOL", "1", "SEARCH_DISPLAY", "9")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
        assertThat(searchProperties.getDisplayMax()).isEqualTo(5);
        mvc.perform(runtimeRequest("ai", admin("ADMIN"), java.util.Map.of("AI_ENABLED", "true")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
        assertThat(runtime.current().bool(RuntimeSetting.AI_ENABLED)).isFalse();
    }

    @Test
    void sessionTimeoutChangesOnNextRequest() throws Exception {
        var session = admin("ADMIN");
        mvc.perform(runtimeRequest("login", session, java.util.Map.of("SESSION_MINUTES", "45")))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/system-settings").session(session)).andExpect(status().isOk());
        assertThat(session.getMaxInactiveInterval()).isEqualTo(45 * 60);
    }

    @Test
    void linkedScreenRendersUnderServicePrefix() throws Exception {
        linked();
        String html =
                mvc.perform(
                                sso(
                                        get("/cleverchat/admin/system-settings")
                                                .contextPath("/cleverchat")
                                                .servletPath("/admin/system-settings"),
                                        "PREFIX-ADMIN",
                                        "ADMIN"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(html)
                .contains(
                        "/cleverchat/asset/common/app-url.js",
                        "/cleverchat/admin/system-settings/runtime/crawl",
                        "SSO 인증");
    }

    @Test
    void settingsPageRequiresAdministratorAndRenders() throws Exception {
        mvc.perform(get("/admin/system-settings").session(admin("USER")))
                .andExpect(status().isForbidden());
        String html =
                mvc.perform(get("/admin/system-settings").session(admin("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(html).contains("독립(단독)", "크롤링 JSON 기본 저장 폴더").doesNotContain("fixture-secret");
    }

    @Test
    void csrfProtectedSavePersistsDefaultAndExporterUsesIt() throws Exception {
        var session = admin("ADMIN");
        mvc.perform(
                        post("/admin/system-settings")
                                .session(session)
                                .param("operationMode", "LOCAL")
                                .param("serviceId", "cleverchat")
                                .param("version", String.valueOf(settings.current().version()))
                                .param("crawlExportDirectory", folder.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/admin/system-settings")
                                .session(session)
                                .param("operationMode", "LOCAL")
                                .param("serviceId", "cleverchat")
                                .param("version", String.valueOf(settings.current().version()))
                                .param("crawlExportDirectory", folder.toString())
                                .param("csrfToken", (String) session.getAttribute("csrfToken"))
                                .param("csrfFormId", (String) session.getAttribute("csrfFormId")))
                .andExpect(status().is3xxRedirection());
        assertThat(settings.current().crawlExportDirectory()).isEqualTo(folder.toString());
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(99L);
        target.setJsonExportEnabled(true);
        CrawlDocument document = new CrawlDocument();
        document.setFetchedAt(OffsetDateTime.now());
        document.setTitle("기본 경로 확인");
        var result = exporter.export(target, document);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(Path.of(result.path())).exists().startsWith(folder);
        Path override = folder.resolve("override");
        target.setJsonExportDirectory(override.toString());
        assertThat(Path.of(exporter.export(target, document).path())).startsWith(override);
    }

    @Test
    void missingAndForgedSsoAreRejectedAndPasswordLoginIsNotUsed() throws Exception {
        linked();
        mvc.perform(get("/chat/api/scenarios")).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/chat/api/scenarios")
                                .header("X-Clever-Gw-Secret", "wrong")
                                .header("X-Clever-User", "E1")
                                .header("X-Clever-Roles", "cleverchat=ADMIN"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/login").param("username", "admin").param("password", "anything"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/login")).andExpect(forwardedUrl("/gateway-login"));
    }

    @Test
    void ssoCreatesPasswordlessUserAndRefreshesRolesOnEveryRequest() throws Exception {
        linked();
        var response =
                mvc.perform(sso(get("/chat/api/scenarios"), "E1001", "ADMIN"))
                        .andExpect(status().isOk())
                        .andReturn();
        var session = (MockHttpSession) response.getRequest().getSession();
        assertThat(((AdminSession) session.getAttribute(AdminSession.SESSION_KEY)).hasRole("ADMIN"))
                .isTrue();
        mvc.perform(sso(get("/chat/api/scenarios").session(session), "E1001", "USER"))
                .andExpect(status().isOk());
        assertThat(((AdminSession) session.getAttribute(AdminSession.SESSION_KEY)).hasRole("ADMIN"))
                .isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM tb_user WHERE emp_no='E1001' AND auth_source='GATEWAY' AND password_hash IS NULL",
                                Integer.class))
                .isEqualTo(1);
        jdbc.update("UPDATE tb_user SET use_yn='N' WHERE emp_no='E1001'");
        mvc.perform(sso(get("/chat/api/scenarios").session(session), "E1001", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void differentEmployeeCannotReadChatEvenWithSameAnonymousCookie() throws Exception {
        linked();
        var response =
                mvc.perform(
                                sso(post("/chat/api/sessions/auto"), "OWNER1", "USER")
                                        .contentType("application/json")
                                        .content("{\"text\":\"전기요금\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        var json =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(response.getResponse().getContentAsString());
        String id = json.path("data").path("sessionId").asText();
        assertThat(id).isNotEmpty();
        var cookie = response.getResponse().getCookie("anonymous_id");
        mvc.perform(sso(get("/chat/api/sessions/" + id).cookie(cookie), "OWNER2", "USER"))
                .andExpect(status().isForbidden());
        mvc.perform(sso(get("/chat/api/sessions/" + id), "OWNER1", "USER"))
                .andExpect(status().isOk());
    }

    @Test
    void crawlDetailRendersTabsAndDocumentPaging() throws Exception {
        Long id =
                jdbc.queryForObject(
                        "INSERT INTO tb_crawl_target(url,label,use_yn) VALUES('https://example.com/settings-test','수집 화면 검증','Y') RETURNING crawl_target_no",
                        Long.class);
        jdbc.update(
                "INSERT INTO tb_crawl_document(target_no,url,title,content,url_hash,content_hash,status,http_status,fetched_at) SELECT ?, 'https://example.com/doc/'||n, '문서 '||n, '본문 확인',md5('url'||n),md5('body'||n),'SUCCESS',200,now() FROM generate_series(1,25) AS n",
                id);
        String html =
                mvc.perform(get("/admin/crawl-targets/" + id).session(admin("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(html).contains("수집 문서", "실행 이력", "대상 설정", "data-crawl-detail", "페이지당 20건");
        assertThat(org.jsoup.Jsoup.parse(html).select(".document-title")).hasSize(20);
        String second =
                mvc.perform(
                                get("/admin/crawl-targets/" + id)
                                        .param("docPage", "2")
                                        .session(admin("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(org.jsoup.Jsoup.parse(second).select(".document-title")).hasSize(5);
    }
}
