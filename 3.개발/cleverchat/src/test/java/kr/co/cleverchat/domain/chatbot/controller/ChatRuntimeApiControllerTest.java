package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FeedbackResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.HistorySessionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.ScenarioSummaryResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SessionResponse;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatRuntimeApiControllerTest {

    @Mock private ChatRuntimeService chatRuntimeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ChatRuntimeApiController(chatRuntimeService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void scenariosApiReturnsActiveScenarioSummaries() throws Exception {
        when(chatRuntimeService.activeScenarios())
                .thenReturn(List.of(new ScenarioSummaryResponse(1L, "가입 상담", "가입 안내")));

        mockMvc.perform(get("/chat/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("가입 상담"));
    }

    @Test
    void startIssuesAnonymousIdCookieWhenMissing() throws Exception {
        when(chatRuntimeService.start(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any()))
                .thenReturn(session());

        mockMvc.perform(
                        post("/chat/api/sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"scenarioId\":1}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("anonymous_id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sessionId").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void autoStartIssuesAnonymousIdCookieAndReturnsSession() throws Exception {
        when(chatRuntimeService.startWithText(
                        org.mockito.Mockito.eq("배송"), org.mockito.Mockito.any()))
                .thenReturn(session());

        mockMvc.perform(
                        post("/chat/api/sessions/auto")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"배송\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("anonymous_id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sessionId").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void searchSelectionPassesSpecificNodeIdentity() throws Exception {
        UUID id = session().sessionId();
        when(chatRuntimeService.selectSearchResult(
                        org.mockito.Mockito.eq(id),
                        org.mockito.Mockito.isNull(),
                        org.mockito.Mockito.eq(6L),
                        org.mockito.Mockito.eq(355L),
                        org.mockito.Mockito.eq(42L),
                        org.mockito.Mockito.any()))
                .thenReturn(session());
        mockMvc.perform(
                        post("/chat/api/sessions/{sessionId}/select-search-result", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"scenarioNo\":6,\"scenarioNodeNo\":355,\"sourceMessageId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(chatRuntimeService)
                .selectSearchResult(
                        org.mockito.Mockito.eq(id),
                        org.mockito.Mockito.isNull(),
                        org.mockito.Mockito.eq(6L),
                        org.mockito.Mockito.eq(355L),
                        org.mockito.Mockito.eq(42L),
                        org.mockito.Mockito.any());
    }

    @Test
    void expiredSessionReturnsGoneEnvelope() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(chatRuntimeService.get(org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any()))
                .thenThrow(new BusinessException(ErrorCode.SESSION_EXPIRED, "Session expired."));

        mockMvc.perform(
                        get("/chat/api/sessions/{sessionId}", sessionId)
                                .cookie(
                                        new jakarta.servlet.http.Cookie(
                                                "anonymous_id",
                                                "00000000-0000-0000-0000-000000000002")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SESSION_EXPIRED"));
    }

    @Test
    void sessionHistoryApiReturnsOwnedSessionHistory() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(chatRuntimeService.history(
                        org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any()))
                .thenReturn(session());

        mockMvc.perform(
                        get("/chat/api/sessions/{sessionId}/history", sessionId)
                                .cookie(
                                        new jakarta.servlet.http.Cookie(
                                                "anonymous_id",
                                                "00000000-0000-0000-0000-000000000002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.sessionId").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    void historyListWithoutCookieReturnsEmptyListWithoutCookieIssue() throws Exception {
        mockMvc.perform(get("/chat/api/history"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("anonymous_id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void historyListWithCookieReturnsSessions() throws Exception {
        UUID anonymousId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(chatRuntimeService.historyList(anonymousId))
                .thenReturn(
                        List.of(
                                new HistorySessionResponse(
                                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                                        1L,
                                        "상담",
                                        "COMPLETED",
                                        OffsetDateTime.now().minusMinutes(10),
                                        OffsetDateTime.now(),
                                        2,
                                        "끝")));

        mockMvc.perform(
                        get("/chat/api/history")
                                .cookie(
                                        new jakarta.servlet.http.Cookie(
                                                "anonymous_id", anonymousId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scenarioTitle").value("상담"));
    }

    @Test
    void feedbackApiReturnsFeedbackEnvelope() throws Exception {
        when(chatRuntimeService.feedback(
                        org.mockito.Mockito.eq(10L),
                        org.mockito.Mockito.eq("UP"),
                        org.mockito.Mockito.eq("good"),
                        org.mockito.Mockito.any()))
                .thenReturn(new FeedbackResponse(10L, "UP", "good"));

        mockMvc.perform(
                        post("/chat/api/messages/{messageId}/feedback", 10L)
                                .cookie(
                                        new jakarta.servlet.http.Cookie(
                                                "anonymous_id",
                                                "00000000-0000-0000-0000-000000000002"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rating\":\"UP\",\"comment\":\"good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageId").value(10))
                .andExpect(jsonPath("$.data.rating").value("UP"));
    }

    @Test
    void searchMoreApiDelegatesToRuntimeService() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(chatRuntimeService.searchMore(
                        org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any()))
                .thenReturn(session());

        mockMvc.perform(post("/chat/api/sessions/{sessionId}/search-more", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()));

        verify(chatRuntimeService)
                .searchMore(org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any());
    }

    @Test
    void backApiDelegatesToRuntimeService() throws Exception {
        UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(chatRuntimeService.goBack(
                        org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any()))
                .thenReturn(session());

        mockMvc.perform(post("/chat/api/sessions/{sessionId}/back", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()));

        verify(chatRuntimeService)
                .goBack(org.mockito.Mockito.eq(sessionId), org.mockito.Mockito.any());
    }

    @Test
    void feedbackApiRejectsInvalidRating() throws Exception {
        mockMvc.perform(
                        post("/chat/api/messages/{messageId}/feedback", 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rating\":\"BAD\",\"comment\":\"good\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private SessionResponse session() {
        return new SessionResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                1L,
                2L,
                3L,
                "ACTIVE",
                OffsetDateTime.now().plusMinutes(30),
                List.of(),
                List.of(),
                List.of(),
                0,
                false,
                null);
    }
}
