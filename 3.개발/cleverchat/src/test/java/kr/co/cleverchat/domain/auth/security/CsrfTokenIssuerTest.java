package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class CsrfTokenIssuerTest {

    private final CsrfTokenIssuer issuer = new CsrfTokenIssuer();

    @Test
    void issueStoresUrlSafeTokenAndFormIdInSession() {
        MockHttpSession session = new MockHttpSession();

        issuer.issue(session);

        assertThat((String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE))
            .hasSize(43)
            .matches("[A-Za-z0-9_-]+");
        assertThat((String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE))
            .hasSize(22)
            .matches("[A-Za-z0-9_-]+");
    }

    @Test
    void issueRotatesTokenAndFormIdWhenCalledAgain() {
        MockHttpSession session = new MockHttpSession();
        issuer.issue(session);
        String firstToken = (String) session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String firstFormId = (String) session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);

        issuer.issue(session);

        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE))
            .isNotEqualTo(firstToken);
        assertThat(session.getAttribute(CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE))
            .isNotEqualTo(firstFormId);
    }
}
