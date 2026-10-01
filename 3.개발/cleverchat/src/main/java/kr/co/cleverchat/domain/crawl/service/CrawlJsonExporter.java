package kr.co.cleverchat.domain.crawl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import org.springframework.stereotype.Component;

/** Writes a new UTF-8 snapshot for each fetched page, including unchanged pages. */
@Component
public class CrawlJsonExporter {
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CrawlExportRegistry registry;

    private final kr.co.cleverchat.domain.settings.SystemSettingsService settings;

    public CrawlJsonExporter(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CrawlJsonExporter(
            ObjectMapper objectMapper,
            kr.co.cleverchat.domain.settings.SystemSettingsService settings) {
        this.objectMapper = objectMapper;
        this.settings = settings;
    }

    public static String validateDirectory(String directory) {
        if (directory == null || directory.isBlank() || directory.length() > 1000) {
            throw new IllegalArgumentException("JSON 저장 폴더의 절대 경로를 입력해 주세요. (최대 1000자)");
        }
        try {
            Path path = Path.of(directory.trim());
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException("JSON 저장 폴더는 서버의 절대 경로로 입력해 주세요.");
            }
            path = path.normalize();
            if (Files.exists(path) && !Files.isDirectory(path)) {
                throw new IllegalArgumentException("JSON 저장 경로는 파일이 아닌 폴더여야 합니다.");
            }
            return path.toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("JSON 저장 폴더 경로가 올바르지 않습니다.");
        }
    }

    public ExportResult export(CrawlTarget target, CrawlDocument document) {
        if (!target.isJsonExportEnabled()) {
            return new ExportResult("DISABLED", null, null);
        }
        Path temporary = null;
        try {
            Path root =
                    Path.of(
                            validateDirectory(
                                    settings == null
                                            ? target.getJsonExportDirectory()
                                            : settings.exportDirectory(
                                                    target.getJsonExportDirectory())));
            Path directory =
                    root.resolve("target-" + target.getCrawlTargetNo())
                            .resolve(document.getFetchedAt().toLocalDate().toString());
            Files.createDirectories(directory);
            Path destination = directory.resolve(UUID.randomUUID() + ".json");
            temporary = Files.createTempFile(directory, ".crawl-", ".tmp");
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(
                            temporary.toFile(),
                            new Snapshot(
                                    1,
                                    document.getFetchedAt().toString(),
                                    target.getCrawlTargetNo(),
                                    target.getLabel(),
                                    document.getCrawlDocumentNo(),
                                    document.getUrl(),
                                    document.getTitle(),
                                    document.getContent(),
                                    document.getContentHash(),
                                    document.getHttpStatus()));
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination);
            }
            if (registry != null) registry.register(target.getCrawlTargetNo(), root, destination);
            return new ExportResult("SUCCESS", destination.toString(), null);
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            String message = "JSON 파일 저장 실패: " + e.getMessage();
            return new ExportResult(
                    "FAILED", null, message.substring(0, Math.min(message.length(), 1000)));
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ignored) {
                    // A leftover .tmp file is never presented as a completed JSON snapshot.
                }
            }
        }
    }

    public record ExportResult(String status, String path, String message) {
        public boolean failed() {
            return "FAILED".equals(status);
        }
    }

    private record Snapshot(
            int schemaVersion,
            String crawledAt,
            Long targetId,
            String targetLabel,
            Long documentId,
            String url,
            String title,
            String content,
            String contentHash,
            Integer httpStatus) {}
}
