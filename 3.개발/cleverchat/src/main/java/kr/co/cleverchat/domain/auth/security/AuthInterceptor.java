package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AdminSession.SESSION_KEY) instanceof AdminSession) {
            return true;
        }

        AdminSession bridgedSession = bridgeFromSecurityContext();
        if (bridgedSession != null) {
            request.getSession(true).setAttribute(AdminSession.SESSION_KEY, bridgedSession);
            return true;
        }

        if (isAdminApiRequest(request)) {
            writeUnauthorized(response);
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private AdminSession bridgeFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }

        Set<String> roles = user.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new AdminSession(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            roles,
            user.isMustChangePassword(),
            LocalDateTime.now()
        );
    }

    private boolean isAdminApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/admin/api/");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}}");
    }
}
