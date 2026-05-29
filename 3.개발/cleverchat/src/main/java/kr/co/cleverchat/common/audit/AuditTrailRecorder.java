package kr.co.cleverchat.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import kr.co.cleverchat.domain.auth.security.CurrentAdminProvider;
import org.springframework.stereotype.Component;

@Component
public class AuditTrailRecorder {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditTrailRecorder(
            AuditLogMapper auditLogMapper, ObjectMapper objectMapper, HttpServletRequest request) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public void record(String action, String targetType, Object targetId, Map<String, ?> detail) {
        auditLogMapper.insert(
                actor(),
                action,
                targetType,
                targetId == null ? null : String.valueOf(targetId),
                json(detail),
                clientIp());
    }

    private String actor() {
        return CurrentAdminProvider.currentUsername(request);
    }

    private String clientIp() {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String json(Map<String, ?> detail) {
        if (detail == null || detail.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
