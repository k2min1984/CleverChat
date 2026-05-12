package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ScenarioCategoryDtos {

    private ScenarioCategoryDtos() {
    }

    public record SaveRequest(
        Long parentId,
        @NotBlank @Size(max = 100) String name,
        int sortOrder,
        boolean enabled
    ) {
    }
}
