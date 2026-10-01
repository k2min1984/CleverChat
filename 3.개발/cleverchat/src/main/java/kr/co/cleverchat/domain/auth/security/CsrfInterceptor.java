package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final String CSRF_TOKEN_PARAMETER = "csrfToken";
    private static final String CSRF_FORM_ID_PARAMETER = "csrfFormId";
    private static final String CSRF_TOKEN_RESPONSE_HEADER = "X-CSRF-Token";
    private static final String CSRF_FORM_ID_RESPONSE_HEADER = "X-CSRF-FormId";
    private static final int MAX_CSRF_VALUE_LENGTH = 128;

    private final CsrfTokenIssuer csrfTokenIssuer;
    private final OpsEventLogger opsEventLogger;

    public CsrfInterceptor(CsrfTokenIssuer csrfTokenIssuer, OpsEventLogger opsEventLogger) {
        this.csrfTokenIssuer = csrfTokenIssuer;
        this.opsEventLogger = opsEventLogger;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (Set.of("GET", "HEAD", "OPTIONS", "TRACE")
                .contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(AdminSession.SESSION_KEY) instanceof AdminSession)) {
            opsEventLogger.securityEvent(
                    "CSRF_FORBIDDEN", request, null, Map.of("reason", "missing_session"));
            writeForbidden(request, response);
            return false;
        }

        String sessionToken =
                getSessionString(session, CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String sessionFormId =
                getSessionString(session, CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        String requestToken =
                resolveCsrfValue(request, CSRF_TOKEN_PARAMETER, CSRF_TOKEN_RESPONSE_HEADER);
        String requestFormId =
                resolveCsrfValue(request, CSRF_FORM_ID_PARAMETER, CSRF_FORM_ID_RESPONSE_HEADER);

        if (!matches(sessionToken, requestToken) || !matches(sessionFormId, requestFormId)) {
            String username =
                    ((AdminSession) session.getAttribute(AdminSession.SESSION_KEY)).getUsername();
            opsEventLogger.securityEvent(
                    "CSRF_FORBIDDEN", request, username, Map.of("reason", "token_mismatch"));
            writeForbidden(request, response);
            return false;
        }

        csrfTokenIssuer.issue(session);
        setReissuedHeaders(response, session);
        return true;
    }

    private String getSessionString(HttpSession session, String name) {
        Object value = session.getAttribute(name);
        return value instanceof String stringValue ? stringValue : null;
    }

    private String resolveCsrfValue(
            HttpServletRequest request, String parameterName, String headerName) {
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue != null && !parameterValue.isBlank()) {
            return parameterValue;
        }
        return request.getHeader(headerName);
    }

    private boolean matches(String expected, String actual) {
        if (!isUsable(expected) || !isUsable(actual)) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private boolean isUsable(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_CSRF_VALUE_LENGTH;
    }

    private void setReissuedHeaders(HttpServletResponse response, HttpSession session) {
        String token = getSessionString(session, CsrfTokenIssuer.CSRF_TOKEN_SESSION_ATTRIBUTE);
        String formId = getSessionString(session, CsrfTokenIssuer.CSRF_FORM_ID_SESSION_ATTRIBUTE);
        if (token != null) {
            response.setHeader(CSRF_TOKEN_RESPONSE_HEADER, token);
        }
        if (formId != null) {
            response.setHeader(CSRF_FORM_ID_RESPONSE_HEADER, formId);
        }
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        if (!isJsonResponseRequest(request)) {
            return;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter()
                .write(
                        "{\"success\":false,\"error\":{\"code\":\"CSRF_INVALID\",\"message\":\"잘못된 접근입니다.\"}}");
    }

    private boolean isJsonResponseRequest(HttpServletRequest request) {
        return isXmlHttpRequest(request) || isAdminApiRequest(request) || acceptsJson(request);
    }

    private boolean isXmlHttpRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private boolean isAdminApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/admin/api/");
    }

    private boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null
                && accept.toLowerCase(Locale.ROOT).contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
