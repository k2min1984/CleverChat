package kr.co.cleverchat.domain.adminmanage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.adminmanage.service.AdminIpWhitelistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(5)
public class AdminIpWhitelistFilter extends OncePerRequestFilter {

    private final AdminIpWhitelistService service;
    private final OpsEventLogger opsEventLogger;
    private final boolean enabled;

    public AdminIpWhitelistFilter(
            AdminIpWhitelistService service,
            OpsEventLogger opsEventLogger,
            @Value("${cleverchat.admin.ip-whitelist.enabled:true}") boolean enabled) {
        this.service = service;
        this.opsEventLogger = opsEventLogger;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return !enabled || (!"/admin".equals(path) && !path.startsWith("/admin/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        if (service.isAllowed(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }
        opsEventLogger.securityEvent(
                "ADMIN_IP_BLOCKED",
                request,
                null,
                Map.of("path", requestPath(request), "clientIp", clientIp));
        if (requestPath(request).startsWith("/admin/api/")) {
            writeForbiddenJson(response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private String requestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath == null || contextPath.isBlank() || !uri.startsWith(contextPath)) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }

    private void writeForbiddenJson(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter()
                .write(
                        "{\"success\":false,\"error\":{\"code\":\"ACCESS_DENIED\",\"message\":\"관리자 접근이 허용되지 않은 IP입니다.\"}}");
    }
}
