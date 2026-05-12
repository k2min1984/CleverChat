package kr.co.cleverchat.domain.scenario.controller;

import jakarta.validation.Valid;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.scenario.dto.ScenarioDtos;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.scenario.service.ScenarioService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/scenarios")
public class ScenarioApiController {

    private final ScenarioService scenarioService;

    public ScenarioApiController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping
    public ApiResponse<?> list(String status) {
        return ApiResponse.ok(scenarioService.findAll(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Scenario> detail(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.get(id));
    }

    @PostMapping
    public ApiResponse<Scenario> create(@Valid @RequestBody ScenarioDtos.SaveRequest request) {
        return ApiResponse.ok(scenarioService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Scenario> update(@PathVariable Long id, @Valid @RequestBody ScenarioDtos.SaveRequest request) {
        return ApiResponse.ok(scenarioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scenarioService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<ScenarioVersion> createVersion(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.createVersion(id));
    }

    @PutMapping("/versions/{versionId}/graph")
    public ApiResponse<Void> saveGraph(@PathVariable Long versionId, @Valid @RequestBody ScenarioGraphDtos.SaveRequest request) {
        scenarioService.saveGraph(versionId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/versions/{versionId}/publish")
    public ApiResponse<Void> publish(@PathVariable Long versionId) {
        scenarioService.publish(versionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/activate/{versionId}")
    public ApiResponse<Void> activate(@PathVariable Long id, @PathVariable Long versionId) {
        scenarioService.activate(id, versionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        scenarioService.deactivate(id);
        return ApiResponse.ok();
    }
}
