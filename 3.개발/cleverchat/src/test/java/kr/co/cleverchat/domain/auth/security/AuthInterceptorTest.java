package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    @Test
    void allowsRequestWithAdminSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSession.SESSION_KEY, adminSession());
        request.setSession(session);

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void redirectsAdminPageRequestWithoutSessionToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    void returnsUnauthorizedJsonForAdminApiRequestWithoutSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED", "인증이 필요합니다.");
    }

    private AdminSession adminSession() {
        return new AdminSession(1L, "admin", "관리자", Set.of("ADMIN"), false, LocalDateTime.now());
    }
}
