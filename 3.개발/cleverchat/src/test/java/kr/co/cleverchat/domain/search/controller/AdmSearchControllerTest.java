package kr.co.cleverchat.domain.search.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmSearchControllerTest {

    @Mock SearchService searchService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdmSearchController(searchService)).build();
    }

    @Test
    void logsPageRendersSearchLogs() throws Exception {
        when(searchService.logs("ship", "ADMIN_TEST", 50)).thenReturn(List.of());

        mockMvc.perform(
                        get("/admin/search/logs")
                                .param("query", "ship")
                                .param("source", "ADMIN_TEST"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/search/logList"))
                .andExpect(model().attributeExists("logs"));

        verify(searchService).logs("ship", "ADMIN_TEST", 50);
    }

    @Test
    void blocksPageRendersBlockedSearches() throws Exception {
        when(searchService.blockLogs("EMAIL", "CHAT_FALLBACK", 50)).thenReturn(List.of());

        mockMvc.perform(
                        get("/admin/search/blocks")
                                .param("piiType", "EMAIL")
                                .param("source", "CHAT_FALLBACK"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/search/blockList"))
                .andExpect(model().attributeExists("blocks"));

        verify(searchService).blockLogs("EMAIL", "CHAT_FALLBACK", 50);
    }

    @Test
    void popularPageRendersPopularQueries() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 27);
        when(searchService.popular(from, to, 50)).thenReturn(List.of());

        mockMvc.perform(
                        get("/admin/search/popular")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-27"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/search/popularList"))
                .andExpect(model().attributeExists("popularQueries"));

        verify(searchService).popular(from, to, 50);
    }
}
