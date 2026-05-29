package kr.co.cleverchat.domain.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.search.model.SearchResultItem;

public final class SearchDtos {

    private SearchDtos() {}

    public record SearchTestRequest(
            @NotBlank @Size(max = 200) String query, @Min(1) @Max(20) Integer limit) {}

    public record SearchResponse(
            String normalizedQuery, int resultCount, List<SearchResultItem> results) {}

    public record PopularRebuildRequest(@NotNull LocalDate statDate) {}

    public record PopularResponse(LocalDate from, LocalDate to, List<?> items) {}

    public record PopularRebuildResponse(
            LocalDate statDate,
            int rebuiltCount,
            int deletedBeforeRebuild,
            boolean idempotent,
            OffsetDateTime rebuiltAt) {}

    public record RetentionResponse(
            int retentionDays,
            OffsetDateTime cutoff,
            int deletedSearchLogs,
            int deletedBlockLogs,
            long wouldDeleteSearchLogs,
            long wouldDeleteBlockLogs,
            boolean dryRun) {}
}
