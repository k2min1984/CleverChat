package kr.co.cleverchat.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.auth.mapper.LoginLogMapper;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAuditService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private final LoginLogMapper loginLogMapper;
    private final UserMapper userMapper;
    private final int maxFailedAttempts;
    private final int lockMinutes;
    private final OpsEventLogger opsEventLogger;

    public LoginAuditService(
            LoginLogMapper loginLogMapper,
            UserMapper userMapper,
            OpsEventLogger opsEventLogger,
            @Value("${cleverchat.auth.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${cleverchat.auth.lock-minutes:30}") int lockMinutes) {
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
        this.opsEventLogger = opsEventLogger;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
    }

    @Transactional
    public void recordSuccess(String username, HttpServletRequest request) {
        userMapper.resetLoginSuccess(username);
        loginLogMapper.insert(username, true, null, clientIp(request), userAgent(request));
    }

    @Transactional
    public void recordFailure(String username, String message, HttpServletRequest request) {
        int maxFailedAttempts =
                runtimeSettings == null
                        ? this.maxFailedAttempts
                        : runtimeSettings.current().integer(RuntimeSetting.LOGIN_FAILURES);
        int lockMinutes =
                runtimeSettings == null
                        ? this.lockMinutes
                        : runtimeSettings.current().integer(RuntimeSetting.LOGIN_LOCK_MINUTES);
        loginLogMapper.insert(username, false, message, clientIp(request), userAgent(request));
        if (userMapper.countByUsername(username) == 0) {
            return;
        }
        userMapper.increaseFailedAttempt(username);
        UserAccount account = userMapper.findByUsername(username);
        if (account != null && account.getFailedAttempts() >= maxFailedAttempts) {
            userMapper.lockUser(username, lockMinutes);
            opsEventLogger.securityEvent(
                    "LOGIN_LOCKED",
                    request,
                    username,
                    Map.of("reason", "failed_attempt_threshold"));
        } else if (account != null && account.getFailedAttempts() >= maxFailedAttempts - 1) {
            opsEventLogger.securityEvent(
                    "LOGIN_FAILURE_SPIKE",
                    request,
                    username,
                    Map.of(
                            "reason",
                            "near_failed_attempt_threshold",
                            "failedAttempts",
                            account.getFailedAttempts(),
                            "threshold",
                            maxFailedAttempts));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }
}
