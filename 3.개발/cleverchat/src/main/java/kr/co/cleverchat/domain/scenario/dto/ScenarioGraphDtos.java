package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ScenarioGraphDtos {

    private ScenarioGraphDtos() {
    }

    public record SaveRequest(
        @NotBlank @Size(max = 80) String startNodeKey,
        @Valid @NotEmpty List<NodeRequest> nodes
    ) {
    }

    public record NodeRequest(
        @NotBlank @Size(max = 80) String nodeKey,
        @NotBlank @Size(max = 20) String nodeType,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 10000) String content,
        int sortOrder,
        String metadata,
        @Valid List<OptionRequest> options
    ) {
    }

    public record OptionRequest(
        @NotBlank @Size(max = 150) String label,
        @Size(max = 80) String nextNodeKey,
        @Size(max = 500) String conditionExpr,
        int sortOrder,
        boolean enabled
    ) {
    }
}
