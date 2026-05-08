package kr.co.cleverchat.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.cleverchat.domain.auth.mapper.LoginLogMapper;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAuditService {

    private final LoginLogMapper loginLogMapper;
    private final UserMapper userMapper;
    private final int maxFailedAttempts;
    private final int lockMinutes;

    public LoginAuditService(
        LoginLogMapper loginLogMapper,
        UserMapper userMapper,
        @Value("${cleverchat.auth.max-failed-attempts:5}") int maxFailedAttempts,
        @Value("${cleverchat.auth.lock-minutes:30}") int lockMinutes
    ) {
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
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
        loginLogMapper.insert(username, false, message, clientIp(request), userAgent(request));
        if (userMapper.countByUsername(username) == 0) {
            return;
        }
        userMapper.increaseFailedAttempt(username);
        UserAccount account = userMapper.findByUsername(username);
        if (account != null && account.getFailedAttempts() >= maxFailedAttempts) {
            userMapper.lockUser(username, lockMinutes);
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
