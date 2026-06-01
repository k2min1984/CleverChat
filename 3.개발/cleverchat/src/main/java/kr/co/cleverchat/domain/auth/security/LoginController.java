package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import kr.co.cleverchat.domain.auth.service.LoginAuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAuditService loginAuditService;
    private final CsrfTokenIssuer csrfTokenIssuer;
    private final OpsEventLogger opsEventLogger;

    public LoginController(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            LoginAuditService loginAuditService,
            CsrfTokenIssuer csrfTokenIssuer,
            OpsEventLogger opsEventLogger) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAuditService = loginAuditService;
        this.csrfTokenIssuer = csrfTokenIssuer;
        this.opsEventLogger = opsEventLogger;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (CurrentAdminProvider.current(request) != null) {
            return "redirect:/admin";
        }
        return "login";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request) {
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            opsEventLogger.securityEvent(
                    "LOGIN_FAILED", request, username, Map.of("reason", "missing_user"));
            loginAuditService.recordFailure(username, "User not found.", request);
            return "redirect:/login?error";
        }
        if (!"Y".equals(account.getUseYn())) {
            opsEventLogger.securityEvent(
                    "LOGIN_FAILED", request, username, Map.of("reason", "disabled"));
            loginAuditService.recordFailure(username, "Account is disabled.", request);
            return "redirect:/login?error";
        }
        if (account.getLockedUntil() != null
                && account.getLockedUntil().isAfter(OffsetDateTime.now())) {
            opsEventLogger.securityEvent(
                    "LOGIN_LOCKED", request, username, Map.of("reason", "locked_until_active"));
            loginAuditService.recordFailure(username, "Account is locked.", request);
            return "redirect:/login?error";
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            opsEventLogger.securityEvent(
                    "LOGIN_FAILED", request, username, Map.of("reason", "bad_credentials"));
            loginAuditService.recordFailure(username, "Password does not match.", request);
            return "redirect:/login?error";
        }

        opsEventLogger.securityEvent(
                "LOGIN_SUCCEEDED", request, username, Map.of("userId", account.getUserNo()));
        loginAuditService.recordSuccess(username, request);
        AuthenticatedUser user = new AuthenticatedUser(account);
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(
                AdminSession.SESSION_KEY,
                new AdminSession(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getRoles(),
                        user.isMustChangePassword(),
                        LocalDateTime.now()));
        csrfTokenIssuer.issue(session);
        return "redirect:/admin";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout";
    }
}
