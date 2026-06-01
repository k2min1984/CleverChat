package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.SessionDetailResponse;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import kr.co.cleverchat.domain.chatbot.service.ChatAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AdmChatApiControllerTest {

    @Mock ChatAdminService chatAdminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdmChatApiController(chatAdminService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new TestCurrentUserResolver())
                        .build();
    }

    @Test
    void failuresApiReturnsQueue() throws Exception {
        ChatFailureQueueItem item = new ChatFailureQueueItem();
        item.setChatFailureNo(1L);
        item.setSessionKey("abcdef12");
        item.setReason("NO_MATCH");
        when(chatAdminService.failures(false, 20)).thenReturn(List.of(item));

        mockMvc.perform(
                        get("/admin/api/chat/failures")
                                .param("reviewed", "false")
                                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].chatFailureNo").value(1))
                .andExpect(jsonPath("$.data[0].sessionKey").value("abcdef12"));
    }

    @Test
    void sessionsApiReturnsList() throws Exception {
        ChatSessionListItem item = new ChatSessionListItem();
        item.setChatSessionNo("00000000-0000-0000-0000-000000000001");
        item.setSessionKey("00000000");
        when(chatAdminService.sessions(10)).thenReturn(List.of(item));

        mockMvc.perform(get("/admin/api/chat/sessions").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sessionKey").value("00000000"));
    }

    @Test
    void sessionDetailApiReturnsTrace() throws Exception {
        ChatSessionListItem item = new ChatSessionListItem();
        item.setChatSessionNo("00000000-0000-0000-0000-000000000001");
        when(chatAdminService.sessionDetail("00000000-0000-0000-0000-000000000001"))
                .thenReturn(new SessionDetailResponse(item, List.of(), List.of()));

        mockMvc.perform(get("/admin/api/chat/sessions/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.session.chatSessionNo")
                                .value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void reviewApiMarksFailureReviewedWithCurrentAdmin() throws Exception {
        mockMvc.perform(
                        post("/admin/api/chat/failures/{id}/review", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(chatAdminService).reviewFailure(1L, 10L, "checked");
    }

    @Test
    void reviewApiRejectsLongComment() throws Exception {
        mockMvc.perform(
                        post("/admin/api/chat/failures/{id}/review", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void feedbackApiReturnsList() throws Exception {
        when(chatAdminService.feedback("UP", 10)).thenReturn(List.of());

        mockMvc.perform(get("/admin/api/chat/feedback").param("rating", "UP").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRecommendationApiReturnsCreatedRecommendation() throws Exception {
        ChatRecommendation recommendation = new ChatRecommendation();
        recommendation.setChatRecommendationNo(1L);
        recommendation.setScenarioNo(2L);
        recommendation.setLabel("Question");
        recommendation.setPriority(10);
        recommendation.setUseYn("Y");
        when(chatAdminService.createRecommendation(org.mockito.Mockito.any()))
                .thenReturn(recommendation);

        mockMvc.perform(
                        post("/admin/api/chat/recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scenarioId\":2,\"label\":\"Question\",\"priority\":10,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRecommendationNo").value(1));
    }

    @Test
    void createRecommendationApiRejectsInvalidScenarioIdAndPriority() throws Exception {
        mockMvc.perform(
                        post("/admin/api/chat/recommendations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scenarioId\":0,\"label\":\"Question\",\"priority\":-1,\"enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateRecommendationApiReturnsUpdatedRecommendation() throws Exception {
        ChatRecommendation recommendation = new ChatRecommendation();
        recommendation.setChatRecommendationNo(1L);
        recommendation.setScenarioNo(2L);
        recommendation.setLabel("Question");
        when(chatAdminService.updateRecommendation(
                        org.mockito.Mockito.eq(1L), org.mockito.Mockito.any()))
                .thenReturn(recommendation);

        mockMvc.perform(
                        put("/admin/api/chat/recommendations/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"scenarioId\":2,\"label\":\"Question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRecommendationNo").value(1));
    }

    private static class TestCurrentUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            return new AdminSession(
                    10L, "admin", "Admin", Set.of("ADMIN"), false, LocalDateTime.now());
        }
    }
}
