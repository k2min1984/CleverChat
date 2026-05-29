package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ScenarioGraphDtos {

    private ScenarioGraphDtos() {}

    public record SaveRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String startNodeKey,
            @Valid @NotEmpty @Size(max = 200) List<NodeRequest> nodes) {}

    public record NodeRequest(
            @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String nodeKey,
            @NotBlank @Size(max = 20) @Pattern(regexp = "QUESTION|ANSWER|BRANCH|END")
                    String nodeType,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 10000) String content,
            @Min(0) @Max(100000) int sortOrder,
            @Size(max = 5000) String metadata,
            @Valid @Size(max = 50) List<OptionRequest> options) {}

    public record OptionRequest(
            @NotBlank @Size(max = 150) String label,
            @Size(max = 80) @Pattern(regexp = "|[A-Za-z0-9_-]{1,80}") String nextNodeKey,
            @Size(max = 500) String conditionExpr,
            @Min(0) @Max(100000) int sortOrder,
            boolean enabled) {}
}
