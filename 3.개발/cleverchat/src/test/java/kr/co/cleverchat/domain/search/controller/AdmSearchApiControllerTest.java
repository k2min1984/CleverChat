package kr.co.cleverchat.domain.search.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import kr.co.cleverchat.common.error.GlobalExceptionHandler;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.search.dto.SearchDtos.PopularRebuildResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.RetentionResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.SearchResponse;
import kr.co.cleverchat.domain.search.model.PopularQueryDaily;
import kr.co.cleverchat.domain.search.model.SearchBlockLog;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import kr.co.cleverchat.domain.search.service.SearchService;
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
class AdmSearchApiControllerTest {

    @Mock SearchService searchService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AdmSearchApiController(searchService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new CurrentUserResolver())
                        .build();
    }

    @Test
    void searchTestReturnsResults() throws Exception {
        SearchResultItem item = new SearchResultItem();
        item.setScenarioNo(10L);
        item.setScenarioTitle("FAQ");
        when(searchService.search(
                        org.mockito.Mockito.eq("shipping"),
                        org.mockito.Mockito.eq("ADMIN_TEST"),
                        org.mockito.Mockito.eq(5),
                        org.mockito.Mockito.eq(10L),
                        org.mockito.Mockito.isNull()))
                .thenReturn(new SearchResponse("shipping", 1, List.of(item)));

        mockMvc.perform(
                        post("/admin/api/search/test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"query\":\"shipping\",\"limit\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resultCount").value(1))
                .andExpect(jsonPath("$.data.results[0].scenarioNo").value(10));
    }

    @Test
    void searchTestRejectsInvalidLimitWithoutEchoingQuery() throws Exception {
        mockMvc.perform(
                        post("/admin/api/search/test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"query\":\"shipping secret\",\"limit\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(
                        jsonPath("$.error.message")
                                .value(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "shipping secret"))));
    }

    @Test
    void logsReturnsEnvelope() throws Exception {
        when(searchService.logs("ship", "ADMIN_TEST", 20)).thenReturn(List.of());

        mockMvc.perform(
                        get("/admin/api/search/logs")
                                .param("query", "ship")
                                .param("source", "ADMIN_TEST")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logsRejectsOversizedFilter() throws Exception {
        mockMvc.perform(
                        get("/admin/api/search/logs")
                                .param("query", "x".repeat(201))
                                .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void blocksReturnsEnvelope() throws Exception {
        SearchBlockLog block = new SearchBlockLog();
        block.setSearchBlockLogNo(7L);
        block.setPiiTypes("EMAIL");
        when(searchService.blockLogs("EMAIL", "CHAT_FALLBACK", 10)).thenReturn(List.of(block));

        mockMvc.perform(
                        get("/admin/api/search/blocks")
                                .param("piiType", "EMAIL")
                                .param("source", "CHAT_FALLBACK")
                                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].searchBlockLogNo").value(7));
    }

    @Test
    void popularReturnsEnvelope() throws Exception {
        PopularQueryDaily item = new PopularQueryDaily();
        item.setStatDate(LocalDate.of(2026, 5, 27));
        item.setNormalizedQuery("shipping");
        when(searchService.popular(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 27), 10))
                .thenReturn(List.of(item));

        mockMvc.perform(
                        get("/admin/api/search/popular")
                                .param("from", "2026-05-01")
                                .param("to", "2026-05-27")
                                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].normalizedQuery").value("shipping"));
    }

    @Test
    void popularRejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/admin/api/search/popular").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rebuildPopularReturnsEnvelope() throws Exception {
        LocalDate date = LocalDate.of(2026, 5, 27);
        when(searchService.rebuildPopular(date))
                .thenReturn(new PopularRebuildResponse(date, 1, 0, true, OffsetDateTime.now()));

        mockMvc.perform(
                        post("/admin/api/search/popular/rebuild")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"statDate\":\"2026-05-27\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rebuiltCount").value(1));
    }

    @Test
    void deleteExpiredReturnsEnvelope() throws Exception {
        when(searchService.deleteExpiredLogs(90, true))
                .thenReturn(new RetentionResponse(90, OffsetDateTime.now(), 0, 0, 2, 1, true));

        mockMvc.perform(
                        delete("/admin/api/search/logs/expired")
                                .param("retentionDays", "90")
                                .param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dryRun").value(true));
    }

    @Test
    void deleteExpiredRejectsTooSmallRetentionDays() throws Exception {
        mockMvc.perform(
                        delete("/admin/api/search/logs/expired")
                                .param("retentionDays", "29")
                                .param("dryRun", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private static class CurrentUserResolver implements HandlerMethodArgumentResolver {
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
