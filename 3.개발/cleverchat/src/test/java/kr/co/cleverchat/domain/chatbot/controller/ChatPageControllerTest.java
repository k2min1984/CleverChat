package kr.co.cleverchat.domain.chatbot.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatPageControllerTest {

    private MockMvc mockMvc;
    private OpsService opsService;
    private CrawlService crawlService;

    @BeforeEach
    void setUp() {
        opsService = Mockito.mock(OpsService.class);
        crawlService = Mockito.mock(CrawlService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ChatPageController(opsService, crawlService))
                        .build();
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

    @Test
    void crawlDocumentPageRendersCachedDocumentView() throws Exception {
        CrawlDocument document = new CrawlDocument();
        document.setCrawlDocumentNo(77L);
        document.setTitle("KEPCO notice");
        document.setContent("로그인\n로그아웃\nCached body\n담당부서 고객지원\n만족하셨습니까?\n사이트맵");
        document.setUrl("https://www.kepco.co.kr/home/media/newsroom/notice/boardView.do");
        when(crawlService.document(77L)).thenReturn(document);

        mockMvc.perform(get("/chat/crawl-documents/77"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/crawlDocument"))
                .andExpect(model().attribute("document", document))
                .andExpect(model().attribute("displayContent", "Cached body"))
                .andExpect(
                        model().attribute(
                                        "sourceUrl",
                                        "https://www.kepco.co.kr/home/media/newsroom/notice/boardList.do"));
    }
}
