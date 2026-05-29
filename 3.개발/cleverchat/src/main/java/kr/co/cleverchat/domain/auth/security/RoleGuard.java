package kr.co.cleverchat.domain.auth.security;

import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";

    public void require(AdminSession adminSession, String... roles) {
        if (adminSession == null || !hasAnyRequiredRole(adminSession, roles)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private boolean hasAnyRequiredRole(AdminSession adminSession, String... roles) {
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if (adminSession.hasRole(role)) {
                return true;
            }
            if (ROLE_OPERATOR.equalsIgnoreCase(role) && adminSession.hasRole(ROLE_ADMIN)) {
                return true;
            }
        }
        return false;
    }
}
