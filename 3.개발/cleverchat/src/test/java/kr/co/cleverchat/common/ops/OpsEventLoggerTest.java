package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

class OpsEventLoggerTest {

    private final StructuredLogWriter structuredLogWriter = Mockito.mock(StructuredLogWriter.class);
    private final OpsEventLogger logger = new OpsEventLogger(structuredLogWriter);

    @Test
    void apiAccessLogsHashesWithoutQueryOrRawNetworkValues() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/chat/api/recommendations");
        request.setQueryString("q=test@example.com");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit Browser");
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        logger.apiAccess(request, 200, 12);

        verify(structuredLogWriter).apiAccess(captor.capture());
        Map<String, ?> fields = captor.getValue();
        assertThat(fields.get("path")).isEqualTo("/chat/api/recommendations");
        assertThat(fields).doesNotContainKey("query");
        assertThat(fields.get("remoteAddrHash")).asString().doesNotContain("127.0.0.1");
        assertThat(fields.get("userAgentHash")).asString().doesNotContain("JUnit Browser");
    }

    @Test
    void ensureRequestIdKeepsInboundHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/api/scenarios");
        request.addHeader(OpsEventLogger.REQUEST_ID_HEADER, "client-req");

        String requestId = logger.ensureRequestId(request);

        assertThat(requestId).isEqualTo("client-req");
        assertThat(request.getAttribute(OpsEventLogger.REQUEST_ID_ATTRIBUTE))
                .isEqualTo("client-req");
    }
}
