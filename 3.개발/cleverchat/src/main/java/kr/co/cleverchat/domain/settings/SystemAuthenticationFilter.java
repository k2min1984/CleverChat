package kr.co.cleverchat.domain.settings;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CsrfTokenIssuer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(15)
public class SystemAuthenticationFilter extends OncePerRequestFilter {
    public static final String PROVIDER = "CLEVERCHAT_IDENTITY_PROVIDER";
    private static final String POLICY_VERSION = "CLEVERCHAT_AUTH_VERSION";
    private final SystemSettingsService settings;
    private final GatewayIdentityService identities;
    private final GatewayUserMapper users;
    private final CsrfTokenIssuer csrf;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    public SystemAuthenticationFilter(
            SystemSettingsService settings,
            GatewayIdentityService identities,
            GatewayUserMapper users,
            CsrfTokenIssuer csrf) {
        this.settings = settings;
        this.identities = identities;
        this.users = users;
        this.csrf = csrf;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/asset/")
                || path.startsWith("/actuator/")
                || path.startsWith("/external/")
                || path.equals("/error")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SystemSettings config = settings.current();
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object version = session.getAttribute(POLICY_VERSION);
            Object provider = session.getAttribute(PROVIDER);
            if ((version != null && !Objects.equals(version, config.authVersion()))
                    || (provider != null && !config.operationMode().equals(provider))
                    || (config.isLinked() && !"GATEWAY".equals(provider))) {
                session.invalidate();
                session = null;
            }
        }
        if (!config.isLinked()) {
            if (session != null
                    || (request.getServletPath().equals("/login")
                            && request.getMethod().equals("POST"))) {
                session = request.getSession(true);
                session.setAttribute(POLICY_VERSION, config.authVersion());
                session.setAttribute(PROVIDER, "LOCAL");
                applySessionTimeout(session);
            }
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (request.getHeader("X-Clever-Gw-Secret") == null
                && path.equals("/login")
                && request.getMethod().equals("GET")) {
            request.setAttribute("gatewayLoginUrl", config.gatewayUrl());
            request.getRequestDispatcher("/gateway-login").forward(request, response);
            return;
        }
        try {
            GatewayIdentityService.Identity identity =
                    identities.verify(request, config.serviceId());
            GatewayUserMapper.Account account =
                    users.provision(identity.empNo(), identity.name(), identity.unit());
            if (!"Y".equals(account.useYn())) throw new IllegalArgumentException("사용 중지된 계정입니다.");
            AdminSession previous =
                    session == null
                            ? null
                            : (AdminSession) session.getAttribute(AdminSession.SESSION_KEY);
            if (previous == null || !account.userNo().equals(previous.getId())) {
                if (session != null) session.invalidate();
                session = request.getSession(true);
                request.changeSessionId();
                csrf.issue(session);
                previous = null;
            }
            session.setAttribute(
                    AdminSession.SESSION_KEY,
                    new AdminSession(
                            account.userNo(),
                            account.empNo(),
                            account.displayName(),
                            identity.roles(),
                            false,
                            previous == null ? LocalDateTime.now() : previous.getLoginAt()));
            session.setAttribute(POLICY_VERSION, config.authVersion());
            session.setAttribute(PROVIDER, "GATEWAY");
            applySessionTimeout(session);
        } catch (IllegalArgumentException e) {
            if (session != null) session.invalidate();
            if (request.getMethod().equals("GET")
                    && !path.contains("/api/")
                    && request.getHeader("X-Clever-Gw-Secret") == null) {
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter()
                        .write(
                                "{\"success\":false,\"error\":{\"code\":\"SSO_REQUIRED\",\"message\":\"게이트웨이 로그인이 필요합니다.\"}}");
            }
            return;
        }
        // In linked mode never invoke the local password verifier, even with valid SSO headers.
        if (path.equals("/login")) {
            AdminSession admin =
                    (AdminSession) request.getSession().getAttribute(AdminSession.SESSION_KEY);
            response.sendRedirect(
                    request.getContextPath()
                            + (admin.hasAnyRole("ADMIN", "OPERATOR") ? "/admin" : "/chat"));
            return;
        }
        if (path.equals("/logout") && request.getMethod().equals("POST")) {
            // Existing CSRF-protected logout controller handles local session cleanup.
            request.setAttribute("gatewayLogoutUrl", config.gatewayUrl());
        }
        chain.doFilter(request, response);
    }

    private void applySessionTimeout(HttpSession session) {
        if (runtimeSettings != null)
            session.setMaxInactiveInterval(
                    runtimeSettings.current().integer(RuntimeSetting.SESSION_MINUTES) * 60);
    }
}
