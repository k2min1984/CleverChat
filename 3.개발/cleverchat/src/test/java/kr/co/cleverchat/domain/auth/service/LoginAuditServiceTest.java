package kr.co.cleverchat.domain.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import kr.co.cleverchat.common.ops.OpsEventLogger;
import kr.co.cleverchat.domain.auth.mapper.LoginLogMapper;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class LoginAuditServiceTest {

    @Mock LoginLogMapper loginLogMapper;

    @Mock UserMapper userMapper;

    @Mock OpsEventLogger opsEventLogger;

    @Test
    void recordFailureLogsSpikeBeforeLockThreshold() {
        LoginAuditService service =
                new LoginAuditService(loginLogMapper, userMapper, opsEventLogger, 5, 30);
        UserAccount account = new UserAccount();
        account.setUsername("admin");
        account.setFailedAttempts(4);
        when(userMapper.countByUsername("admin")).thenReturn(1);
        when(userMapper.findByUsername("admin")).thenReturn(account);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        ArgumentCaptor<Map> detailCaptor = ArgumentCaptor.forClass(Map.class);

        service.recordFailure("admin", "Password does not match.", request);

        verify(opsEventLogger)
                .securityEvent(
                        eq("LOGIN_FAILURE_SPIKE"),
                        eq(request),
                        eq("admin"),
                        detailCaptor.capture());
        verify(userMapper).increaseFailedAttempt("admin");
        org.assertj.core.api.Assertions.assertThat(detailCaptor.getValue())
                .containsEntry("reason", "near_failed_attempt_threshold")
                .containsEntry("failedAttempts", 4)
                .containsEntry("threshold", 5);
    }

    @Test
    void recordFailureLocksAtThreshold() {
        LoginAuditService service =
                new LoginAuditService(loginLogMapper, userMapper, opsEventLogger, 5, 30);
        UserAccount account = new UserAccount();
        account.setUsername("admin");
        account.setFailedAttempts(5);
        when(userMapper.countByUsername("admin")).thenReturn(1);
        when(userMapper.findByUsername("admin")).thenReturn(account);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        service.recordFailure("admin", "Password does not match.", request);

        verify(userMapper).lockUser("admin", 30);
        verify(opsEventLogger).securityEvent(eq("LOGIN_LOCKED"), eq(request), eq("admin"), any());
    }
}
