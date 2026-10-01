package kr.co.cleverchat.domain.settings;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class SystemSettingsServiceTest {
    private final SystemSettingsMapper mapper = mock(SystemSettingsMapper.class);
    private final SystemSettingsService service = new SystemSettingsService(mapper, "");

    private SystemSettings value(String mode, String directory) {
        return new SystemSettings(
                mode, "https://portal.example.com", "cleverchat", directory, 0, 0, null, null);
    }

    @Test
    void gatewayCannotBeEnabledWithoutSecret() {
        assertThatThrownBy(() -> service.save(value("GATEWAY", null), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시크릿");
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void cannotRemoveDefaultWhileTargetsInheritIt() {
        when(mapper.inheritedExportTargets()).thenReturn(1L);
        assertThatThrownBy(() -> service.save(value("LOCAL", null), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("크롤링 대상");
    }

    @Test
    void staleSettingsAreNotOverwritten() {
        when(mapper.update(any(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.save(value("LOCAL", null), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("새로고침");
    }

    @Test
    void relativeDirectoryIsRejectedBeforeSave() {
        assertThatThrownBy(() -> service.save(value("LOCAL", "relative/path"), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("절대 경로");
    }
}
