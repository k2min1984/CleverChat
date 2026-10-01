package kr.co.cleverchat.domain.adminmanage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.adminmanage.mapper.AdminManageMapper;
import kr.co.cleverchat.domain.adminmanage.model.AdminCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminManageServiceTest {

    @Mock private AdminManageMapper mapper;

    private AdminManageService service;

    @BeforeEach
    void setUp() {
        service = new AdminManageService(mapper);
    }

    @Test
    void legacyRootIsAlwaysStoredAtDepthOne() {
        service.createLegacyCode(0L, "코드 그룹", 3, 1L);

        verify(mapper).insertLegacyCode(null, "코드 그룹", 1, 1L);
    }

    @Test
    void legacyChildIsAlwaysStoredAtDepthTwo() {
        AdminCode parent = code(10L, null, 1, true);
        when(mapper.findCodeById(10L)).thenReturn(parent);

        service.createLegacyCode(10L, "상세 코드", 3, 1L);

        verify(mapper).insertLegacyCode(10L, "상세 코드", 2, 1L);
    }

    @Test
    void childBelowDepthTwoIsStoredAtDepthThree() {
        AdminCode child = code(20L, 10L, 2, true);
        when(mapper.findCodeById(20L)).thenReturn(child);

        service.createLegacyCode(20L, "3뎁스", 3, 1L);

        verify(mapper).insertLegacyCode(20L, "3뎁스", 3, 1L);
    }

    @Test
    void childCannotBeRegisteredBelowDepthThree() {
        AdminCode child = code(30L, 20L, 3, true);
        when(mapper.findCodeById(30L)).thenReturn(child);

        assertThatThrownBy(() -> service.createLegacyCode(30L, "4뎁스", 4, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("최대 3뎁스");
    }

    @Test
    void activeCodeOptionsAreGroupedByDepthTwoCodeGroup() {
        when(mapper.findActiveCodeOptions())
                .thenReturn(
                        List.of(
                                Map.of("groupCode", "SCENARIO_STATUS", "value", "ACTIVE"),
                                Map.of("groupCode", "SCENARIO_STATUS", "value", "INACTIVE"),
                                Map.of("groupCode", "CRAWL_STATUS", "value", "SUCCESS")));

        Map<String, List<Map<String, Object>>> result = service.activeCodeOptions();

        assertThat(result.get("SCENARIO_STATUS"))
                .extracting(option -> option.get("value"))
                .containsExactly("ACTIVE", "INACTIVE");
        assertThat(result.get("CRAWL_STATUS"))
                .extracting(option -> option.get("value"))
                .containsExactly("SUCCESS");
    }

    private AdminCode code(Long id, Long parentId, int depth, boolean enabled) {
        AdminCode code = new AdminCode();
        code.setId(id);
        code.setParentId(parentId);
        code.setDepth(depth);
        code.setEnabled(enabled);
        return code;
    }
}
