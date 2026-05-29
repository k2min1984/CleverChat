package kr.co.cleverchat.common.ops;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
@Intercepts({
    @Signature(
            type = Executor.class,
            method = "update",
            args = {MappedStatement.class, Object.class}),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class,
                Object.class,
                RowBounds.class,
                ResultHandler.class,
                org.apache.ibatis.cache.CacheKey.class,
                org.apache.ibatis.mapping.BoundSql.class
            })
})
public class SlowQueryMyBatisInterceptor implements Interceptor {

    private final StructuredLogWriter structuredLogWriter;
    private final long thresholdMs;

    public SlowQueryMyBatisInterceptor(
            StructuredLogWriter structuredLogWriter,
            @Value("${cleverchat.ops.slow-query-threshold-ms:1000}") long thresholdMs) {
        this.structuredLogWriter = structuredLogWriter;
        this.thresholdMs = Math.max(0L, thresholdMs);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long started = System.nanoTime();
        boolean success = false;
        try {
            Object result = invocation.proceed();
            success = true;
            return result;
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            if (durationMs >= thresholdMs) {
                structuredLogWriter.slowQuery(fields(invocation, durationMs, success));
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No plugin properties are required; Spring configuration supplies the threshold.
    }

    private Map<String, Object> fields(Invocation invocation, long durationMs, boolean success) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requestId", requestId());
        fields.put("statementId", statementId(invocation));
        fields.put("operation", invocation.getMethod().getName());
        fields.put("durationMs", durationMs);
        fields.put("success", success);
        return fields;
    }

    private String statementId(Invocation invocation) {
        Object[] args = invocation.getArgs();
        if (args.length > 0 && args[0] instanceof MappedStatement mappedStatement) {
            return mappedStatement.getId();
        }
        return "unknown";
    }

    private String requestId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object requestId =
                attributes.getAttribute(
                        OpsEventLogger.REQUEST_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return requestId instanceof String value && !value.isBlank() ? value : null;
    }
}
