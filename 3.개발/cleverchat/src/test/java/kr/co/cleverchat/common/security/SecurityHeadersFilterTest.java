package kr.co.cleverchat.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    @Test
    void addsSecurityHeadersToHtmlRequest() throws Exception {
        MockHttpServletResponse response = filterWithProfiles().filter("/admin/scenarios");

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy"))
                .isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("Permissions-Policy"))
                .isEqualTo("camera=(), microphone=(), geolocation=()");
        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo(SecurityHeadersFilter.CSP_POLICY);
    }

    @Test
    void addsSecurityHeadersToApiAndHealthRequests() throws Exception {
        for (String path :
                new String[] {"/admin/api/scenarios", "/chat/api/sessions", "/actuator/health"}) {
            MockHttpServletResponse response = filterWithProfiles().filter(path);

            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(response.getHeader("Content-Security-Policy"))
                    .isEqualTo(SecurityHeadersFilter.CSP_POLICY);
        }
    }

    @Test
    void cspDoesNotAllowExternalDomains() throws Exception {
        MockHttpServletResponse response = filterWithProfiles().filter("/chat");

        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("frame-ancestors 'none'")
                .doesNotContain("http://")
                .doesNotContain("https://")
                .doesNotContain("*");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void doesNotSetHstsByDefault() throws Exception {
        MockHttpServletResponse response = filterWithProfiles().filter("/admin");

        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void setsHstsOnlyForProdProfile() throws Exception {
        MockHttpServletResponse response = filterWithProfiles("prod").filter("/admin");

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo(SecurityHeadersFilter.HSTS_POLICY);
    }

    private TestFilter filterWithProfiles(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new TestFilter(new SecurityHeadersFilter(environment));
    }

    private record TestFilter(SecurityHeadersFilter filter) {

        MockHttpServletResponse filter(String path) throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            return response;
        }
    }
}
