package kr.co.cleverchat.domain.adminmanage.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.adminmanage.service.AdminIpWhitelistService;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdmAdminIpWhitelistController {

    private final AdminIpWhitelistService service;

    public AdmAdminIpWhitelistController(AdminIpWhitelistService service) {
        this.service = service;
    }

    @GetMapping("/admin/manage/ip-whitelist")
    public String list(Model model, HttpServletRequest request) {
        model.addAttribute("entries", service.entries());
        model.addAttribute("currentIp", request.getRemoteAddr());
        return "admmgr/manage/ipWhitelist";
    }

    @PostMapping("/admin/manage/ip-whitelist")
    public String add(
            @RequestParam String ipCidr,
            @RequestParam(required = false) String description,
            @CurrentUser AdminSession adminSession,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            service.addEntry(ipCidr, description, actor(adminSession), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "IP 허용 항목이 추가되었습니다.");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage/ip-whitelist";
    }

    @PostMapping("/admin/manage/ip-whitelist/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String ipCidr,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "Y") String useYn,
            @CurrentUser AdminSession adminSession,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            service.updateEntry(
                    id, ipCidr, description, useYn, actor(adminSession), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "IP 허용 항목이 수정되었습니다.");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage/ip-whitelist";
    }

    @PostMapping("/admin/manage/ip-whitelist/{id}/disable")
    public String disable(
            @PathVariable Long id,
            @CurrentUser AdminSession adminSession,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            service.disableEntry(id, actor(adminSession), request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("successMessage", "IP 허용 항목이 비활성화되었습니다.");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/manage/ip-whitelist";
    }

    private String actor(AdminSession adminSession) {
        return adminSession == null ? null : adminSession.getUsername();
    }
}
