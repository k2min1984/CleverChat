package kr.co.cleverchat.common.ops;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAccessLogFilterTest {

    private final OpsEventLogger opsEventLogger = Mockito.mock(OpsEventLogger.class);
    private final ApiAccessLogFilter filter = new ApiAccessLogFilter(opsEventLogger);

    @Test
    void logsAdminApiRequestAndPreservesRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(opsEventLogger.ensureRequestId(request)).thenReturn("req-1");

        filter.doFilter(request, response, new MockFilterChain());

        verify(opsEventLogger).ensureRequestId(request);
        verify(opsEventLogger)
                .apiAccess(
                        Mockito.eq(request),
                        Mockito.eq(200),
                        Mockito.longThat(value -> value >= 0));
        org.assertj.core.api.Assertions.assertThat(
                        response.getHeader(OpsEventLogger.REQUEST_ID_HEADER))
                .isEqualTo("req-1");
    }

    @Test
    void skipsViewRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/scenarios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(opsEventLogger, never()).ensureRequestId(request);
        verify(opsEventLogger, never())
                .apiAccess(Mockito.any(), Mockito.anyInt(), Mockito.anyLong());
    }
}
