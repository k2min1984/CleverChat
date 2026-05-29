package kr.co.cleverchat.domain.scenario.controller;

import jakarta.validation.Valid;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.scenario.dto.ScenarioCategoryDtos;
import kr.co.cleverchat.domain.scenario.model.ScenarioCategory;
import kr.co.cleverchat.domain.scenario.service.ScenarioCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/scenario-categories")
public class AdmScenarioCategoryController {

    private final ScenarioCategoryService categoryService;

    public AdmScenarioCategoryController(ScenarioCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<?> scenarioCategoryList() {
        return ApiResponse.ok(categoryService.findAll());
    }

    @PostMapping
    public ApiResponse<ScenarioCategory> scenarioCategoryRegistProc(
            @Valid @RequestBody ScenarioCategoryDtos.SaveRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScenarioCategory> scenarioCategoryModifyProc(
            @PathVariable Long id, @Valid @RequestBody ScenarioCategoryDtos.SaveRequest request) {
        return ApiResponse.ok(categoryService.update(id, request));
    }
}
