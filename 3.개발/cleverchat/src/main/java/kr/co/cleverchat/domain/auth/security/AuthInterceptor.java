package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.adminmanage.service.AdminManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final OpsEventLogger opsEventLogger;
    private final AdminManageService adminManageService;

    @Autowired
    public AuthInterceptor(OpsEventLogger opsEventLogger, AdminManageService adminManageService) {
        this.opsEventLogger = opsEventLogger;
        this.adminManageService = adminManageService;
    }

    AuthInterceptor(OpsEventLogger opsEventLogger) {
        this(opsEventLogger, null);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null
                && session.getAttribute(AdminSession.SESSION_KEY) instanceof AdminSession admin) {
            if (canAccessMenu(request, admin)) {
                return true;
            }
            opsEventLogger.securityEvent(
                    "AUTH_FORBIDDEN_MENU",
                    request,
                    admin.getUsername(),
                    Map.of("path", requestPath(request)));
            if (isAdminApiRequest(request)) {
                writeForbidden(response);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return false;
        }

        if (isAdminApiRequest(request)) {
            opsEventLogger.securityEvent(
                    "AUTH_UNAUTHORIZED", request, null, Map.of("response", "json"));
            writeUnauthorized(response);
            return false;
        }

        opsEventLogger.securityEvent(
                "AUTH_UNAUTHORIZED", request, null, Map.of("response", "redirect"));
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private boolean isAdminApiRequest(HttpServletRequest request) {
        return requestPath(request).startsWith("/admin/api/");
    }

    private boolean canAccessMenu(HttpServletRequest request, AdminSession admin) {
        String path = requestPath(request);
        if (!path.startsWith("/admin") || path.startsWith("/admin/api/")) {
            return true;
        }
        if (adminManageService == null) {
            return true;
        }
        return adminManageService.canAccessPath(admin, path);
    }

    private String requestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"\uC778\uC99D\uC774 \uD544\uC694\uD569\uB2C8\uB2E4.\"}}");
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        "{\"success\":false,\"error\":{\"code\":\"ACCESS_DENIED\",\"message\":\"\uC811\uADFC \uAD8C\uD55C\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.\"}}");
    }
}
