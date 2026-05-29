package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Set;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RoleGuardTest {

    private final RoleGuard roleGuard = new RoleGuard();

    @Test
    void allowsMatchingRole() {
        roleGuard.require(session("OPERATOR"), "OPERATOR");
    }

    @Test
    void adminCanPerformOperatorWork() {
        roleGuard.require(session("ADMIN"), "OPERATOR");
    }

    @Test
    void operatorCannotPerformAdminWork() {
        assertThatThrownBy(() -> roleGuard.require(session("OPERATOR"), "ADMIN"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void rejectsMissingSession() {
        assertThatThrownBy(() -> roleGuard.require(null, "OPERATOR"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void rejectsNonMatchingRole() {
        assertThatThrownBy(() -> roleGuard.require(session("VIEWER"), "OPERATOR"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void aspectBlocksInvocationWithoutCurrentAdmin() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        RoleGuardAspect aspect = new RoleGuardAspect(roleGuard);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        RequireRole requireRole = annotatedMethod().getAnnotation(RequireRole.class);

        assertThatThrownBy(() -> aspect.guard(joinPoint, requireRole))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void aspectAllowsInvocationWithCurrentAdmin() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(AdminSession.SESSION_KEY, session("OPERATOR"));
        request.setSession(httpSession);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RoleGuardAspect aspect = new RoleGuardAspect(roleGuard);
        ProceedingJoinPoint joinPoint = org.mockito.Mockito.mock(ProceedingJoinPoint.class);
        RequireRole requireRole = annotatedMethod().getAnnotation(RequireRole.class);

        aspect.guard(joinPoint, requireRole);

        verify(joinPoint).proceed();
        RequestContextHolder.resetRequestAttributes();
    }

    @RequireRole("OPERATOR")
    private void guardedMethod() {}

    private java.lang.reflect.Method annotatedMethod() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("guardedMethod");
    }

    private AdminSession session(String role) {
        return new AdminSession(1L, "admin", "관리자", Set.of(role), false, LocalDateTime.now());
    }
}
