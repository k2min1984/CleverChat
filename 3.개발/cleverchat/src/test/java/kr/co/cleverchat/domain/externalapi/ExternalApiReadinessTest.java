package kr.co.cleverchat.domain.externalapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExternalApiReadinessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path REPO_ROOT = PROJECT_ROOT.getParent().getParent();

    @Test
    void docsDescribeDisabledStubBoundaryAndNoCorsOpening() throws IOException {
        String apiDocs = readRepoFile("2.설계/03.API설계서/API목록.md");
        String securityDocs = readRepoFile("2.설계/05.보안설계서/M3_사용자화면보안노트.md");

        assertThat(apiDocs)
                .contains("/external/api/v1")
                .contains("CLEVERCHAT_EXTERNAL_API_ENABLED")
                .contains("CLEVERCHAT_EXTERNAL_API_KEY_SHA256")
                .contains("CORS")
                .contains("business API");
        assertThat(securityDocs)
                .contains("/external/api/v1")
                .contains("SHA-256")
                .contains("raw API key")
                .contains("CORS");
    }

    @Test
    void webMvcInterceptorsDoNotAttachAdminCsrfToExternalApi() throws IOException {
        String webMvcConfig =
                Files.readString(
                        PROJECT_ROOT.resolve(
                                "src/main/java/kr/co/cleverchat/config/WebMvcConfig.java"));

        assertThat(webMvcConfig)
                .contains(".addPathPatterns(\"/admin/**\")")
                .doesNotContain("/external/api");
        assertThat(webMvcConfig).contains(".addPathPatterns(\"/admin/**\", \"/logout\")");
    }

    private String readRepoFile(String path) throws IOException {
        return Files.readString(REPO_ROOT.resolve(path));
    }
}
