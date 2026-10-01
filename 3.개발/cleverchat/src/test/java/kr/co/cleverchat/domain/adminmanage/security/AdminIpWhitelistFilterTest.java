package kr.co.cleverchat.domain.adminmanage.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.adminmanage.service.AdminIpWhitelistService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AdminIpWhitelistFilterTest {
    private final AdminIpWhitelistService service =
            org.mockito.Mockito.mock(AdminIpWhitelistService.class);
    private final OpsEventLogger opsEventLogger = org.mockito.Mockito.mock(OpsEventLogger.class);

    @Test
    void blocksDisallowedAdminIpBeforeController() throws Exception {
        AdminIpWhitelistFilter filter = new AdminIpWhitelistFilter(service, opsEventLogger, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/manage/codes");
        request.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(service.isAllowed("198.51.100.10")).thenReturn(false);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void writesJsonForBlockedAdminApi() throws Exception {
        AdminIpWhitelistFilter filter = new AdminIpWhitelistFilter(service, opsEventLogger, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/audit-logs");
        request.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(service.isAllowed("198.51.100.10")).thenReturn(false);

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("ACCESS_DENIED");
    }

    @Test
    void allowsMatchingAdminIp() throws Exception {
        AdminIpWhitelistFilter filter = new AdminIpWhitelistFilter(service, opsEventLogger, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(service.isAllowed("203.0.113.10")).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresNonAdminPaths() throws Exception {
        AdminIpWhitelistFilter filter = new AdminIpWhitelistFilter(service, opsEventLogger, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/chat");
        request.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void featureToggleBypassesFilter() throws Exception {
        AdminIpWhitelistFilter filter = new AdminIpWhitelistFilter(service, opsEventLogger, false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/manage/codes");
        request.setRemoteAddr("198.51.100.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        org.mockito.Mockito.verifyNoInteractions(service);
    }
}
