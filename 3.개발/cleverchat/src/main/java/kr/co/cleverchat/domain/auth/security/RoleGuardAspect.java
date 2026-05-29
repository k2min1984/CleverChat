package kr.co.cleverchat.domain.auth.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RoleGuardAspect {

    private final RoleGuard roleGuard;

    public RoleGuardAspect(RoleGuard roleGuard) {
        this.roleGuard = roleGuard;
    }

    @Around(value = "@annotation(requireRole)", argNames = "joinPoint,requireRole")
    public Object guard(ProceedingJoinPoint joinPoint, RequireRole requireRole) throws Throwable {
        roleGuard.require(CurrentAdminProvider.current(), requireRole.value());
        return joinPoint.proceed();
    }
}
