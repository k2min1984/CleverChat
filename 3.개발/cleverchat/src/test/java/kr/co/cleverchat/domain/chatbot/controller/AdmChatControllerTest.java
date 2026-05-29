package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.SessionDetailResponse;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import kr.co.cleverchat.domain.chatbot.service.ChatAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmChatControllerTest {

    @Mock ChatAdminService chatAdminService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdmChatController(chatAdminService)).build();
    }

    @Test
    void failuresPageRendersQueue() throws Exception {
        when(chatAdminService.failures(false, 50)).thenReturn(List.of(new ChatFailureQueueItem()));

        mockMvc.perform(get("/admin/chat/failures").param("reviewed", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/chat/failureList"))
                .andExpect(model().attributeExists("failures"))
                .andExpect(model().attribute("reviewed", false));

        verify(chatAdminService).failures(false, 50);
    }

    @Test
    void sessionsPageRendersList() throws Exception {
        when(chatAdminService.sessions(50)).thenReturn(List.of(new ChatSessionListItem()));

        mockMvc.perform(get("/admin/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/chat/sessionList"))
                .andExpect(model().attributeExists("sessions"));
    }

    @Test
    void sessionDetailPageRendersTrace() throws Exception {
        ChatSessionListItem session = new ChatSessionListItem();
        session.setId("00000000-0000-0000-0000-000000000001");
        when(chatAdminService.sessionDetail("00000000-0000-0000-0000-000000000001"))
                .thenReturn(new SessionDetailResponse(session, List.of(), List.of()));

        mockMvc.perform(get("/admin/chat/sessions/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/chat/sessionDetail"))
                .andExpect(model().attributeExists("detail"));
    }

    @Test
    void feedbackPageRendersList() throws Exception {
        when(chatAdminService.feedback("DOWN", 50)).thenReturn(List.of());

        mockMvc.perform(get("/admin/chat/feedback").param("rating", "DOWN"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/chat/feedbackList"))
                .andExpect(model().attributeExists("feedbackItems"));
    }

    @Test
    void recommendationsPageRendersList() throws Exception {
        when(chatAdminService.recommendations(true)).thenReturn(List.of());

        mockMvc.perform(get("/admin/chat/recommendations").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/chat/recommendationList"))
                .andExpect(model().attributeExists("recommendations"));
    }
}
