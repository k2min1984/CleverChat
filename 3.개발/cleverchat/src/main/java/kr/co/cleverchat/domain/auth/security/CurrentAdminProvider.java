package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CurrentAdminProvider {

    private CurrentAdminProvider() {
    }

    public static AdminSession current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return current(servletRequestAttributes.getRequest());
    }

    public static AdminSession current(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object adminSession = session.getAttribute(AdminSession.SESSION_KEY);
        return adminSession instanceof AdminSession currentAdmin ? currentAdmin : null;
    }

    public static String currentUsername() {
        AdminSession currentAdmin = current();
        return currentAdmin == null ? null : currentAdmin.getUsername();
    }

    public static String currentUsername(HttpServletRequest request) {
        AdminSession currentAdmin = current(request);
        return currentAdmin == null ? null : currentAdmin.getUsername();
    }
}
