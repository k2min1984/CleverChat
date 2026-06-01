package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import kr.co.cleverchat.domain.auth.service.LoginAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock private UserMapper userMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private LoginAuditService loginAuditService;

    @Mock private OpsEventLogger opsEventLogger;

    private CsrfTokenIssuer csrfTokenIssuer;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        csrfTokenIssuer = new CsrfTokenIssuer();
        controller =
                new LoginController(
                        userMapper,
                        passwordEncoder,
                        loginAuditService,
                        csrfTokenIssuer,
                        opsEventLogger);
    }

    @Test
    void loginPageRedirectsWhenAlreadyAuthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSession.SESSION_KEY, adminSession());
        request.setSession(session);

        String view = controller.login(request);

        assertThat(view).isEqualTo("redirect:/admin");
    }

    @Test
    void loginPageReturnsLoginViewWithoutSession() {
        String view = controller.login(new MockHttpServletRequest());

        assertThat(view).isEqualTo("login");
    }

    @Test
    void authenticateCreatesAdminSessionOnSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserAccount account = account();
        when(userMapper.findByUsername("admin")).thenReturn(account);
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        String view = controller.authenticate("admin", "password", request);

        assertThat(view).isEqualTo("redirect:/admin");
        assertThat(request.getSession(false).getAttribute(AdminSession.SESSION_KEY))
                .isInstanceOfSatisfying(
                        AdminSession.class,
                        session -> {
                            assertThat(session.getUsername()).isEqualTo("admin");
                            assertThat(session.getDisplayName()).isEqualTo("관리자");
                            assertThat(session.hasRole("ADMIN")).isTrue();
                            assertThat(session.isMustChangePassword()).isTrue();
                        });
        assertThat(
                        (String)
                                request.getSession(false)
                                        .getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE))
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
        assertThat(
                        (String)
                                request.getSession(false)
                                        .getAttribute(
                                                CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE))
                .hasSize(22)
                .matches("[A-Za-z0-9_-]+");
        verify(loginAuditService).recordSuccess("admin", request);
    }

    @Test
    void authenticateRegeneratesExistingSessionIdWithoutInvalidatingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession existingSession = new MockHttpSession();
        String oldSessionId = existingSession.getId();
        existingSession.setAttribute(AdminSession.SESSION_KEY, adminSession());
        existingSession.setAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE, "old-token");
        existingSession.setAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE, "old-form-id");
        request.setSession(existingSession);
        UserAccount account = account();
        when(userMapper.findByUsername("admin")).thenReturn(account);
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);

        String view = controller.authenticate("admin", "password", request);

        assertThat(view).isEqualTo("redirect:/admin");
        assertThat(existingSession.isInvalid()).isFalse();
        assertThat(request.getSession(false)).isSameAs(existingSession);
        assertThat(existingSession.getId()).isNotEqualTo(oldSessionId);
        assertThat(existingSession.getAttribute(AdminSession.SESSION_KEY))
                .isInstanceOf(AdminSession.class);
        assertThat(
                        (String)
                                existingSession.getAttribute(
                                        CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE))
                .isNotEqualTo("old-token")
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
        assertThat(
                        (String)
                                existingSession.getAttribute(
                                        CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE))
                .isNotEqualTo("old-form-id")
                .hasSize(22)
                .matches("[A-Za-z0-9_-]+");
    }

    @Test
    void authenticateRecordsFailureForMissingUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(userMapper.findByUsername("missing")).thenReturn(null);

        String view = controller.authenticate("missing", "password", request);

        assertThat(view).isEqualTo("redirect:/login?error");
        assertThat(request.getSession(false)).isNull();
        verify(loginAuditService).recordFailure("missing", "User not found.", request);
        verify(passwordEncoder, never()).matches("password", "hash");
    }

    @Test
    void authenticateRecordsFailureForLockedUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserAccount account = account();
        account.setLockedUntil(OffsetDateTime.now().plusMinutes(5));
        when(userMapper.findByUsername("admin")).thenReturn(account);

        String view = controller.authenticate("admin", "password", request);

        assertThat(view).isEqualTo("redirect:/login?error");
        verify(loginAuditService).recordFailure("admin", "Account is locked.", request);
        verify(passwordEncoder, never()).matches("password", "hash");
    }

    @Test
    void authenticateRecordsFailureForInvalidPassword() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserAccount account = account();
        when(userMapper.findByUsername("admin")).thenReturn(account);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        String view = controller.authenticate("admin", "wrong", request);

        assertThat(view).isEqualTo("redirect:/login?error");
        verify(loginAuditService).recordFailure("admin", "Password does not match.", request);
    }

    @Test
    void logoutInvalidatesSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        String view = controller.logout(request);

        assertThat(view).isEqualTo("redirect:/login?logout");
        assertThat(session.isInvalid()).isTrue();
    }

    private UserAccount account() {
        UserAccount account = new UserAccount();
        account.setUserNo(1L);
        account.setUsername("admin");
        account.setDisplayName("관리자");
        account.setPasswordHash("hash");
        account.setUseYn("Y");
        account.setMustChangePassword(true);
        account.setRoles(List.of("ADMIN"));
        return account;
    }

    private AdminSession adminSession() {
        return new AdminSession(1L, "admin", "관리자", Set.of("ADMIN"), false, LocalDateTime.now());
    }
}
