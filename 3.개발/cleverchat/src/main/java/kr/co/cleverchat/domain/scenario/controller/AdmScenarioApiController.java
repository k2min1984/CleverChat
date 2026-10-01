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
public class AdmScenarioApiController {

    private final ScenarioService scenarioService;

    public AdmScenarioApiController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping
    public ApiResponse<?> scenarioList(String status) {
        return ApiResponse.ok(scenarioService.findAll(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Scenario> scenarioView(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.get(id));
    }

    @PostMapping
    public ApiResponse<Scenario> scenarioRegistProc(
            @Valid @RequestBody ScenarioDtos.SaveRequest request) {
        return ApiResponse.ok(scenarioService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Scenario> scenarioModifyProc(
            @PathVariable Long id, @Valid @RequestBody ScenarioDtos.SaveRequest request) {
        return ApiResponse.ok(scenarioService.update(id, request));
    }

    @PostMapping("/order")
    public ApiResponse<Void> scenarioOrderModifyProc(
            @Valid @RequestBody ScenarioDtos.ReorderRequest request) {
        scenarioService.reorderActiveScenarios(request.scenarioIds());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> scenarioDeleteProc(@PathVariable Long id) {
        scenarioService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<ScenarioVersion> scenarioVersionRegistProc(@PathVariable Long id) {
        return ApiResponse.ok(scenarioService.createVersion(id));
    }

    @PostMapping("/{id}/versions/{sourceVersionId}/copy")
    public ApiResponse<ScenarioVersion> scenarioVersionCopyProc(
            @PathVariable Long id, @PathVariable Long sourceVersionId) {
        return ApiResponse.ok(scenarioService.createVersionFromSource(id, sourceVersionId));
    }

    @GetMapping("/versions/{versionId}/graph")
    public ApiResponse<ScenarioGraphDtos.SaveRequest> scenarioGraphView(
            @PathVariable Long versionId) {
        return ApiResponse.ok(scenarioService.graph(versionId));
    }

    @PutMapping("/versions/{versionId}/graph")
    public ApiResponse<Void> scenarioGraphModifyProc(
            @PathVariable Long versionId,
            @Valid @RequestBody ScenarioGraphDtos.SaveRequest request) {
        scenarioService.saveGraph(versionId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/versions/{versionId}/publish")
    public ApiResponse<Void> scenarioPublishProc(@PathVariable Long versionId) {
        scenarioService.publish(versionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/activate/{versionId}")
    public ApiResponse<Void> scenarioActivateProc(
            @PathVariable Long id, @PathVariable Long versionId) {
        scenarioService.activate(id, versionId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> scenarioDeactivateProc(@PathVariable Long id) {
        scenarioService.deactivate(id);
        return ApiResponse.ok();
    }
}
