package kr.co.cleverchat.domain.crawl.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdmCrawlController(crawlService))
                        .setCustomArgumentResolvers(
                                new org.springframework.web.method.support
                                        .HandlerMethodArgumentResolver() {
                                    @Override
                                    public boolean supportsParameter(
                                            org.springframework.core.MethodParameter parameter) {
                                        return parameter.hasParameterAnnotation(
                                                kr.co.cleverchat.domain.auth.security.CurrentUser
                                                        .class);
                                    }

                                    @Override
                                    public Object resolveArgument(
                                            org.springframework.core.MethodParameter parameter,
                                            org.springframework.web.method.support
                                                            .ModelAndViewContainer
                                                    container,
                                            org.springframework.web.context.request.NativeWebRequest
                                                    request,
                                            org.springframework.web.bind.support
                                                            .WebDataBinderFactory
                                                    factory) {
                                        return null;
                                    }
                                })
                        .build();
    }

    @Test
    void targetsPageRenders() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(1L);
        when(crawlService.targets(true)).thenReturn(List.of(target));
        when(crawlService.runLogs(1L, null, null, 5)).thenReturn(List.of());

        mockMvc.perform(get("/admin/crawl-targets").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/targetList"))
                .andExpect(model().attributeExists("targets", "recentRuns"));

        verify(crawlService).targets(true);
        verify(crawlService).runLogs(1L, null, null, 5);
    }

    @Test
    void newTargetPageRenders() throws Exception {
        mockMvc.perform(get("/admin/crawl-targets/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/targetForm"))
                .andExpect(model().attribute("mode", "create"));
    }

    @Test
    void targetDetailPageRenders() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(1L);
        when(crawlService.target(1L)).thenReturn(target);
        when(crawlService.runLogs(1L, null, null, 20)).thenReturn(List.of());
        when(crawlService.documentPage(1L, "", 1)).thenReturn(List.of());

        mockMvc.perform(get("/admin/crawl-targets/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/targetView"))
                .andExpect(model().attributeExists("target", "runs", "documents"));
    }

    @Test
    void editTargetPageRenders() throws Exception {
        CrawlTarget target = new CrawlTarget();
        target.setCrawlTargetNo(1L);
        when(crawlService.target(1L)).thenReturn(target);

        mockMvc.perform(get("/admin/crawl-targets/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admmgr/crawl/targetForm"))
                .andExpect(model().attribute("mode", "edit"));
    }

    @Test
    void documentsPageRedirectsToUnifiedCrawlManagement() throws Exception {
        mockMvc.perform(get("/admin/crawl-documents").param("targetId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/crawl-targets/1*"));
    }

    @Test
    void runsPageRedirectsToUnifiedCrawlManagement() throws Exception {
        mockMvc.perform(
                        get("/admin/crawl-runs")
                                .param("targetId", "1")
                                .param("status", "FAILED")
                                .param("failureCode", "HTTP_ERROR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/crawl-targets/1*"));
    }

    @Test
    void checkedFormValuesAndJsonDirectoryReachService() throws Exception {
        CrawlTarget saved = new CrawlTarget();
        saved.setCrawlTargetNo(12L);
        when(crawlService.createTarget(any(), isNull())).thenReturn(saved);
        String directory = java.nio.file.Path.of("target/crawl-json").toAbsolutePath().toString();
        mockMvc.perform(
                        post("/admin/crawl-targets")
                                .param("url", "https://example.com/help")
                                .param("useYn", "Y")
                                .param("scheduleEnabled", "true")
                                .param("scheduleIntervalMinutes", "30")
                                .param("scheduleMode", "INTERVAL")
                                .param("jsonExportEnabled", "true")
                                .param("jsonExportDirectory", directory))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<TargetRequest> request = ArgumentCaptor.forClass(TargetRequest.class);
        verify(crawlService).createTarget(request.capture(), isNull());
        assertThat(request.getValue().useYn()).isEqualTo("Y");
        assertThat(request.getValue().scheduleEnabled()).isTrue();
        assertThat(request.getValue().jsonExportEnabled()).isTrue();
        assertThat(request.getValue().jsonExportDirectory()).isEqualTo(directory);
    }

    @Test
    void uncheckedFormValuesAreFalseAndValidationPreservesInput() throws Exception {
        mockMvc.perform(post("/admin/crawl-targets/12").param("url", "https://example.com/help"))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<TargetRequest> request = ArgumentCaptor.forClass(TargetRequest.class);
        verify(crawlService).updateTarget(org.mockito.ArgumentMatchers.eq(12L), request.capture());
        assertThat(request.getValue().useYn()).isEqualTo("N");
        assertThat(request.getValue().scheduleEnabled()).isFalse();
        assertThat(request.getValue().jsonExportEnabled()).isFalse();

        mockMvc.perform(
                        post("/admin/crawl-targets/12")
                                .param("url", "")
                                .param("jsonExportEnabled", "true")
                                .param("jsonExportDirectory", "chosen"))
                .andExpect(view().name("admmgr/crawl/targetForm"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(
                        model().attribute(
                                        "target",
                                        org.hamcrest.Matchers.hasProperty(
                                                "jsonExportDirectory",
                                                org.hamcrest.Matchers.is("chosen"))));
    }
}
