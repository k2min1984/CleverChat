package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import kr.co.cleverchat.domain.auth.service.LoginAuditService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class LoginController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginAuditService loginAuditService;
    private final CsrfTokenIssuer csrfTokenIssuer;

    public LoginController(
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        LoginAuditService loginAuditService,
        CsrfTokenIssuer csrfTokenIssuer
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginAuditService = loginAuditService;
        this.csrfTokenIssuer = csrfTokenIssuer;
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
        HttpServletRequest request
    ) {
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            loginAuditService.recordFailure(username, "사용자를 찾을 수 없습니다.", request);
            return "redirect:/login?error";
        }
        if (!account.isEnabled()) {
            loginAuditService.recordFailure(username, "비활성 계정입니다.", request);
            return "redirect:/login?error";
        }
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(OffsetDateTime.now())) {
            loginAuditService.recordFailure(username, "계정이 잠겨 있습니다.", request);
            return "redirect:/login?error";
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            loginAuditService.recordFailure(username, "비밀번호가 일치하지 않습니다.", request);
            return "redirect:/login?error";
        }

        loginAuditService.recordSuccess(username, request);
        AuthenticatedUser user = new AuthenticatedUser(account);
        HttpSession session = request.getSession(true);
        session.setAttribute(AdminSession.SESSION_KEY, new AdminSession(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getRoles(),
            user.isMustChangePassword(),
            LocalDateTime.now()
        ));
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
