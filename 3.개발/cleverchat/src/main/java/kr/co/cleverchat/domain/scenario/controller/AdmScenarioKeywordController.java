package kr.co.cleverchat.domain.scenario.controller;

import jakarta.validation.Valid;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.scenario.dto.ScenarioKeywordDtos;
import kr.co.cleverchat.domain.scenario.service.ScenarioKeywordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/scenarios/{scenarioId}/keywords")
public class AdmScenarioKeywordController {

    private final ScenarioKeywordService keywordService;

    public AdmScenarioKeywordController(ScenarioKeywordService keywordService) {
        this.keywordService = keywordService;
    }

    @GetMapping
    public ApiResponse<?> scenarioKeywordList(@PathVariable Long scenarioId) {
        return ApiResponse.ok(keywordService.findKeywords(scenarioId));
    }

    @PutMapping
    public ApiResponse<Void> scenarioKeywordModifyProc(@PathVariable Long scenarioId, @Valid @RequestBody ScenarioKeywordDtos.ReplaceRequest request) {
        keywordService.replace(scenarioId, request);
        return ApiResponse.ok();
    }
}
