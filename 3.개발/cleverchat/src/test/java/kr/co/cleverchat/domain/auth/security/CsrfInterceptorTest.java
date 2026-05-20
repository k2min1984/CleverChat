package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class CsrfInterceptorTest {

    private final CsrfTokenIssuer csrfTokenIssuer = new CsrfTokenIssuer();
    private final CsrfInterceptor interceptor = new CsrfInterceptor(csrfTokenIssuer);

    @Test
    void allowsGetRequestWithoutCsrfValidation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsPostWithMatchingCsrfAndReissuesSessionAttributesAndHeaders() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.setParameter("csrfToken", token);
        request.setParameter("csrfFormId", formId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isTrue();
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isNotEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isNotEqualTo(formId);
        assertThat(response.getHeader("X-CSRF-Token"))
            .isEqualTo(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE));
        assertThat(response.getHeader("X-CSRF-FormId"))
            .isEqualTo(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE));
    }

    @Test
    void allowsPostWithMatchingCsrfFromHeadersWhenParameterMissing() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.addHeader("X-CSRF-Token", token);
        request.addHeader("X-CSRF-FormId", formId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isTrue();
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isNotEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isNotEqualTo(formId);
        assertThat(response.getHeader("X-CSRF-Token"))
            .isEqualTo(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE));
        assertThat(response.getHeader("X-CSRF-FormId"))
            .isEqualTo(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE));
    }

    @Test
    void prefersParameterOverHeaderWhenBothPresent() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.setParameter("csrfToken", token);
        request.setParameter("csrfFormId", formId);
        request.addHeader("X-CSRF-Token", "wrong-header-token");
        request.addHeader("X-CSRF-FormId", "wrong-header-form-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isTrue();
    }

    @Test
    void rejectsPostWithMismatchedCsrfHeaderToken() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.addHeader("X-CSRF-Token", "wrong-header-token");
        request.addHeader("X-CSRF-FormId", formId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isEqualTo(formId);
    }

    @Test
    void rejectsPostWithoutCsrfToken() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.setParameter("csrfFormId", formId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isEqualTo(formId);
        assertThat(response.getHeader("X-CSRF-Token")).isNull();
        assertThat(response.getHeader("X-CSRF-FormId")).isNull();
    }

    @Test
    void rejectsPostWithMismatchedCsrfFormId() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.setParameter("csrfToken", token);
        request.setParameter("csrfFormId", "wrong-form-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isEqualTo(formId);
    }

    @Test
    void returnsJsonForAjaxFailure() throws Exception {
        MockHttpSession session = authenticatedSession();
        String token = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        MockHttpServletRequest request = postRequest(session);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.setParameter("csrfToken", "wrong-token");
        request.setParameter("csrfFormId", formId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("CSRF_INVALID", "잘못된 접근입니다.");
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE)).isEqualTo(token);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE)).isEqualTo(formId);
    }

    @Test
    void returnsJsonForAdminApiFailure() throws Exception {
        MockHttpSession session = authenticatedSession();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/api/scenarios");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = interceptor.preHandle(request, response, new Object());

        assertThat(handled).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("CSRF_INVALID");
    }

    private MockHttpServletRequest postRequest(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/scenarios");
        request.setSession(session);
        return request;
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminSession.SESSION_KEY, adminSession());
        csrfTokenIssuer.issue(session);
        return session;
    }

    private AdminSession adminSession() {
        return new AdminSession(1L, "admin", "관리자", Set.of("ADMIN"), false, LocalDateTime.now());
    }
}
