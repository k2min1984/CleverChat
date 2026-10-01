package kr.co.cleverchat.domain.scenario.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/scenarios")
public class AdmScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioCategoryService categoryService;
    private final ScenarioKeywordService keywordService;

    public AdmScenarioController(
            ScenarioService scenarioService,
            ScenarioCategoryService categoryService,
            ScenarioKeywordService keywordService) {
        this.scenarioService = scenarioService;
        this.categoryService = categoryService;
        this.keywordService = keywordService;
    }

    @GetMapping
    public String scenarioList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var filteredScenarios =
                scenarioService.findAll(null).stream()
                        .filter(
                                scenario ->
                                        categoryId == null
                                                || categoryId.equals(scenario.getCategoryNo()))
                        .filter(
                                scenario ->
                                        title == null
                                                || title.isBlank()
                                                || scenario.getTitle()
                                                        .toLowerCase(java.util.Locale.ROOT)
                                                        .contains(
                                                                title.trim()
                                                                        .toLowerCase(
                                                                                java.util.Locale
                                                                                        .ROOT)))
                        .filter(
                                scenario ->
                                        status == null
                                                || status.isBlank()
                                                || status.equals(scenario.getStatus()))
                        .toList();
        int pageSize = Math.max(10, Math.min(size, 100));
        int totalCount = filteredScenarios.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);
        var scenarios = filteredScenarios.subList(fromIndex, toIndex);
        model.addAttribute("scenarios", scenarios);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("title", title);
        model.addAttribute("status", status);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute(
                "pageNumbers",
                java.util.stream.IntStream.rangeClosed(1, totalPages).boxed().toList());
        model.addAttribute("previewVersionByScenarioId", previewVersionByScenarioId(scenarios));
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
    public String scenarioRegistProc(
            @Valid @ModelAttribute("scenarioForm") ScenarioForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/admin/scenarios");
            model.addAttribute("categories", categoryService.findAll());
            return "admmgr/scenario/scenarioRegist";
        }
        Long id =
                scenarioService
                        .create(
                                new ScenarioDtos.SaveRequest(
                                        form.getCategoryNo(),
                                        form.getTitle(),
                                        form.getDescription(),
                                        null))
                        .getScenarioNo();
        return "redirect:/admin/scenarios/" + id;
    }

    @GetMapping("/{id}")
    public String scenarioView(@PathVariable Long id, Model model) {
        List<ScenarioVersion> versions = scenarioService.versions(id);
        model.addAttribute("scenario", scenarioService.get(id));
        model.addAttribute("versions", versions);
        model.addAttribute("publishabilityByVersionId", publishabilityByVersionId(versions));
        model.addAttribute("keywords", keywordService.findKeywords(id));
        return "admmgr/scenario/scenarioView";
    }

    @GetMapping("/{id}/edit")
    public String scenarioModify(@PathVariable Long id, Model model) {
        var scenario = scenarioService.get(id);
        ScenarioForm form = new ScenarioForm();
        form.setCategoryNo(scenario.getCategoryNo());
        form.setTitle(scenario.getTitle());
        form.setDescription(scenario.getDescription());
        model.addAttribute("scenario", scenario);
        model.addAttribute("scenarioForm", form);
        model.addAttribute("formAction", "/admin/scenarios/" + scenario.getScenarioNo());
        model.addAttribute("categories", categoryService.findAll());
        return "admmgr/scenario/scenarioRegist";
    }

    @PostMapping("/{id}")
    public String scenarioModifyProc(
            @PathVariable Long id,
            @Valid @ModelAttribute("scenarioForm") ScenarioForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("scenario", scenarioService.get(id));
            model.addAttribute("formAction", "/admin/scenarios/" + id);
            model.addAttribute("categories", categoryService.findAll());
            return "admmgr/scenario/scenarioRegist";
        }
        scenarioService.update(
                id,
                new ScenarioDtos.SaveRequest(
                        form.getCategoryNo(), form.getTitle(), form.getDescription(), null));
        return "redirect:/admin/scenarios/" + id;
    }

    @GetMapping("/order")
    public String scenarioOrder(Model model) {
        model.addAttribute("scenarios", scenarioService.activeForOrdering());
        return "admmgr/scenario/scenarioOrder";
    }

    @PostMapping("/{id}/versions")
    public String scenarioVersionRegistProc(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ScenarioVersion existingDraft =
                    scenarioService.versions(id).stream()
                            .filter(version -> "DRAFT".equals(version.getStatus()))
                            .findFirst()
                            .orElse(null);
            ScenarioVersion version = scenarioService.createVersion(id);
            if (existingDraft != null) {
                redirectAttributes.addFlashAttribute(
                        "infoMessage",
                        "이미 편집 중인 초안 v" + existingDraft.getVersionNo() + "이 있습니다. 기존 초안을 편집해 주세요.");
                redirectAttributes.addFlashAttribute(
                        "draftEditUrl",
                        "/admin/scenarios/"
                                + id
                                + "/versions/"
                                + existingDraft.getScenarioVersionNo()
                                + "/graph");
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage", "새 초안 v" + version.getVersionNo() + "을 만들었습니다.");
            }
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", formFailureMessage(e, "새 초안을 만들 수 없습니다."));
        }
        return "redirect:/admin/scenarios/" + id;
    }

    @PostMapping("/{id}/versions/{sourceVersionId}/copy")
    public String scenarioVersionCopyProc(
            @PathVariable Long id,
            @PathVariable Long sourceVersionId,
            RedirectAttributes redirectAttributes) {
        try {
            ScenarioVersion version = scenarioService.createVersionFromSource(id, sourceVersionId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "선택한 버전을 기반으로 새 초안 v" + version.getVersionNo() + "을 만들었습니다.");
            return "redirect:/admin/scenarios/"
                    + id
                    + "/versions/"
                    + version.getScenarioVersionNo()
                    + "/graph";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", formFailureMessage(e, "새 초안을 만들 수 없습니다."));
            return "redirect:/admin/scenarios/" + id;
        }
    }

    @PostMapping("/versions/{versionId}/publish")
    public String scenarioPublishProc(
            @PathVariable Long versionId,
            @RequestParam Long scenarioId,
            RedirectAttributes redirectAttributes) {
        try {
            scenarioService.publish(versionId);
            redirectAttributes.addFlashAttribute("successMessage", "시나리오 버전을 게시했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", publishFailureMessage(e));
        }
        return "redirect:/admin/scenarios/" + scenarioId;
    }

    @PostMapping("/{id}/activate")
    public String scenarioActivateProc(
            @PathVariable Long id,
            @RequestParam Long versionId,
            RedirectAttributes redirectAttributes) {
        try {
            scenarioService.activate(id, versionId);
            redirectAttributes.addFlashAttribute("successMessage", "시나리오를 활성화했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", formFailureMessage(e, "시나리오를 활성화할 수 없습니다."));
        }
        return "redirect:/admin/scenarios/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String scenarioDeactivateProc(
            @PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scenarioService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "시나리오를 비활성화했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", formFailureMessage(e, "시나리오를 비활성화할 수 없습니다."));
        }
        return "redirect:/admin/scenarios/" + id;
    }

    @GetMapping("/{scenarioId}/versions/{versionId}/graph")
    public String scenarioGraphEdit(
            @PathVariable Long scenarioId, @PathVariable Long versionId, Model model) {
        var scenario = scenarioService.get(scenarioId);
        ScenarioVersion version = scenarioService.version(versionId);
        if (!scenarioId.equals(version.getScenarioNo())) {
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

    @GetMapping("/versions/{versionId}/detail")
    public String scenarioVersionDetail(@PathVariable Long versionId, Model model) {
        ScenarioVersion version = scenarioService.version(versionId);
        var scenario = scenarioService.get(version.getScenarioNo());
        model.addAttribute("scenario", scenario);
        model.addAttribute("version", version);
        model.addAttribute("backUrl", "/admin/scenarios/" + scenario.getScenarioNo());
        model.addAttribute("previewUrl", "/admin/scenarios/versions/" + versionId + "/preview");
        model.addAttribute("graphLoadUrl", "/admin/api/scenarios/versions/" + versionId + "/graph");
        return "admmgr/scenario/scenarioVersionDetail";
    }

    @GetMapping("/versions/{versionId}/preview")
    public String scenarioPreviewLayer(
            @PathVariable Long versionId,
            @RequestParam(required = false) Long nodeId,
            Model model) {
        ScenarioVersion version = scenarioService.version(versionId);
        var nodes = scenarioService.nodes(versionId);
        ScenarioNode current =
                nodeId == null && version.getStartNodeNo() != null
                        ? nodes.stream()
                                .filter(
                                        node ->
                                                node.getScenarioNodeNo()
                                                        .equals(version.getStartNodeNo()))
                                .findFirst()
                                .orElse(null)
                        : nodes.stream()
                                .filter(node -> node.getScenarioNodeNo().equals(nodeId))
                                .findFirst()
                                .orElse(null);
        model.addAttribute("version", version);
        model.addAttribute("current", current);
        model.addAttribute(
                "options",
                current == null
                        ? java.util.List.<ScenarioNodeOption>of()
                        : scenarioService.options(current.getScenarioNodeNo()));
        model.addAttribute(
                "links",
                current == null
                        ? java.util.List.of()
                        : scenarioService.links(current.getScenarioNodeNo()));
        model.addAttribute(
                "previewTitle", "DRAFT".equals(version.getStatus()) ? "초안 미리보기" : "게시본 미리보기");
        return "admmgr/scenario/scenarioPreviewLayer";
    }

    private String publishFailureMessage(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.STATE_CONFLICT) {
            return "현재 버전 상태에서는 게시할 수 없습니다. DRAFT 버전인지 확인해 주세요.";
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "게시 조건을 확인해 주세요.";
        }
        if (message.contains("저장된 그래프")) {
            return "저장된 그래프가 없습니다. 그래프를 저장한 뒤 게시해 주세요.";
        }
        if (message.contains("시작")) {
            return "시작 노드를 지정한 뒤 게시해 주세요.";
        }
        if (message.contains("END")) {
            return "END 노드의 옵션을 제거한 뒤 게시해 주세요.";
        }
        if (message.contains("nextNodeKey") || message.contains("옵션") || message.contains("연결")) {
            return "옵션 연결을 확인한 뒤 게시해 주세요.";
        }
        return message;
    }

    private String formFailureMessage(BusinessException e, String fallback) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    private Map<Long, ScenarioDtos.Publishability> publishabilityByVersionId(
            List<ScenarioVersion> versions) {
        return versions.stream()
                .filter(version -> "DRAFT".equals(version.getStatus()))
                .collect(
                        Collectors.toMap(
                                ScenarioVersion::getScenarioVersionNo,
                                scenarioService::publishability));
    }

    private Map<Long, Long> previewVersionByScenarioId(
            List<kr.co.cleverchat.domain.scenario.model.Scenario> scenarios) {
        return scenarios.stream()
                .map(
                        scenario -> {
                            Long versionId = scenario.getActiveVersionNo();
                            if (versionId == null) {
                                versionId =
                                        scenarioService.versions(scenario.getScenarioNo()).stream()
                                                .findFirst()
                                                .map(ScenarioVersion::getScenarioVersionNo)
                                                .orElse(null);
                            }
                            return versionId == null
                                    ? null
                                    : Map.entry(scenario.getScenarioNo(), versionId);
                        })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static class ScenarioForm {
        @NotNull private Long categoryNo;

        @NotBlank
        @Size(max = 150)
        private String title;

        @Size(max = 2000)
        private String description;

        public Long getCategoryNo() {
            return categoryNo;
        }

        public void setCategoryNo(Long categoryNo) {
            this.categoryNo = categoryNo;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
