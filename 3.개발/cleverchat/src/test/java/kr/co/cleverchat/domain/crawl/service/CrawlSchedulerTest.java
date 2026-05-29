package kr.co.cleverchat.domain.crawl.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import org.junit.jupiter.api.Test;

class CrawlSchedulerTest {

    private final CrawlService crawlService = org.mockito.Mockito.mock(CrawlService.class);
    private final CrawlScheduler scheduler = new CrawlScheduler(crawlService);

    @Test
    void runsDueTargets() {
        CrawlTarget target = new CrawlTarget();
        target.setId(10L);
        when(crawlService.dueTargets(10)).thenReturn(List.of(target));

        scheduler.runDueTargets();

        verify(crawlService).runScheduled(10L);
    }

    @Test
    void deletesExpiredOperationalData() {
        when(crawlService.deleteExpired(null, null, false))
                .thenReturn(
                        new kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RetentionResponse(
                                365,
                                90,
                                java.time.OffsetDateTime.now(),
                                java.time.OffsetDateTime.now(),
                                2,
                                1,
                                2,
                                1,
                                false));

        scheduler.deleteExpiredOperationalData();

        verify(crawlService).deleteExpired(null, null, false);
    }
}
