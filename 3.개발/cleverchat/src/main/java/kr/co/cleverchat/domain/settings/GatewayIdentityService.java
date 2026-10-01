package kr.co.cleverchat.domain.settings;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GatewayIdentityService {
    private final String secret;

    public GatewayIdentityService(@Value("${cleverchat.gateway.shared-secret:}") String secret) {
        this.secret = secret;
    }

    public Identity verify(HttpServletRequest request, String serviceId) {
        String supplied = request.getHeader("X-Clever-Gw-Secret");
        if (secret == null
                || secret.isBlank()
                || supplied == null
                || !MessageDigest.isEqual(digest(secret), digest(supplied)))
            throw new IllegalArgumentException("SSO 신원을 확인할 수 없습니다.");
        String empNo = request.getHeader("X-Clever-User");
        if (empNo == null
                || empNo.isBlank()
                || empNo.length() > 64
                || empNo.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("SSO 사번이 올바르지 않습니다.");
        String role = null;
        String encodedRoles = request.getHeader("X-Clever-Roles");
        if (encodedRoles != null) {
            if (encodedRoles.length() > 8192)
                throw new IllegalArgumentException("SSO 역할이 올바르지 않습니다.");
            for (String pair : encodedRoles.split(";")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2 && serviceId.equals(parts[0].trim())) {
                    if (role != null) throw new IllegalArgumentException("SSO 역할이 중복되었습니다.");
                    role = parts[1].trim();
                }
            }
        }
        if ("1".equals(request.getHeader("X-Clever-Admin"))) role = "ADMIN";
        if (role == null || !Set.of("ADMIN", "OPERATOR", "USER").contains(role))
            throw new IllegalArgumentException("이 서비스에 부여된 권한이 없습니다.");
        String name = decode(request.getHeader("X-Clever-Name"), 100);
        return new Identity(
                empNo,
                name == null ? empNo : name,
                decode(request.getHeader("X-Clever-Unit"), 200),
                Set.of(role));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decode(String value, int max) {
        if (value == null || value.isBlank()) return null;
        // Header values are percent encoded, not application/x-www-form-urlencoded.
        String decoded = URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        if (decoded.length() > max || decoded.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("SSO 사용자 정보가 올바르지 않습니다.");
        return decoded;
    }

    public record Identity(String empNo, String name, String unit, Set<String> roles) {}
}
