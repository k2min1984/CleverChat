package kr.co.cleverchat.common.ops;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(20)
public class ApiAccessLogFilter extends OncePerRequestFilter {

    private final OpsEventLogger opsEventLogger;

    public ApiAccessLogFilter(OpsEventLogger opsEventLogger) {
        this.opsEventLogger = opsEventLogger;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = path(request);
        return !(path.startsWith("/admin/api/") || path.startsWith("/chat/api/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = opsEventLogger.ensureRequestId(request);
        response.setHeader(OpsEventLogger.REQUEST_ID_HEADER, requestId);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            opsEventLogger.apiAccess(request, response.getStatus(), durationMs);
        }
    }

    private String path(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath == null || contextPath.isBlank() || !uri.startsWith(contextPath)) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }
}
