package kr.co.cleverchat.domain.scenario.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.scenario.dto.ScenarioDtos;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.scenario.service.ScenarioCategoryService;
import kr.co.cleverchat.domain.scenario.service.ScenarioKeywordService;
import kr.co.cleverchat.domain.scenario.service.ScenarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/scenarios")
public class AdmScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioCategoryService categoryService;
    private final ScenarioKeywordService keywordService;

    public AdmScenarioController(
        ScenarioService scenarioService,
        ScenarioCategoryService categoryService,
        ScenarioKeywordService keywordService
    ) {
        this.scenarioService = scenarioService;
        this.categoryService = categoryService;
        this.keywordService = keywordService;
    }

    @GetMapping
    public String scenarioList(@RequestParam(required = false) String status, Model model) {
        model.addAttribute("scenarios", scenarioService.findAll(status));
        model.addAttribute("status", status);
        return "admmgr/scenario/scenarioList";
    }

    @GetMapping("/new")
    public String scenarioRegist(Model model) {
        model.addAttribute("scenarioForm", new ScenarioForm());
        model.addAttribute("formAction", "/admin/scenarios");
        model.addAttribute("categories", categoryService.findAll());
        return "admmgr/scenario/scenarioRegist";
    }

    @PostMapping
    public String scenarioRegistProc(@Valid @ModelAttribute("scenarioForm") ScenarioForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/admin/scenarios");
            model.addAttribute("categories", categoryService.findAll());
            return "admmgr/scenario/scenarioRegist";
        }
        Long id = scenarioService.create(new ScenarioDtos.SaveRequest(form.getCategoryId(), form.getTitle(), form.getDescription())).getId();
        return "redirect:/admin/scenarios/" + id;
    }

    @GetMapping("/{id}")
    public String scenarioView(@PathVariable Long id, Model model) {
        model.addAttribute("scenario", scenarioService.get(id));
        model.addAttribute("versions", scenarioService.versions(id));
        model.addAttribute("keywords", keywordService.findKeywords(id));
        return "admmgr/scenario/scenarioView";
    }

    @GetMapping("/{id}/edit")
    public String scenarioModify(@PathVariable Long id, Model model) {
        var scenario = scenarioService.get(id);
        ScenarioForm form = new ScenarioForm();
        form.setCategoryId(scenario.getCategoryId());
        form.setTitle(scenario.getTitle());
        form.setDescription(scenario.getDescription());
        model.addAttribute("scenario", scenario);
        model.addAttribute("scenarioForm", form);
        model.addAttribute("formAction", "/admin/scenarios/" + scenario.getId());
        model.addAttribute("categories", categoryService.findAll());
        return "admmgr/scenario/scenarioRegist";
    }

    @PostMapping("/{id}")
    public String scenarioModifyProc(@PathVariable Long id, @Valid @ModelAttribute("scenarioForm") ScenarioForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("scenario", scenarioService.get(id));
            model.addAttribute("formAction", "/admin/scenarios/" + id);
            model.addAttribute("categories", categoryService.findAll());
            return "admmgr/scenario/scenarioRegist";
        }
        scenarioService.update(id, new ScenarioDtos.SaveRequest(form.getCategoryId(), form.getTitle(), form.getDescription()));
        return "redirect:/admin/scenarios/" + id;
    }

    @PostMapping("/{id}/versions")
    public String scenarioVersionRegistProc(@PathVariable Long id) {
        scenarioService.createVersion(id);
        return "redirect:/admin/scenarios/" + id;
    }

    @PostMapping("/versions/{versionId}/publish")
    public String scenarioPublishProc(@PathVariable Long versionId, @RequestParam Long scenarioId) {
        scenarioService.publish(versionId);
        return "redirect:/admin/scenarios/" + scenarioId;
    }

    @PostMapping("/{id}/activate")
    public String scenarioActivateProc(@PathVariable Long id, @RequestParam Long versionId) {
        scenarioService.activate(id, versionId);
        return "redirect:/admin/scenarios/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String scenarioDeactivateProc(@PathVariable Long id) {
        scenarioService.deactivate(id);
        return "redirect:/admin/scenarios/" + id;
    }

    @GetMapping("/{scenarioId}/versions/{versionId}/graph")
    public String scenarioGraphEdit(@PathVariable Long scenarioId, @PathVariable Long versionId, Model model) {
        var scenario = scenarioService.get(scenarioId);
        ScenarioVersion version = scenarioService.version(versionId);
        if (!scenarioId.equals(version.getScenarioId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 버전만 편집할 수 있습니다.");
        }
        model.addAttribute("scenario", scenario);
        model.addAttribute("version", version);
        model.addAttribute("scenarioId", scenarioId);
        model.addAttribute("versionId", versionId);
        model.addAttribute("backUrl", "/admin/scenarios/" + scenarioId);
        model.addAttribute("previewUrl", "/admin/scenarios/versions/" + versionId + "/preview");
        model.addAttribute("graphLoadUrl", "/admin/api/scenarios/versions/" + versionId + "/graph");
        model.addAttribute("graphSaveUrl", "/admin/api/scenarios/versions/" + versionId + "/graph");
        return "admmgr/scenario/scenarioGraphEdit";
    }

    @GetMapping("/versions/{versionId}/preview")
    public String scenarioPreviewLayer(@PathVariable Long versionId, @RequestParam(required = false) Long nodeId, Model model) {
        ScenarioVersion version = scenarioService.version(versionId);
        var nodes = scenarioService.nodes(versionId);
        ScenarioNode current = nodeId == null && version.getStartNodeId() != null
            ? nodes.stream().filter(node -> node.getId().equals(version.getStartNodeId())).findFirst().orElse(null)
            : nodes.stream()
            .filter(node -> node.getId().equals(nodeId))
            .findFirst()
            .orElse(null);
        model.addAttribute("version", version);
        model.addAttribute("current", current);
        model.addAttribute("options", current == null ? java.util.List.<ScenarioNodeOption>of() : scenarioService.options(current.getId()));
        return "admmgr/scenario/scenarioPreviewLayer";
    }

    public static class ScenarioForm {
        @NotNull
        private Long categoryId;
        @NotBlank
        @Size(max = 150)
        private String title;
        @Size(max = 2000)
        private String description;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
