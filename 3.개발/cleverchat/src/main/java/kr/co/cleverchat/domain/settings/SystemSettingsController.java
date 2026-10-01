package kr.co.cleverchat.domain.settings;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.cleverchat.domain.auth.security.CurrentAdminProvider;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/system-settings")
public class SystemSettingsController {
    private final SystemSettingsService service;
    private final RuntimeSettingsService runtime;
    private final MaintenanceMapper maintenance;
    private final kr.co.cleverchat.domain.chatbot.ai.VllmAiAnswerProvider ai;

    public SystemSettingsController(
            SystemSettingsService service,
            RuntimeSettingsService runtime,
            MaintenanceMapper maintenance,
            kr.co.cleverchat.domain.chatbot.ai.VllmAiAnswerProvider ai) {
        this.service = service;
        this.runtime = runtime;
        this.maintenance = maintenance;
        this.ai = ai;
    }

    @ModelAttribute
    public void commonModel(Model model, HttpServletRequest request) {
        model.addAttribute("settings", service.current());
        model.addAttribute("gatewayConfigured", service.isGatewayConfigured());
        model.addAttribute("runtimeValues", runtime.current().values());
        var fields = new java.util.LinkedHashMap<String, java.util.List<RuntimeSetting>>();
        for (String section :
                java.util.List.of("crawl", "search", "chat", "login", "retention", "ai"))
            fields.put(section, runtime.fields(section));
        model.addAttribute("runtimeFields", fields);
        model.addAttribute("aiKeyConfigured", ai.isKeyConfigured());
        model.addAttribute("lastMaintenance", maintenance.lastResult());
        model.addAttribute(
                "deploymentPath",
                request.getContextPath().isBlank() ? "/" : request.getContextPath() + "/");
        var session = request.getSession(false);
        model.addAttribute(
                "identityProvider",
                session == null
                        ? "LOCAL"
                        : session.getAttribute(SystemAuthenticationFilter.PROVIDER));
        model.addAttribute("activeTab", "connection");
    }

    @PostMapping("/runtime/{section}")
    @RequireRole("ADMIN")
    public String saveRuntime(
            @PathVariable String section,
            @RequestParam java.util.Map<String, String> values,
            @RequestParam long version,
            Model model,
            HttpServletRequest request,
            RedirectAttributes flash) {
        String tab =
                switch (section) {
                    case "login" -> "connection";
                    case "retention" -> "storage";
                    case "search", "chat" -> "search";
                    default -> section;
                };
        try {
            runtime.save(section, values, version, CurrentAdminProvider.currentUsername(request));
            flash.addFlashAttribute(
                    "successMessage", "설정을 저장했습니다. 크롤링은 다음 작업부터, 나머지는 다음 요청부터 적용합니다.");
            return "redirect:/admin/system-settings#" + tab;
        } catch (IllegalArgumentException e) {
            model.addAttribute("activeTab", tab);
            model.addAttribute("errorMessage", e.getMessage());
            var submitted = new java.util.LinkedHashMap<String, Object>(runtime.current().values());
            for (RuntimeSetting field : runtime.fields(section))
                if (values.containsKey(field.name()))
                    submitted.put(field.name(), values.get(field.name()));
            model.addAttribute("runtimeValues", submitted);
            return "admmgr/settings/systemSettings";
        }
    }

    @PostMapping("/ai/test")
    @RequireRole("ADMIN")
    public String testAi(RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("successMessage", ai.checkConnection());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/system-settings#ai";
    }

    @GetMapping
    @RequireRole("ADMIN")
    public String page(Model model) {
        model.addAttribute("settings", service.current());
        model.addAttribute("gatewayConfigured", service.isGatewayConfigured());
        return "admmgr/settings/systemSettings";
    }

    @PostMapping
    @RequireRole("ADMIN")
    public String save(
            @ModelAttribute SettingsForm form,
            org.springframework.validation.BindingResult errors,
            Model model,
            HttpServletRequest request,
            RedirectAttributes flash) {
        SystemSettings settings =
                new SystemSettings(
                        form.operationMode(),
                        form.gatewayUrl(),
                        form.serviceId(),
                        form.crawlExportDirectory(),
                        form.version() == null ? 0 : form.version(),
                        0,
                        null,
                        null);
        try {
            if (errors.hasErrors() || form.version() == null || form.version() < 0)
                throw new IllegalArgumentException("입력값을 확인하거나 화면을 새로고침해 주세요.");
            SystemSettings previous = service.current();
            SystemSettings saved =
                    service.save(settings, CurrentAdminProvider.currentUsername(request));
            if (previous.authVersion() != saved.authVersion()) {
                request.getSession().invalidate();
                return "redirect:/login";
            }
            flash.addFlashAttribute("successMessage", "시스템 설정을 저장했습니다. 새로 저장하는 파일부터 적용됩니다.");
            String tab = "storage".equals(request.getParameter("tab")) ? "storage" : "connection";
            return "redirect:/admin/system-settings"
                    + (request.getParameter("tab") == null ? "" : "#" + tab);
        } catch (IllegalArgumentException e) {
            model.addAttribute("settings", settings);
            model.addAttribute("gatewayConfigured", service.isGatewayConfigured());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute(
                    "activeTab",
                    "storage".equals(request.getParameter("tab")) ? "storage" : "connection");
            return "admmgr/settings/systemSettings";
        }
    }

    public record SettingsForm(
            String operationMode,
            String gatewayUrl,
            String serviceId,
            String crawlExportDirectory,
            Long version) {}
}
