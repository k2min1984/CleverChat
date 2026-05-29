package kr.co.cleverchat.common.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SlowQueryMyBatisInterceptorTest {

    private final StructuredLogWriter writer = org.mockito.Mockito.mock(StructuredLogWriter.class);

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logsSlowQueryWithoutSqlParametersOrSensitiveInput() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(OpsEventLogger.REQUEST_ID_ATTRIBUTE, "req-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SlowQueryMyBatisInterceptor interceptor = new SlowQueryMyBatisInterceptor(writer, 0);
        Invocation invocation = invocation("kr.co.cleverchat.Mapper.findSecret");

        interceptor.intercept(invocation);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(writer).slowQuery(captor.capture());
        Map<String, ?> fields = captor.getValue();
        assertThat(fields.get("requestId")).isEqualTo("req-1");
        assertThat(fields.get("statementId")).isEqualTo("kr.co.cleverchat.Mapper.findSecret");
        assertThat(fields.get("operation")).isEqualTo("update");
        assertThat(fields.get("success")).isEqualTo(true);
        assertThat(fields).containsKey("durationMs");
        assertThat(fields.toString()).doesNotContain("password", "select", "where");
    }

    @Test
    void skipsQueryBelowThreshold() throws Throwable {
        SlowQueryMyBatisInterceptor interceptor = new SlowQueryMyBatisInterceptor(writer, 60_000);
        Invocation invocation = invocation("kr.co.cleverchat.Mapper.fast");

        interceptor.intercept(invocation);

        verify(writer, never()).slowQuery(anyMap());
    }

    private Invocation invocation(String statementId) throws Exception {
        MappedStatement statement = org.mockito.Mockito.mock(MappedStatement.class);
        when(statement.getId()).thenReturn(statementId);
        Executor executor = org.mockito.Mockito.mock(Executor.class);
        Method method = Executor.class.getMethod("update", MappedStatement.class, Object.class);
        return new Invocation(executor, method, new Object[] {statement, new Object()});
    }
}
