package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QualityAutomationReadinessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path REPO_ROOT = PROJECT_ROOT.getParent().getParent();

    @Test
    void pomDefinesSpotlessCheckOnlyJavaBaseline() throws IOException {
        String pom = Files.readString(PROJECT_ROOT.resolve("pom.xml"));

        assertThat(pom)
                .contains("<artifactId>spotless-maven-plugin</artifactId>")
                .contains("<spotless-maven-plugin.version>3.4.0</spotless-maven-plugin.version>")
                .contains("<lineEndings>UNIX</lineEndings>")
                .contains("<include>src/main/java/**/*.java</include>")
                .contains("<include>src/test/java/**/*.java</include>")
                .contains("<removeUnusedImports/>")
                .contains("<googleJavaFormat>")
                .contains("<style>AOSP</style>")
                .contains("<trimTrailingWhitespace/>")
                .contains("<endWithNewline/>");
        assertThat(pom).doesNotContain("<goal>check</goal>", "<goal>apply</goal>");
    }

    @Test
    void ciWorkflowRunsTestSpotlessAndDiffCheck() throws IOException {
        String workflow = readRepoFile(".github/workflows/cleverchat-ci.yml");

        assertThat(workflow)
                .contains("actions/setup-java@v4")
                .contains("java-version: '17'")
                .contains("cache: maven")
                .contains("working-directory: 3.개발/cleverchat")
                .contains(".\\mvnw.cmd clean test")
                .contains(".\\mvnw.cmd spotless:check")
                .contains("git diff --check")
                .contains("Optional integration-test job")
                .contains(".\\mvnw.cmd -Pit test");
        assertThat(workflow).doesNotContain("continue-on-error: true");
    }

    @Test
    void docsWarnThatSpotlessApplyNeedsSeparateFormattingChange() throws IOException {
        String docs = readRepoFile("2.설계/04.아키텍처/M0_품질자동화_CI.md");
        String wbs = readRepoFile("1.기획/WBS.md");

        assertThat(docs)
                .contains("Spotless Check-only")
                .contains(".\\mvnw.cmd spotless:check")
                .contains(".\\mvnw.cmd spotless:apply")
                .contains("대량 포맷 전용 PR")
                .contains("Java baseline")
                .contains("blocking check")
                .contains("GitHub Actions");
        assertThat(wbs).contains("Java baseline 정리 완료").contains("GitHub Actions 초안");
    }

    private String readRepoFile(String path) throws IOException {
        return Files.readString(REPO_ROOT.resolve(path));
    }
}
