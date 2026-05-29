package kr.co.cleverchat.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ChatEmbedReadinessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path REPO_ROOT = PROJECT_ROOT.getParent().getParent();

    @Test
    void securityHeadersKeepIframeEmbeddingBlocked() {
        String csp = SecurityHeadersFilter.CSP_POLICY;

        assertThat(csp).contains("frame-ancestors 'none'");
        assertThat(csp)
                .doesNotContain("frame-src")
                .doesNotContain("child-src")
                .doesNotContain("https://")
                .doesNotContain("*");
    }

    @Test
    void embedSampleOnlyLaunchesChatPage() throws IOException {
        String sample =
                Files.readString(PROJECT_ROOT.resolve("deploy/embed/cleverchat-embed-sample.html"));
        String lower = sample.toLowerCase(Locale.ROOT);

        assertThat(sample).contains("data-cleverchat-url=\"/chat\"");
        assertThat(sample).contains("window.open");
        assertThat(sample).contains("aria-label");
        assertThat(lower)
                .doesNotContain("<iframe")
                .doesNotContain("/chat/api")
                .doesNotContain("token")
                .doesNotContain("secret")
                .doesNotContain("apikey")
                .doesNotContain("api_key")
                .doesNotContain("authorization")
                .doesNotContain("?anonymous")
                .doesNotContain("?email")
                .doesNotContain("?phone");
    }

    @Test
    void embedDocsDefineSecurityBoundaryAndFollowUps() throws IOException {
        String design = readRepoFile("2.설계/01.화면설계서/M3_챗봇임베드위젯_설계.md");
        String securityNote = readRepoFile("2.설계/05.보안설계서/M3_사용자화면보안노트.md");
        String headerMemo = readRepoFile("2.설계/06.운영메모/M7_보안헤더_운영메모.md");

        assertThat(design)
                .contains("launcher-only")
                .contains("frame-ancestors 'none'")
                .contains("X-Frame-Options: DENY")
                .contains("CORS")
                .contains("CSRF")
                .contains("Cookie SameSite/Secure");
        assertThat(securityNote)
                .contains("launcher-only")
                .contains("iframe embedding remains blocked");
        assertThat(headerMemo).contains("launcher-only").contains("iframe allowlist");
    }

    private String readRepoFile(String path) throws IOException {
        return Files.readString(REPO_ROOT.resolve(path));
    }
}
