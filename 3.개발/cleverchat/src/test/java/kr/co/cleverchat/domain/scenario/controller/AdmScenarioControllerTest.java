package kr.co.cleverchat.domain.scenario.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.scenario.service.ScenarioCategoryService;
import kr.co.cleverchat.domain.scenario.service.ScenarioKeywordService;
import kr.co.cleverchat.domain.scenario.service.ScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmScenarioControllerTest {

    @Mock private ScenarioService scenarioService;

    @Mock private ScenarioCategoryService categoryService;

    @Mock private ScenarioKeywordService keywordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdmScenarioController controller =
                new AdmScenarioController(scenarioService, categoryService, keywordService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void draftVersionGraphEditPageRenders() throws Exception {
        when(scenarioService.get(1L)).thenReturn(scenario(1L));
        when(scenarioService.version(10L)).thenReturn(version(10L, 1L, "DRAFT"));

        mockMvc.perform(get("/admin/scenarios/1/versions/10/graph"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/scenario/scenarioGraphEdit"))
                .andExpect(model().attribute("scenarioId", 1L))
                .andExpect(model().attribute("versionId", 10L))
                .andExpect(
                        model().attribute("graphLoadUrl", "/admin/api/scenarios/versions/10/graph"))
                .andExpect(
                        model().attribute(
                                        "graphSaveUrl", "/admin/api/scenarios/versions/10/graph"));
    }

    @Test
    void publishedVersionGraphEditPageIsRejected() throws Exception {
        when(scenarioService.get(1L)).thenReturn(scenario(1L));
        when(scenarioService.version(10L)).thenReturn(version(10L, 1L, "PUBLISHED"));

        mockMvc.perform(get("/admin/scenarios/1/versions/10/graph"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STATE_CONFLICT"));
    }

    @Test
    void graphEditPageRejectsScenarioVersionMismatch() throws Exception {
        when(scenarioService.get(1L)).thenReturn(scenario(1L));
        when(scenarioService.version(10L)).thenReturn(version(10L, 2L, "DRAFT"));

        mockMvc.perform(get("/admin/scenarios/1/versions/10/graph"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void scenarioListAddsPreviewVersionMap() throws Exception {
        Scenario active = scenario(1L);
        active.setActiveVersionNo(10L);
        Scenario draft = scenario(2L);
        when(scenarioService.findAll(null)).thenReturn(List.of(active, draft));
        when(scenarioService.versions(2L)).thenReturn(List.of(version(20L, 2L, "DRAFT")));

        mockMvc.perform(get("/admin/scenarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/scenario/scenarioList"))
                .andExpect(model().attribute("previewVersionByScenarioId", Map.of(1L, 10L, 2L, 20L)));
    }

    @Test
    void scenarioViewAddsPublishabilityMap() throws Exception {
        ScenarioVersion draft = version(10L, 1L, "DRAFT");
        when(scenarioService.versions(1L)).thenReturn(List.of(draft));
        when(scenarioService.get(1L)).thenReturn(scenario(1L));
        when(scenarioService.publishability(draft))
                .thenReturn(
                        new kr.co.cleverchat.domain.scenario.dto.ScenarioDtos.Publishability(
                                false, false, false, "그래프 저장 후 게시할 수 있습니다."));
        when(keywordService.findKeywords(1L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/scenarios/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/scenario/scenarioView"))
                .andExpect(model().attributeExists("publishabilityByVersionId"));
    }

    @Test
    void createVersionRedirectsWithExistingDraftFlash() throws Exception {
        when(scenarioService.versions(1L)).thenReturn(List.of(version(10L, 1L, "DRAFT")));
        when(scenarioService.createVersion(1L)).thenReturn(version(10L, 1L, "DRAFT"));

        mockMvc.perform(post("/admin/scenarios/1/versions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(
                        flash().attribute("infoMessage", "이미 편집 중인 초안 v1이 있습니다. 기존 초안을 편집해 주세요."))
                .andExpect(
                        flash().attribute("draftEditUrl", "/admin/scenarios/1/versions/10/graph"));
    }

    @Test
    void createVersionFailureRedirectsWithFlashMessage() throws Exception {
        when(scenarioService.versions(1L)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(
                        new BusinessException(ErrorCode.STATE_CONFLICT, "새 초안을 만들 수 없습니다."))
                .when(scenarioService)
                .createVersion(1L);

        mockMvc.perform(post("/admin/scenarios/1/versions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(flash().attribute("errorMessage", "새 초안을 만들 수 없습니다."));
    }

    @Test
    void publishFailureRedirectsWithFlashMessage() throws Exception {
        org.mockito.Mockito.doThrow(
                        new BusinessException(ErrorCode.VALIDATION_ERROR, "저장된 그래프가 없습니다."))
                .when(scenarioService)
                .publish(10L);

        mockMvc.perform(post("/admin/scenarios/versions/10/publish").param("scenarioId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(flash().attribute("errorMessage", "저장된 그래프가 없습니다. 그래프를 저장한 뒤 게시해 주세요."));
    }

    @Test
    void publishSuccessRedirectsWithFlashMessage() throws Exception {
        mockMvc.perform(post("/admin/scenarios/versions/10/publish").param("scenarioId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(flash().attribute("successMessage", "시나리오 버전을 게시했습니다."));
    }

    @Test
    void activateFailureRedirectsWithFlashMessage() throws Exception {
        org.mockito.Mockito.doThrow(
                        new BusinessException(ErrorCode.STATE_CONFLICT, "게시된 버전만 활성화할 수 있습니다."))
                .when(scenarioService)
                .activate(1L, 10L);

        mockMvc.perform(post("/admin/scenarios/1/activate").param("versionId", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(flash().attribute("errorMessage", "게시된 버전만 활성화할 수 있습니다."));
    }

    @Test
    void deactivateFailureRedirectsWithFlashMessage() throws Exception {
        org.mockito.Mockito.doThrow(
                        new BusinessException(
                                ErrorCode.STATE_CONFLICT, "ACTIVE 시나리오만 비활성화할 수 있습니다."))
                .when(scenarioService)
                .deactivate(1L);

        mockMvc.perform(post("/admin/scenarios/1/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/scenarios/1"))
                .andExpect(flash().attribute("errorMessage", "ACTIVE 시나리오만 비활성화할 수 있습니다."));
    }

    @Test
    void scenarioViewTemplateUsesPublishabilityForDisabledPublish() throws Exception {
        String html =
                Files.readString(
                        Path.of("src/main/resources/templates/admmgr/scenario/scenarioView.html"));

        org.assertj.core.api.Assertions.assertThat(html)
                .contains("publishabilityByVersionId")
                .contains("!publishability.publishable()")
                .contains("publishability.reason()")
                .contains("aria-describedby");
    }

    private Scenario scenario(Long id) {
        Scenario scenario = new Scenario();
        scenario.setScenarioNo(id);
        scenario.setTitle("테스트 시나리오");
        scenario.setStatus("DRAFT");
        return scenario;
    }

    private ScenarioVersion version(Long id, Long scenarioId, String status) {
        ScenarioVersion version = new ScenarioVersion();
        version.setScenarioVersionNo(id);
        version.setScenarioNo(scenarioId);
        version.setVersionNo(1);
        version.setStatus(status);
        return version;
    }
}
