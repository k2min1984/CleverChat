package kr.co.cleverchat.domain.settings;

import static kr.co.cleverchat.domain.settings.RuntimeSetting.*;

import java.time.*;
import kr.co.cleverchat.domain.crawl.service.CrawlExportRegistry;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionMaintenance {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final RuntimeSettingsService settings;
    private final MaintenanceMapper mapper;
    private final CrawlService crawl;
    private final CrawlExportRegistry exports;

    public RetentionMaintenance(
            RuntimeSettingsService settings,
            MaintenanceMapper mapper,
            CrawlService crawl,
            CrawlExportRegistry exports) {
        this.settings = settings;
        this.mapper = mapper;
        this.crawl = crawl;
        this.exports = exports;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void poll() {
        runDue(ZonedDateTime.now(ZONE));
    }

    void runDue(ZonedDateTime now) {
        var config = settings.current();
        if (!config.bool(RETENTION_ENABLED) && !config.bool(JSON_RETENTION_ENABLED)) return;
        if (!now.withZoneSameInstant(ZONE)
                .toLocalTime()
                .withSecond(0)
                .withNano(0)
                .equals(LocalTime.parse(config.text(RETENTION_TIME)))) return;
        LocalDate date = now.withZoneSameInstant(ZONE).toLocalDate();
        if (mapper.claim(date) != 1) return;
        try {
            String message = "DB 정리 사용 안 함";
            if (config.bool(RETENTION_ENABLED)) {
                var result = crawl.deleteExpiredScheduled();
                message =
                        "DB 문서 "
                                + result.deletedDocuments()
                                + "건, 실행 로그 "
                                + result.deletedRunLogs()
                                + "건 정리";
            }
            if (config.bool(JSON_RETENTION_ENABLED))
                message +=
                        " · "
                                + exports.cleanup(
                                        now.toOffsetDateTime()
                                                .minusDays(config.integer(JSON_RETENTION_DAYS)));
            mapper.finish(date, message);
        } catch (RuntimeException e) {
            mapper.finish(date, "자동 정리 실패. DB 연결 및 파일 접근 상태를 확인하세요.");
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Retention maintenance failed", e);
        }
    }
}
