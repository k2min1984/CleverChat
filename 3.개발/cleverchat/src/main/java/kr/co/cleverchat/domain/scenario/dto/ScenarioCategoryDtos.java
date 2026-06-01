package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ScenarioCategoryDtos {

    private ScenarioCategoryDtos() {}

    public record SaveRequest(
            @Positive Long pScenarioCategoryNo,
            @NotBlank @Size(max = 100) String name,
            @Min(0) @Max(100000) int sortOrder,
            @Pattern(regexp = "Y|N") String useYn) {}
}
