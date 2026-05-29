package kr.co.cleverchat.domain.crawl.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdmCrawlControllerTest {

    @Mock CrawlService crawlService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdmCrawlController(crawlService)).build();
    }

    @Test
    void targetsPageRenders() throws Exception {
        when(crawlService.targets(true)).thenReturn(List.of());

        mockMvc.perform(get("/admin/crawl-targets").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/targetList"))
                .andExpect(model().attributeExists("targets"));

        verify(crawlService).targets(true);
    }

    @Test
    void documentsPageRenders() throws Exception {
        when(crawlService.documents(1L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/crawl-documents").param("targetId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/documentList"))
                .andExpect(model().attributeExists("documents"));

        verify(crawlService).documents(1L);
    }

    @Test
    void runsPageRenders() throws Exception {
        when(crawlService.runLogs(1L, "FAILED", "HTTP_ERROR", 100)).thenReturn(List.of());

        mockMvc.perform(
                        get("/admin/crawl-runs")
                                .param("targetId", "1")
                                .param("status", "FAILED")
                                .param("failureCode", "HTTP_ERROR"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/runList"))
                .andExpect(model().attributeExists("runs"));

        verify(crawlService).runLogs(1L, "FAILED", "HTTP_ERROR", 100);
    }
}
