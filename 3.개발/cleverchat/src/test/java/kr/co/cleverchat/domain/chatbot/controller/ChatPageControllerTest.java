package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatPageControllerTest {

    private MockMvc mockMvc;
    private OpsService opsService;

    @BeforeEach
    void setUp() {
        opsService = Mockito.mock(OpsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatPageController(opsService)).build();
    }

    @Test
    void chatPageRendersDedicatedChatView() throws Exception {
        when(opsService.visibleNotices()).thenReturn(List.of());

        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/chat"))
                .andExpect(model().attributeExists("notices"));
    }

    @Test
    void historyPageRendersDedicatedHistoryView() throws Exception {
        when(opsService.visibleNotices()).thenReturn(List.of());

        mockMvc.perform(get("/chat/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/history"))
                .andExpect(model().attributeExists("notices"));
    }
}
