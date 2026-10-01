package kr.co.cleverchat.domain.crawl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CrawlJsonExporterTest {
    @TempDir Path directory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CrawlJsonExporter exporter = new CrawlJsonExporter(mapper);

    @Test
    void disabledExportCreatesNoDirectories() {
        CrawlTarget target = target();
        target.setJsonExportEnabled(false);
        assertThat(exporter.export(target, document()).status()).isEqualTo("DISABLED");
        assertThat(directory.resolve("target-12")).doesNotExist();
    }

    @Test
    void writesUtf8SnapshotsWithProvenanceAndDoesNotOverwritePreviousFetch() throws Exception {
        CrawlTarget target = target();
        var first = exporter.export(target, document());
        var second = exporter.export(target, document());
        assertThat(first.status()).isEqualTo("SUCCESS");
        assertThat(second.path()).isNotEqualTo(first.path());
        var json = mapper.readTree(Files.readString(Path.of(first.path())));
        assertThat(json.get("content").asText()).isEqualTo("전기 요금 안내\n둘째 줄 \"인용\" \\ 경로");
        assertThat(json.get("url").asText()).isEqualTo("https://example.com/help");
        assertThat(json.get("targetId").asLong()).isEqualTo(12L);
        assertThat(json.get("schemaVersion").asInt()).isOne();
        assertThat(json.get("crawledAt").asText()).isEqualTo("2026-09-21T12:00+09:00");
        try (var paths = Files.walk(directory)) {
            assertThat(paths.filter(Files::isRegularFile).toList())
                    .hasSize(2)
                    .allMatch(path -> path.toString().endsWith(".json"));
        }
    }

    @Test
    void writeFailureIsExplicitAndLeavesExistingFileUntouched() throws Exception {
        Path file = directory.resolve("existing.txt");
        Files.writeString(file, "keep");
        CrawlTarget target = target();
        target.setJsonExportDirectory(file.toString());
        var result = exporter.export(target, document());
        assertThat(result.failed()).isTrue();
        assertThat(result.path()).isNull();
        assertThat(Files.readString(file)).isEqualTo("keep");
    }

    @Test
    void rejectsMissingAndRelativeDirectories() {
        assertThatThrownBy(() -> CrawlJsonExporter.validateDirectory(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrawlJsonExporter.validateDirectory("relative/path"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CrawlJsonExporter.validateDirectory(directory.resolve("new").toString()))
                .isEqualTo(directory.resolve("new").toString());
    }

    private CrawlTarget target() {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(12L);
        target.setLabel("자료실");
        target.setJsonExportEnabled(true);
        target.setJsonExportDirectory(directory.toString());
        return target;
    }

    private CrawlDocument document() {
        CrawlDocument document = new CrawlDocument();
        document.setCrawlDocumentNo(34L);
        document.setUrl("https://example.com/help");
        document.setTitle("한글 제목");
        document.setContent("전기 요금 안내\n둘째 줄 \"인용\" \\ 경로");
        document.setContentHash("hash");
        document.setHttpStatus(200);
        document.setFetchedAt(OffsetDateTime.parse("2026-09-21T12:00:00+09:00"));
        return document;
    }
}
