package kr.co.cleverchat.domain.crawl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Backfills Korean morpheme tokens for crawl documents created before morphological indexing
 * existed (or before a prior backfill completed). Runs once at startup, in batches, until no
 * documents are left without tokens. Idempotent — after the first successful pass it finds nothing
 * and exits immediately on subsequent boots.
 *
 * <p>Disable with {@code cleverchat.crawl.morph-backfill-on-startup=false}.
 */
@Component
@Order(100)
@ConditionalOnProperty(
        name = "cleverchat.crawl.morph-backfill-on-startup",
        havingValue = "true",
        matchIfMissing = true)
public class CrawlTokenBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CrawlTokenBackfillRunner.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_BATCHES = 10_000;

    private final CrawlService crawlService;

    public CrawlTokenBackfillRunner(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Override
    public void run(ApplicationArguments args) {
        long total = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            int processed;
            try {
                processed = crawlService.reindexMissingTokens(BATCH_SIZE);
            } catch (RuntimeException e) {
                log.warn(
                        "Crawl morpheme backfill aborted after {} documents: {}",
                        total,
                        e.toString());
                return;
            }
            if (processed == 0) {
                break;
            }
            total += processed;
        }
        if (total > 0) {
            log.info("Crawl morpheme backfill complete: {} documents reindexed", total);
        }
    }
}
