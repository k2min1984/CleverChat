package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.co.cleverchat.domain.auth.service.LoginAuditService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAuditService loginAuditService;

    public LoginSuccessHandler(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
        setDefaultTargetUrl("/admin");
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        loginAuditService.recordSuccess(authentication.getName(), request);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
