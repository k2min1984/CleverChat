package kr.co.cleverchat.common.ops;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import org.springframework.stereotype.Component;

@Component
public class OpsEventLogger {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "CLEVERCHAT_REQUEST_ID";

    private final StructuredLogWriter structuredLogWriter;

    public OpsEventLogger(StructuredLogWriter structuredLogWriter) {
        this.structuredLogWriter = structuredLogWriter;
    }

    public void apiAccess(HttpServletRequest request, int status, long durationMs) {
        Map<String, Object> fields = baseFields(request);
        fields.put("method", request.getMethod());
        fields.put("path", path(request));
        fields.put("status", status);
        fields.put("durationMs", durationMs);
        AdminSession adminSession = adminSession(request);
        if (adminSession != null) {
            fields.put("actor", adminSession.getUsername());
            fields.put("actorType", "ADMIN");
        } else if (anonymousId(request) != null) {
            fields.put("actor", OpsLogHasher.sha256(anonymousId(request)));
            fields.put("actorType", "ANONYMOUS");
        } else {
            fields.put("actorType", "UNKNOWN");
        }
        structuredLogWriter.apiAccess(fields);
    }

    public void securityEvent(
            String type, HttpServletRequest request, String username, Map<String, ?> detail) {
        Map<String, Object> fields = baseFields(request);
        fields.put("type", type);
        fields.put("path", path(request));
        fields.put("method", request.getMethod());
        if (username != null && !username.isBlank()) {
            fields.put("username", username);
        }
        if (detail != null && !detail.isEmpty()) {
            fields.put("detail", detail);
        }
        structuredLogWriter.securityEvent(fields);
    }

    public String ensureRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }
        String headerValue = request.getHeader(REQUEST_ID_HEADER);
        String requestId =
                headerValue == null || headerValue.isBlank()
                        ? UUID.randomUUID().toString()
                        : headerValue;
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }

    private Map<String, Object> baseFields(HttpServletRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requestId", ensureRequestId(request));
        fields.put("remoteAddrHash", OpsLogHasher.sha256(clientIp(request)));
        fields.put("userAgentHash", OpsLogHasher.sha256(request.getHeader("User-Agent")));
        return fields;
    }

    private String path(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (contextPath == null || contextPath.isBlank() || !uri.startsWith(contextPath)) {
            return uri;
        }
        return uri.substring(contextPath.length());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AdminSession adminSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(AdminSession.SESSION_KEY);
        return value instanceof AdminSession adminSession ? adminSession : null;
    }

    private String anonymousId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("anonymous_id".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
