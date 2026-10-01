package kr.co.cleverchat.domain.scenario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import kr.co.cleverchat.domain.scenario.service.ScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmScenarioApiControllerTest {

    @Mock private ScenarioService scenarioService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdmScenarioApiController(scenarioService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void graphApiReturnsDraftGraph() throws Exception {
        when(scenarioService.graph(10L)).thenReturn(graph("start"));

        mockMvc.perform(get("/admin/api/scenarios/versions/10/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startNodeKey").value("start"))
                .andExpect(jsonPath("$.data.nodes[0].nodeKey").value("start"));
    }

    @Test
    void graphApiReturnsPublishedGraphBecauseReadIsAllowed() throws Exception {
        when(scenarioService.graph(20L)).thenReturn(graph("published-start"));

        mockMvc.perform(get("/admin/api/scenarios/versions/20/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startNodeKey").value("published-start"));
    }

    @Test
    void graphApiSavesDraftGraph() throws Exception {
        mockMvc.perform(
                        put("/admin/api/scenarios/versions/10/graph")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "startNodeKey": "start",
                      "nodes": [
                        {
                          "nodeKey": "start",
                          "nodeType": "QUESTION",
                          "title": "문의 유형",
                          "content": "무엇을 도와드릴까요?",
                          "sortOrder": 1,
                          "metadata": "{}",
                          "options": [
                            {
                              "label": "종료",
                              "nextNodeKey": "end",
                              "conditionExpr": null,
                              "sortOrder": 1,
                              "useYn": "Y"
                            }
                          ]
                        },
                        {
                          "nodeKey": "end",
                          "nodeType": "END",
                          "title": "종료",
                          "content": "감사합니다.",
                          "sortOrder": 2,
                          "metadata": "{}",
                          "options": []
                        }
                      ]
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(scenarioService)
                .saveGraph(any(Long.class), any(ScenarioGraphDtos.SaveRequest.class));
    }

    @Test
    void graphApiRejectsSaveAfterVersionPublished() throws Exception {
        doThrow(new BusinessException(ErrorCode.STATE_CONFLICT, "DRAFT 버전만 수정하거나 게시할 수 있습니다."))
                .when(scenarioService)
                .saveGraph(any(Long.class), any(ScenarioGraphDtos.SaveRequest.class));

        mockMvc.perform(
                        put("/admin/api/scenarios/versions/10/graph")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "startNodeKey": "start",
                      "nodes": [
                        {
                          "nodeKey": "start",
                          "nodeType": "QUESTION",
                          "title": "문의 유형",
                          "content": "무엇을 도와드릴까요?",
                          "sortOrder": 1,
                          "metadata": "{}",
                          "options": []
                        }
                      ]
                    }
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STATE_CONFLICT"));
    }

    @Test
    void createVersionApiReturnsExistingDraftWithSuccessEnvelope() throws Exception {
        when(scenarioService.createVersion(1L)).thenReturn(version(10L, 1L, "DRAFT"));

        mockMvc.perform(post("/admin/api/scenarios/1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scenarioVersionNo").value(10))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void publishApiFailureKeepsJsonErrorEnvelope() throws Exception {
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "저장된 그래프가 없습니다."))
                .when(scenarioService)
                .publish(10L);

        mockMvc.perform(post("/admin/api/scenarios/versions/10/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("저장된 그래프가 없습니다."));
    }

    private ScenarioGraphDtos.SaveRequest graph(String startNodeKey) {
        return new ScenarioGraphDtos.SaveRequest(
                startNodeKey,
                List.of(
                        new ScenarioGraphDtos.NodeRequest(
                                startNodeKey,
                                "QUESTION",
                                "문의 유형",
                                "무엇을 도와드릴까요?",
                                1,
                                "{}",
                                List.of(),
                                List.of())));
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
