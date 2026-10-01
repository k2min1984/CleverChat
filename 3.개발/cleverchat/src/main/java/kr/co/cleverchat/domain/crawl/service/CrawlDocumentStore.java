package kr.co.cleverchat.domain.crawl.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
import kr.co.cleverchat.domain.crawl.mapper.CrawlMapper;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlJsonExporter.ExportResult;
import org.springframework.stereotype.Service;

/** Shared persistence and snapshot behavior for HTTP and browser crawling. */
@Service
public class CrawlDocumentStore {
    private final CrawlMapper mapper;
    private final KoreanMorphAnalyzer analyzer;
    private final CrawlJsonExporter exporter;

    public CrawlDocumentStore(
            CrawlMapper mapper, KoreanMorphAnalyzer analyzer, CrawlJsonExporter exporter) {
        this.mapper = mapper;
        this.analyzer = analyzer;
        this.exporter = exporter;
    }

    public StoredDocument store(
            CrawlTarget target, String url, String title, String content, Integer httpStatus) {
        CrawlDocument document = new CrawlDocument();
        document.setTargetNo(target.getCrawlTargetNo());
        document.setUrl(url);
        document.setTitle(title == null ? null : title.substring(0, Math.min(title.length(), 500)));
        document.setContent(content);
        document.setContentTokens(analyzer.tokenize(tokenSource(document.getTitle(), content)));
        document.setUrlHash(sha256(url));
        document.setContentHash(sha256(content));
        document.setStatus("SUCCESS");
        document.setHttpStatus(httpStatus);
        document.setFetchedAt(OffsetDateTime.now());

        CrawlDocument byUrl =
                mapper.findDocumentByTargetAndUrlHash(
                        target.getCrawlTargetNo(), document.getUrlHash());
        CrawlDocument byContent =
                mapper.findDocumentByTargetAndHash(
                        target.getCrawlTargetNo(), document.getContentHash());
        boolean duplicate = byContent != null;
        if (byContent != null
                && (byUrl == null
                        || !Objects.equals(
                                byContent.getCrawlDocumentNo(), byUrl.getCrawlDocumentNo()))) {
            document.setCrawlDocumentNo(byContent.getCrawlDocumentNo());
        } else if (byUrl != null) {
            document.setCrawlDocumentNo(byUrl.getCrawlDocumentNo());
            mapper.updateDocument(document);
        } else {
            mapper.insertDocument(document);
        }
        // Export the newly fetched page even if its content already exists in the database.
        return new StoredDocument(document, duplicate, exporter.export(target, document));
    }

    public static String tokenSource(String title, String content) {
        return (title == null ? "" : title) + " " + (content == null ? "" : content);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    public record StoredDocument(CrawlDocument document, boolean duplicate, ExportResult export) {}
}
