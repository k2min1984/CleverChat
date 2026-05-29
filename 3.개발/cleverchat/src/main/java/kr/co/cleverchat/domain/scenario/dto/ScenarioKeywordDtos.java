package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ScenarioKeywordDtos {

    private ScenarioKeywordDtos() {}

    public record ReplaceRequest(@Valid @Size(max = 100) List<KeywordRequest> keywords) {}

    public record KeywordRequest(
            @NotBlank @Size(max = 100) String keyword,
            @NotNull @Min(0) @Max(100000) Integer weight,
            boolean enabled,
            @Valid @Size(max = 50) List<SynonymRequest> synonyms) {}

    public record SynonymRequest(
            @NotBlank @Size(max = 100) String synonym,
            @NotNull @Min(0) @Max(100000) Integer weight,
            boolean enabled) {}
}
