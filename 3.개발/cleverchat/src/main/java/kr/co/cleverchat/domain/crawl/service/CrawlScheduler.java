package kr.co.cleverchat.domain.crawl.service;

import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(CrawlScheduler.class);
    private static final int BATCH_SIZE = 10;

    private final CrawlService crawlService;

    public CrawlScheduler(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void runDueTargets() {
        for (CrawlTarget target : crawlService.dueTargets(BATCH_SIZE)) {
            try {
                crawlService.runScheduled(target.getCrawlTargetNo());
            } catch (BusinessException e) {
                log.info(
                        "Scheduled crawl failed: targetId={}, code={}, message={}",
                        target.getCrawlTargetNo(),
                        e.getErrorCode(),
                        e.getMessage());
            } catch (RuntimeException e) {
                log.warn(
                        "Scheduled crawl failed unexpectedly: targetId={}",
                        target.getCrawlTargetNo(),
                        e);
            }
        }
    }

    public void deleteExpiredOperationalData() {
        var response = crawlService.deleteExpiredScheduled();
        log.info(
                "Deleted expired crawl operational data: runLogs={}, documents={}, runRetentionDays={}, documentRetentionDays={}",
                response.deletedRunLogs(),
                response.deletedDocuments(),
                response.runRetentionDays(),
                response.documentRetentionDays());
    }
}
