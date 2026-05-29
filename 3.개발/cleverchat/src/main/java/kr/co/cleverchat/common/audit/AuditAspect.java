package kr.co.cleverchat.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.co.cleverchat.domain.auth.security.CurrentAdminProvider;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditAspect(
            AuditLogMapper auditLogMapper, ObjectMapper objectMapper, HttpServletRequest request) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @AfterReturning("@annotation(audited)")
    public void writeAuditLog(JoinPoint joinPoint, Audited audited) {
        auditLogMapper.insert(
                actor(),
                audited.action(),
                blankToNull(audited.targetType()),
                null,
                detail(joinPoint),
                clientIp());
    }

    private String actor() {
        return CurrentAdminProvider.currentUsername(request);
    }

    private String detail(JoinPoint joinPoint) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("class", joinPoint.getSignature().getDeclaringTypeName());
        detail.put("method", joinPoint.getSignature().getName());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String clientIp() {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
