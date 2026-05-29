package kr.co.cleverchat.domain.ops.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public final class OpsDtos {

    private OpsDtos() {}

    public record MetricSummary(String key, String label, long today, long last7Days) {}

    public record StatisticsSummary(OffsetDateTime generatedAt, List<MetricSummary> metrics) {}

    public record NoticeRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String content,
            Boolean enabled,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            @Min(0) @Max(10000) Integer priority) {}

    public record NotificationChannelRequest(
            @NotBlank @Size(max = 100) String name,
            String type,
            Boolean enabled,
            @NotBlank @Pattern(regexp = "[A-Z0-9_]{3,100}") String endpointEnvKey,
            @Pattern(regexp = "[A-Z0-9_]{3,100}") String previousEndpointEnvKey,
            @Min(1) @Max(1000) Integer rateLimitPerHour) {}

    public record NotificationTestResponse(
            Long eventId, boolean sent, String status, String message) {}
}
