package kr.co.cleverchat.domain.settings;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RetentionResponse;
import kr.co.cleverchat.domain.crawl.service.*;
import org.junit.jupiter.api.Test;

class RetentionMaintenanceTest {
    private final RuntimeSettingsService settings = mock(RuntimeSettingsService.class);
    private final MaintenanceMapper mapper = mock(MaintenanceMapper.class);
    private final CrawlService crawl = mock(CrawlService.class);
    private final CrawlExportRegistry exports = mock(CrawlExportRegistry.class);
    private final RetentionMaintenance maintenance =
            new RetentionMaintenance(settings, mapper, crawl, exports);

    private Map<String, Object> defaults() {
        var values = new HashMap<String, Object>();
        for (var key : RuntimeSetting.values()) values.put(key.name(), key.defaultValue);
        return values;
    }

    @Test
    void runsAtConfiguredMinuteOnlyOnceAndDoesNotEnableJsonCleanupImplicitly() {
        var values = defaults();
        when(settings.current()).thenReturn(new RuntimeSettingsService.Snapshot(values));
        when(mapper.claim(any())).thenReturn(1, 0);
        when(crawl.deleteExpiredScheduled()).thenReturn(mock(RetentionResponse.class));
        maintenance.runDue(ZonedDateTime.parse("2026-09-22T03:44:00+09:00[Asia/Seoul]"));
        verifyNoInteractions(crawl, mapper, exports);
        maintenance.runDue(ZonedDateTime.parse("2026-09-22T03:45:00+09:00[Asia/Seoul]"));
        maintenance.runDue(ZonedDateTime.parse("2026-09-22T03:45:30+09:00[Asia/Seoul]"));
        verify(crawl, times(1)).deleteExpiredScheduled();
        verifyNoInteractions(exports);
    }

    @Test
    void bothDisabledLeavesDatabaseAndFilesUntouched() {
        var values = defaults();
        values.put("RETENTION_ENABLED", false);
        when(settings.current()).thenReturn(new RuntimeSettingsService.Snapshot(values));
        maintenance.runDue(ZonedDateTime.parse("2026-09-22T03:45:00+09:00[Asia/Seoul]"));
        verifyNoInteractions(mapper, crawl, exports);
    }
}
