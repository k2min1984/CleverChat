package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ScenarioKeywordDtos {

    private ScenarioKeywordDtos() {
    }

    public record ReplaceRequest(@Valid List<KeywordRequest> keywords) {
    }

    public record KeywordRequest(
        @NotBlank @Size(max = 100) String keyword,
        @NotNull Integer weight,
        boolean enabled,
        @Valid List<SynonymRequest> synonyms
    ) {
    }

    public record SynonymRequest(
        @NotBlank @Size(max = 100) String synonym,
        @NotNull Integer weight,
        boolean enabled
    ) {
    }
}
