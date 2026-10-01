package kr.co.cleverchat.domain.crawl.controller;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.model.CrawlRunLog;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdmCrawlController {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private kr.co.cleverchat.domain.settings.RuntimeSettingsService runtime;

    @ModelAttribute("runRetentionDays")
    public int runRetentionDays() {
        return runtime == null
                ? 365
                : runtime.current()
                        .integer(
                                kr.co.cleverchat.domain.settings.RuntimeSetting.RUN_RETENTION_DAYS);
    }

    @ModelAttribute("documentRetentionDays")
    public int documentRetentionDays() {
        return runtime == null
                ? 90
                : runtime.current()
                        .integer(
                                kr.co.cleverchat.domain.settings.RuntimeSetting
                                        .DOCUMENT_RETENTION_DAYS);
    }

    private final CrawlService crawlService;

    public AdmCrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/admin/crawl-targets")
    public String targets(@RequestParam(required = false) Boolean enabled, Model model) {
        List<CrawlTarget> targets = crawlService.targets(enabled);
        Map<Long, List<CrawlRunLog>> recentRuns = new LinkedHashMap<>();
        for (CrawlTarget target : targets) {
            recentRuns.put(
                    target.getCrawlTargetNo(),
                    crawlService.runLogs(target.getCrawlTargetNo(), null, null, 5));
        }
        model.addAttribute("targets", targets);
        model.addAttribute("recentRuns", recentRuns);
        model.addAttribute("enabled", enabled);
        return "admmgr/crawl/targetList";
    }

    @GetMapping("/admin/crawl-targets/new")
    public String newTarget(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("target", new CrawlTarget());
        model.addAttribute("formAction", "/admin/crawl-targets");
        return "admmgr/crawl/targetForm";
    }

    @PostMapping("/admin/crawl-targets")
    public String createTarget(
            @Valid @ModelAttribute TargetRequest request,
            BindingResult errors,
            @CurrentUser AdminSession adminSession,
            RedirectAttributes redirectAttributes,
            Model model) {
        return saveTarget(
                null,
                request,
                errors,
                adminSession == null ? null : adminSession.getId(),
                redirectAttributes,
                model);
    }

    public String targetView(@PathVariable Long id, Model model) {
        return targetView(id, 1, "", model);
    }

    @ModelAttribute("defaultExportDirectory")
    public String defaultExportDirectory() {
        return crawlService.defaultExportDirectory();
    }

    @GetMapping("/admin/crawl-targets/{id}")
    public String targetView(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int docPage,
            @RequestParam(defaultValue = "") String q,
            Model model) {
        CrawlTarget target = crawlService.target(id);
        model.addAttribute("target", target);
        model.addAttribute("runs", crawlService.runLogs(id, null, null, 20));
        var jobs = crawlService.jobs(id, 20);
        var latest = jobs.isEmpty() ? null : jobs.get(0);
        boolean active =
                latest != null
                        && ("PENDING".equals(latest.getStatus())
                                || "RUNNING".equals(latest.getStatus()));
        model.addAttribute("jobs", jobs);
        model.addAttribute("latestJob", latest);
        model.addAttribute("jobActive", active);
        model.addAttribute("coverages", crawlService.coverages(id, 20));
        String query = q.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        long total = crawlService.documentCount(id, query);
        int pages = (int) Math.max(1, Math.min(100000, (total + 19) / 20));
        int page = Math.max(1, Math.min(docPage, pages));
        model.addAttribute(
                "documentCount", query.isEmpty() ? total : crawlService.documentCount(id, ""));
        model.addAttribute("filteredCount", total);
        model.addAttribute("docPage", page);
        model.addAttribute("docPages", pages);
        model.addAttribute("q", query);
        model.addAttribute("documents", crawlService.documentPage(id, query, page));
        model.addAttribute("effectiveExportDirectory", crawlService.exportDirectory(target));
        return "admmgr/crawl/targetView";
    }

    @GetMapping("/admin/crawl-targets/{id}/edit")
    public String editTarget(@PathVariable Long id, Model model) {
        model.addAttribute("mode", "edit");
        model.addAttribute("target", crawlService.target(id));
        model.addAttribute("formAction", "/admin/crawl-targets/" + id);
        return "admmgr/crawl/targetForm";
    }

    @PostMapping("/admin/crawl-targets/{id}")
    public String updateTarget(
            @PathVariable Long id,
            @Valid @ModelAttribute TargetRequest request,
            BindingResult errors,
            RedirectAttributes redirectAttributes,
            Model model) {
        return saveTarget(id, request, errors, null, redirectAttributes, model);
    }

    private String saveTarget(
            Long id,
            TargetRequest request,
            BindingResult errors,
            Long adminId,
            RedirectAttributes redirectAttributes,
            Model model) {
        TargetRequest normalized =
                new TargetRequest(
                        request.url(),
                        request.label(),
                        "Y".equals(request.useYn()) ? "Y" : "N",
                        Boolean.TRUE.equals(request.scheduleEnabled()),
                        request.scheduleIntervalMinutes(),
                        request.scheduleMode(),
                        request.scheduleCron(),
                        Boolean.TRUE.equals(request.jsonExportEnabled()),
                        request.jsonExportDirectory());
        try {
            if (errors.hasErrors()) {
                throw new IllegalArgumentException(
                        "입력값을 확인해 주세요: " + errors.getAllErrors().get(0).getDefaultMessage());
            }
            CrawlTarget saved =
                    id == null
                            ? crawlService.createTarget(normalized, adminId)
                            : crawlService.updateTarget(id, normalized);
            redirectAttributes.addFlashAttribute(
                    "successMessage", id == null ? "크롤링 대상이 등록되었습니다." : "크롤링 대상이 수정되었습니다.");
            return "redirect:/admin/crawl-targets/" + (id == null ? saved.getCrawlTargetNo() : id);
        } catch (BusinessException | IllegalArgumentException e) {
            model.addAttribute("mode", id == null ? "create" : "edit");
            model.addAttribute("target", formTarget(id, normalized));
            model.addAttribute(
                    "formAction",
                    id == null ? "/admin/crawl-targets" : "/admin/crawl-targets/" + id);
            model.addAttribute("errorMessage", e.getMessage());
            return "admmgr/crawl/targetForm";
        }
    }

    @PostMapping("/admin/crawl-targets/{id}/run")
    public String runTarget(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            var response = crawlService.run(id);
            String message =
                    response.enqueued() ? "크롤링 작업이 접수되었습니다." : "이미 대기 또는 실행 중인 크롤링 작업이 있습니다.";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/crawl-targets/" + id;
    }

    @GetMapping("/admin/crawl-documents")
    public String documents(
            @RequestParam(required = false) Long targetId, RedirectAttributes redirectAttributes) {
        if (targetId != null && targetId > 0) {
            return "redirect:/admin/crawl-targets/" + targetId + "#crawl-documents";
        }
        return "redirect:/admin/crawl-targets";
    }

    @GetMapping("/admin/crawl-runs")
    public String runs(
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String failureCode,
            RedirectAttributes redirectAttributes) {
        if (targetId != null && targetId > 0) {
            return "redirect:/admin/crawl-targets/" + targetId + "#crawl-runs";
        }
        return "redirect:/admin/crawl-targets";
    }

    private CrawlTarget formTarget(Long id, TargetRequest request) {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(id);
        target.setUrl(request.url());
        target.setLabel(request.label());
        target.setUseYn(request.useYn());
        target.setScheduleEnabled(Boolean.TRUE.equals(request.scheduleEnabled()));
        target.setScheduleIntervalMinutes(request.scheduleIntervalMinutes());
        target.setScheduleMode(request.scheduleMode());
        target.setScheduleCron(request.scheduleCron());
        target.setJsonExportEnabled(Boolean.TRUE.equals(request.jsonExportEnabled()));
        target.setJsonExportDirectory(request.jsonExportDirectory());
        return target;
    }
}
