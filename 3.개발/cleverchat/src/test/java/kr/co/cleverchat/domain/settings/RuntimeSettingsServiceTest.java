package kr.co.cleverchat.domain.settings;

import static kr.co.cleverchat.domain.settings.RuntimeSetting.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RuntimeSettingsServiceTest {
    private final SystemSettingsMapper mapper = mock(SystemSettingsMapper.class);
    private final RuntimeSettingsService service =
            new RuntimeSettingsService(mapper, new ObjectMapper(), new MockEnvironment());

    private Map<String, String> input(String section) {
        var values = new HashMap<String, String>();
        for (var field : service.fields(section))
            values.put(field.name(), service.current().values().get(field.name()).toString());
        return values;
    }

    @Test
    void defaultsPreserveDeploymentConfigurationUntilSaved() {
        var configured =
                new RuntimeSettingsService(
                        mapper,
                        new ObjectMapper(),
                        new MockEnvironment()
                                .withProperty("cleverchat.crawl.browser.enabled", "true")
                                .withProperty("chat.search.display-max", "7"));
        assertThat(configured.current().bool(BROWSER_ENABLED)).isTrue();
        assertThat(configured.current().integer(SEARCH_DISPLAY)).isEqualTo(7);
        assertThat(configured.current().bool(JSON_RETENTION_ENABLED)).isFalse();
    }

    @Test
    void savesOnlyWhitelistedFieldsFromRequestedSection() throws Exception {
        when(mapper.updateRuntime(anyString(), eq(3L), eq("admin"))).thenReturn(1);
        var input = input("search");
        input.put("SEARCH_DISPLAY", "2");
        input.put("LOGIN_FAILURES", "999");
        service.save("search", input, 3, "admin");
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mapper).updateRuntime(captured.capture(), eq(3L), eq("admin"));
        var saved = new ObjectMapper().readTree(captured.getValue());
        assertThat(saved.path("SEARCH_DISPLAY").asInt()).isEqualTo(2);
        assertThat(saved.has("LOGIN_FAILURES")).isFalse();
    }

    @Test
    void inconsistentSearchAndNonFiniteScoresAreRejected() {
        var values = input("search");
        values.put("SEARCH_POOL", "2");
        values.put("SEARCH_DISPLAY", "5");
        assertThatThrownBy(() -> service.save("search", values, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        values.put("SEARCH_POOL", "10");
        values.put("SEARCH_RELATIVE", "NaN");
        assertThatThrownBy(() -> service.save("search", values, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mapper, never()).updateRuntime(anyString(), anyLong(), anyString());
    }

    @Test
    void enabledAiRequiresEndpointAndModelAndRejectsCredentialUrls() {
        var values = input("ai");
        values.put("AI_ENABLED", "true");
        assertThatThrownBy(() -> service.save("ai", values, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        values.put("AI_BASE_URL", "https://user:password@example.com/v1");
        values.put("AI_MODEL", "fixture");
        assertThatThrownBy(() -> service.save("ai", values, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        values.put("AI_BASE_URL", "file:///tmp/v1");
        assertThatThrownBy(() -> service.save("ai", values, 0, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conflictingVersionDoesNotOverwriteSavedValues() {
        assertThatThrownBy(() -> service.save("chat", input("chat"), 9, "admin"))
                .hasMessageContaining("다른 관리자");
    }

    @Test
    void runningJobKeepsItsSnapshotAndFollowingJobSeesChange() {
        when(mapper.getRuntimeJson()).thenReturn("{\"BOARD_PAGE_LIMIT\":10}");
        service.runWithSnapshot(
                () -> {
                    assertThat(service.current().integer(BOARD_PAGE_LIMIT)).isEqualTo(10);
                    when(mapper.getRuntimeJson()).thenReturn("{\"BOARD_PAGE_LIMIT\":2}");
                    service.invalidate();
                    assertThat(service.current().integer(BOARD_PAGE_LIMIT)).isEqualTo(10);
                });
        assertThat(service.current().integer(BOARD_PAGE_LIMIT)).isEqualTo(2);
    }

    @Test
    void retentionCannotAcceptShortPeriodOrInvalidClockTime() {
        var values = input("retention");
        values.put("JSON_RETENTION_DAYS", "1");
        assertThatThrownBy(() -> service.save("retention", values, 0, "admin"))
                .hasMessageContaining("보관일");
        values.put("JSON_RETENTION_DAYS", "90");
        values.put("RETENTION_TIME", "25:00");
        assertThatThrownBy(() -> service.save("retention", values, 0, "admin"))
                .hasMessageContaining("시각");
    }
}
