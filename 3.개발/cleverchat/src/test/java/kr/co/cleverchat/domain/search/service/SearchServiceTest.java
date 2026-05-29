package kr.co.cleverchat.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.chatbot.service.ChatPiiGuard;
import kr.co.cleverchat.domain.search.mapper.SearchMapper;
import kr.co.cleverchat.domain.search.model.SearchBlockLog;
import kr.co.cleverchat.domain.search.model.SearchLog;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchServiceTest {

    private final SearchMapper searchMapper = org.mockito.Mockito.mock(SearchMapper.class);
    private final ChatPiiGuard piiGuard = org.mockito.Mockito.mock(ChatPiiGuard.class);
    private final SearchService service = new SearchService(searchMapper, piiGuard);

    @Test
    void searchNormalizesQueryAndRecordsLog() {
        SearchResultItem result = new SearchResultItem();
        result.setScenarioId(10L);
        result.setScenarioTitle("FAQ");
        when(piiGuard.detectTypes("shipping help")).thenReturn(List.of());
        when(searchMapper.searchScenarios("shipping help", List.of("shipping", "help"), 5))
                .thenReturn(List.of(result));
        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        var response = service.search("  Shipping   HELP ", "ADMIN_TEST", 5, 1L, null);

        assertThat(response.normalizedQuery()).isEqualTo("shipping help");
        assertThat(response.resultCount()).isEqualTo(1);
        verify(searchMapper).searchScenarios("shipping help", List.of("shipping", "help"), 5);
        verify(searchMapper).insertLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getTopScenarioId()).isEqualTo(10L);
        assertThat(logCaptor.getValue().getSource()).isEqualTo("ADMIN_TEST");
    }

    @Test
    void searchPassesDistinctTokenizedTermsToMapper() {
        SearchResultItem result = new SearchResultItem();
        result.setScenarioId(11L);
        result.setScenarioTitle("Password reset");
        when(piiGuard.detectTypes("password reset password")).thenReturn(List.of());
        when(searchMapper.searchScenarios(
                        "password reset password", List.of("password", "reset"), 20))
                .thenReturn(List.of(result));

        var response = service.search("Password reset password", "ADMIN_TEST", null, null, null);

        assertThat(response.resultCount()).isEqualTo(1);
        verify(searchMapper)
                .searchScenarios("password reset password", List.of("password", "reset"), 20);
    }

    @Test
    void searchCanReturnCrawlDocumentWithoutTopScenario() {
        SearchResultItem result = new SearchResultItem();
        result.setCrawlDocumentId(30L);
        result.setScenarioTitle("Policy document");
        result.setMatchedField("CRAWL_DOCUMENT");
        when(piiGuard.detectTypes("policy")).thenReturn(List.of());
        when(searchMapper.searchScenarios("policy", List.of("policy"), 5))
                .thenReturn(List.of(result));
        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        var response = service.search("policy", "CHAT_FALLBACK", 5, null, "anon");

        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.results().get(0).getMatchedField()).isEqualTo("CRAWL_DOCUMENT");
        verify(searchMapper).insertLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getTopScenarioId()).isNull();
    }

    @Test
    void searchBlocksPiiWithoutSearching() {
        when(piiGuard.detectTypes(
                        "test@example.com 010-1234-5678 900101-1234567 4111-1111-1111-1111"))
                .thenReturn(List.of("EMAIL", "PHONE", "RRN", "CARD"));
        ArgumentCaptor<SearchBlockLog> blockCaptor = ArgumentCaptor.forClass(SearchBlockLog.class);

        assertThatThrownBy(
                        () ->
                                service.search(
                                        "test@example.com 010-1234-5678 900101-1234567 4111-1111-1111-1111",
                                        "ADMIN_TEST",
                                        5,
                                        1L,
                                        null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEARCH_PII_BLOCKED);

        verify(searchMapper).insertBlockLog(blockCaptor.capture());
        assertThat(blockCaptor.getValue().getPiiTypes()).isEqualTo("EMAIL,PHONE,RRN,CARD");
        assertThat(blockCaptor.getValue().getQueryLength())
                .isEqualTo(
                        "test@example.com 010-1234-5678 900101-1234567 4111-1111-1111-1111"
                                .length());
        org.mockito.Mockito.verify(searchMapper, org.mockito.Mockito.never())
                .searchScenarios(any(), any(), org.mockito.Mockito.anyInt());
        org.mockito.Mockito.verify(searchMapper, org.mockito.Mockito.never())
                .insertLog(any(SearchLog.class));
    }

    @Test
    void rebuildPopularReturnsCounts() {
        LocalDate date = LocalDate.of(2026, 5, 27);
        when(searchMapper.deletePopularByDate(date)).thenReturn(2);
        when(searchMapper.rebuildPopularByDate(date)).thenReturn(5);

        var response = service.rebuildPopular(date);

        assertThat(response.deletedBeforeRebuild()).isEqualTo(2);
        assertThat(response.rebuiltCount()).isEqualTo(5);
    }

    @Test
    void deleteExpiredLogsSupportsDryRun() {
        when(searchMapper.countSearchLogsBefore(any(OffsetDateTime.class))).thenReturn(3L);
        when(searchMapper.countBlockLogsBefore(any(OffsetDateTime.class))).thenReturn(2L);

        var response = service.deleteExpiredLogs(90, true);

        assertThat(response.dryRun()).isTrue();
        assertThat(response.deletedSearchLogs()).isZero();
        assertThat(response.deletedBlockLogs()).isZero();
        assertThat(response.wouldDeleteSearchLogs()).isEqualTo(3);
        assertThat(response.wouldDeleteBlockLogs()).isEqualTo(2);
        org.mockito.Mockito.verify(searchMapper, org.mockito.Mockito.never())
                .deleteSearchLogsBefore(any());
        org.mockito.Mockito.verify(searchMapper, org.mockito.Mockito.never())
                .deleteBlockLogsBefore(any());
    }
}
