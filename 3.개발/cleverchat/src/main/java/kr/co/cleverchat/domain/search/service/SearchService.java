package kr.co.cleverchat.domain.search.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.chatbot.service.ChatPiiGuard;
import kr.co.cleverchat.domain.search.dto.SearchDtos.PopularRebuildResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.RetentionResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.SearchResponse;
import kr.co.cleverchat.domain.search.mapper.SearchMapper;
import kr.co.cleverchat.domain.search.model.PopularQueryDaily;
import kr.co.cleverchat.domain.search.model.SearchBlockLog;
import kr.co.cleverchat.domain.search.model.SearchLog;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final SearchMapper searchMapper;
    private final ChatPiiGuard piiGuard;

    public SearchService(SearchMapper searchMapper, ChatPiiGuard piiGuard) {
        this.searchMapper = searchMapper;
        this.piiGuard = piiGuard;
    }

    @Transactional
    public SearchResponse search(
            String query, String source, Integer limit, Long userId, String anonymousIdHash) {
        String normalized = normalizeQuery(query);
        List<String> terms = tokenize(normalized);
        List<String> piiTypes = piiGuard.detectTypes(normalized);
        if (!piiTypes.isEmpty()) {
            recordBlock(normalized, source, userId, anonymousIdHash, piiTypes);
            throw new BusinessException(ErrorCode.SEARCH_PII_BLOCKED);
        }
        long startedAt = System.nanoTime();
        List<SearchResultItem> results =
                searchMapper.searchScenarios(normalized, terms, resolveLimit(limit));
        int latencyMs = (int) ((System.nanoTime() - startedAt) / 1_000_000);
        recordSearch(normalized, source, userId, anonymousIdHash, results, latencyMs);
        return new SearchResponse(normalized, results.size(), results);
    }

    @Transactional(readOnly = true)
    public List<SearchLog> logs(String query, String source, Integer limit) {
        return searchMapper.findLogs(
                query == null ? null : query.trim().toLowerCase(Locale.ROOT),
                source,
                resolveLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<SearchBlockLog> blockLogs(String piiType, String source, Integer limit) {
        return searchMapper.findBlockLogs(piiType, source, resolveLimit(limit));
    }

    @Transactional(readOnly = true)
    public List<PopularQueryDaily> popular(LocalDate from, LocalDate to, Integer limit) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo : from;
        return searchMapper.findPopular(resolvedFrom, resolvedTo, resolveLimit(limit));
    }

    @Transactional
    @RequireRole("OPERATOR")
    public PopularRebuildResponse rebuildPopular(LocalDate statDate) {
        int deleted = searchMapper.deletePopularByDate(statDate);
        int rebuilt = searchMapper.rebuildPopularByDate(statDate);
        return new PopularRebuildResponse(statDate, rebuilt, deleted, true, OffsetDateTime.now());
    }

    @Transactional
    @RequireRole("OPERATOR")
    public RetentionResponse deleteExpiredLogs(Integer retentionDays, boolean dryRun) {
        int days = retentionDays == null ? 90 : Math.max(30, retentionDays);
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days);
        long wouldDeleteSearchLogs = searchMapper.countSearchLogsBefore(cutoff);
        long wouldDeleteBlockLogs = searchMapper.countBlockLogsBefore(cutoff);
        int deletedSearchLogs = dryRun ? 0 : searchMapper.deleteSearchLogsBefore(cutoff);
        int deletedBlockLogs = dryRun ? 0 : searchMapper.deleteBlockLogsBefore(cutoff);
        return new RetentionResponse(
                days,
                cutoff,
                deletedSearchLogs,
                deletedBlockLogs,
                wouldDeleteSearchLogs,
                wouldDeleteBlockLogs,
                dryRun);
    }

    private void recordSearch(
            String query,
            String source,
            Long userId,
            String anonymousIdHash,
            List<SearchResultItem> results,
            int latencyMs) {
        SearchLog log = new SearchLog();
        log.setQueryText(query);
        log.setNormalizedQuery(query);
        log.setResultCount(results.size());
        log.setTopScenarioNo(results.isEmpty() ? null : results.get(0).getScenarioNo());
        log.setSource(source);
        log.setLatencyMs(latencyMs);
        log.setUserNo(userId);
        log.setAnonymousIdHash(anonymousIdHash);
        searchMapper.insertLog(log);
    }

    private void recordBlock(
            String query,
            String source,
            Long userId,
            String anonymousIdHash,
            List<String> piiTypes) {
        SearchBlockLog log = new SearchBlockLog();
        log.setQueryLength(query.length());
        log.setPiiTypes(String.join(",", piiTypes));
        log.setSource(source);
        log.setUserNo(userId);
        log.setAnonymousIdHash(anonymousIdHash);
        searchMapper.insertBlockLog(log);
    }

    private String normalizeQuery(String query) {
        String normalized =
                query == null ? "" : query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Search query is empty.");
        }
        if (normalized.length() > 200) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Search query is too long.");
        }
        return normalized;
    }

    private List<String> tokenize(String query) {
        return List.of(query.split(" ")).stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }

    private int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
