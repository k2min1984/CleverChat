package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.co.cleverchat.domain.auth.service.LoginAuditService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAuditService loginAuditService;

    public LoginFailureHandler(LoginAuditService loginAuditService) {
        this.loginAuditService = loginAuditService;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        String username = request.getParameter("username");
        loginAuditService.recordFailure(username == null ? "" : username, exception.getMessage(), request);
        super.onAuthenticationFailure(request, response, exception);
    }
}
