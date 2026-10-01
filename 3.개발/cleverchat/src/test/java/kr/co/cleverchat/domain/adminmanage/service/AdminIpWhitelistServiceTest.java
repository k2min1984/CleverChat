package kr.co.cleverchat.domain.adminmanage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.adminmanage.mapper.AdminIpWhitelistMapper;
import kr.co.cleverchat.domain.adminmanage.model.AdminIpWhitelist;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminIpWhitelistServiceTest {
    private final AdminIpWhitelistMapper mapper =
            org.mockito.Mockito.mock(AdminIpWhitelistMapper.class);
    private final AdminIpWhitelistService service = new AdminIpWhitelistService(mapper);

    @Test
    void emptyActiveListAllowsAllIps() {
        when(mapper.findActive()).thenReturn(List.of());

        assertThat(service.isAllowed("198.51.100.10")).isTrue();
    }

    @Test
    void loopbackIsAlwaysAllowed() {
        when(mapper.findActive()).thenReturn(List.of(entry(1L, "203.0.113.0/24", "Y")));

        assertThat(service.isAllowed("127.0.0.1")).isTrue();
        assertThat(service.isAllowed("::1")).isTrue();
    }

    @Test
    void activeRulesAllowOnlyMatchingIp() {
        when(mapper.findActive()).thenReturn(List.of(entry(1L, "203.0.113.0/24", "Y")));

        assertThat(service.isAllowed("203.0.113.7")).isTrue();
        assertThat(service.isAllowed("198.51.100.10")).isFalse();
    }

    @Test
    void addRejectsInvalidRule() {
        when(mapper.findActive()).thenReturn(List.of());

        assertThatThrownBy(() -> service.addEntry("not-ip", "bad", "admin", "203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void addRejectsCurrentIpLockout() {
        when(mapper.findActive()).thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                service.addEntry(
                                        "198.51.100.0/24",
                                        "other network",
                                        "admin",
                                        "203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("현재 접속 IP");
    }

    @Test
    void addInvalidatesActiveCacheAfterSuccess() {
        when(mapper.findActive())
                .thenReturn(List.of(entry(1L, "198.51.100.0/24", "Y")))
                .thenReturn(
                        List.of(entry(1L, "198.51.100.0/24", "Y"), entry(2L, "203.0.113.10", "Y")));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            AdminIpWhitelist entry = invocation.getArgument(0);
                            entry.setAdminIpWhitelistNo(2L);
                            return null;
                        })
                .when(mapper)
                .insert(any(AdminIpWhitelist.class));
        when(mapper.findById(2L)).thenReturn(entry(2L, "203.0.113.10", "Y"));
        service.isAllowed("203.0.113.10");

        service.addEntry("203.0.113.10", "current", "admin", "203.0.113.10");

        assertThat(service.isAllowed("203.0.113.10")).isTrue();
        verify(mapper, org.mockito.Mockito.times(2)).findActive();
    }

    @Test
    void disableRejectsWhenRemainingActiveRulesExcludeCurrentIp() {
        AdminIpWhitelist currentRule = entry(1L, "203.0.113.10", "Y");
        when(mapper.findById(1L)).thenReturn(currentRule);
        when(mapper.findActive())
                .thenReturn(List.of(currentRule, entry(2L, "198.51.100.0/24", "Y")));

        assertThatThrownBy(() -> service.disableEntry(1L, "admin", "203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("현재 접속 IP");
    }

    @Test
    void updateStoresMetadata() {
        when(mapper.findById(1L)).thenReturn(entry(1L, "203.0.113.10", "Y"));
        when(mapper.findActive()).thenReturn(List.of(entry(1L, "203.0.113.10", "Y")));
        when(mapper.update(any(AdminIpWhitelist.class))).thenReturn(1);
        ArgumentCaptor<AdminIpWhitelist> captor = ArgumentCaptor.forClass(AdminIpWhitelist.class);

        service.updateEntry(1L, "203.0.113.0/24", "office", "Y", "admin", "203.0.113.10");

        verify(mapper).update(captor.capture());
        assertThat(captor.getValue().getIpCidr()).isEqualTo("203.0.113.0/24");
        assertThat(captor.getValue().getDescription()).isEqualTo("office");
        assertThat(captor.getValue().getLstChgrEmpno()).isEqualTo("admin");
        assertThat(captor.getValue().getLstChgrIp()).isEqualTo("203.0.113.10");
    }

    @Test
    void auditedAnnotationsUseExistingAuditInfrastructure() throws Exception {
        assertAudit("addEntry", "ADMIN_IP_WHITELIST_ADD");
        assertAudit("updateEntry", "ADMIN_IP_WHITELIST_UPDATE");
        assertAudit("disableEntry", "ADMIN_IP_WHITELIST_DISABLE");
    }

    private void assertAudit(String methodName, String action) throws Exception {
        Method method;
        if ("addEntry".equals(methodName)) {
            method =
                    AdminIpWhitelistService.class.getMethod(
                            methodName, String.class, String.class, String.class, String.class);
        } else if ("updateEntry".equals(methodName)) {
            method =
                    AdminIpWhitelistService.class.getMethod(
                            methodName,
                            Long.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class,
                            String.class);
        } else {
            method =
                    AdminIpWhitelistService.class.getMethod(
                            methodName, Long.class, String.class, String.class);
        }
        Audited audited = method.getAnnotation(Audited.class);
        assertThat(audited).isNotNull();
        assertThat(audited.action()).isEqualTo(action);
        assertThat(audited.targetType()).isEqualTo("ADMIN_IP_WHITELIST");
    }

    private AdminIpWhitelist entry(Long id, String ipCidr, String useYn) {
        AdminIpWhitelist entry = new AdminIpWhitelist();
        entry.setAdminIpWhitelistNo(id);
        entry.setIpCidr(ipCidr);
        entry.setUseYn(useYn);
        return entry;
    }
}
