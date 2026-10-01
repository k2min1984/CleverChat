package kr.co.cleverchat.domain.adminmanage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.domain.adminmanage.model.AdminMenu;
import kr.co.cleverchat.domain.adminmanage.service.AdminManageService;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "kr.co.cleverchat")
public class AdminNavigationAdvice {

    private final AdminManageService adminManageService;

    public AdminNavigationAdvice(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @ModelAttribute("adminSidebarMenus")
    public List<AdminMenu> adminSidebarMenus(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/admin") || path.startsWith("/admin/api")) {
            return List.of();
        }
        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(AdminSession.SESSION_KEY)
                        instanceof AdminSession admin)) {
            return List.of();
        }
        return adminManageService.sidebarMenus(admin);
    }

    @ModelAttribute("adminCommonCodes")
    public Map<String, List<Map<String, Object>>> adminCommonCodes(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/admin") || path.startsWith("/admin/api")) {
            return Map.of();
        }
        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(AdminSession.SESSION_KEY) instanceof AdminSession)) {
            return Map.of();
        }
        return adminManageService.activeCodeOptions();
    }
}
