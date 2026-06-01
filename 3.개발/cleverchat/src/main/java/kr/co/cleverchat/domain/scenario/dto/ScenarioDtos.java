package kr.co.cleverchat.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ScenarioDtos {

    private ScenarioDtos() {}

    public record SaveRequest(
            @NotNull Long categoryNo,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 2000) String description) {}

    public record Publishability(
            boolean hasGraph, boolean hasStartNode, boolean publishable, String reason) {}
}
