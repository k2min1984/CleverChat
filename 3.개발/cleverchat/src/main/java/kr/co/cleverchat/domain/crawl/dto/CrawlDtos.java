package kr.co.cleverchat.domain.crawl.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;

public final class CrawlDtos {

    private CrawlDtos() {}

    public record TargetRequest(
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 200) String label,
            @Pattern(regexp = "Y|N") String useYn,
            Boolean scheduleEnabled,
            @Min(5) @Max(10080) Integer scheduleIntervalMinutes,
            @Pattern(regexp = "INTERVAL|CRON") String scheduleMode,
            @Size(max = 120) String scheduleCron) {}

    public record ScheduleRequest(
            Boolean scheduleEnabled,
            @Min(5) @Max(10080) Integer scheduleIntervalMinutes,
            @Pattern(regexp = "INTERVAL|CRON") String scheduleMode,
            @Size(max = 120) String scheduleCron) {}

    public record SchedulePreviewResponse(
            boolean scheduleEnabled, String scheduleMode, List<OffsetDateTime> nextRunTimes) {}

    public record TargetListResponse(List<CrawlTarget> targets) {}

    public record RunResponse(CrawlRunLog run, CrawlDocument document) {}

    public record ReviewRequest(@Size(max = 1000) String comment) {}

    public record RetentionResponse(
            int runRetentionDays,
            int documentRetentionDays,
            OffsetDateTime runCutoff,
            OffsetDateTime documentCutoff,
            long wouldDeleteRunLogs,
            long wouldDeleteDocuments,
            int deletedRunLogs,
            int deletedDocuments,
            boolean dryRun) {}
}
