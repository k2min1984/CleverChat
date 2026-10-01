package kr.co.cleverchat.domain.search.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.common.search.KoreanMorphAnalyzer;
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
import kr.co.cleverchat.domain.settings.RuntimeSetting;
import kr.co.cleverchat.domain.settings.RuntimeSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RuntimeSettingsService runtimeSettings;

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_TERMS = 12;
    // Nori retains these request phrases as content words. They express the request to answer,
    // not its subject ("전기요금 알려주세요" should search for 전기 + 요금).
    private static final java.util.Set<String> REQUEST_TERMS = java.util.Set.of(
            "알려", "알리", "알려줘", "알려주세요", "주세요", "궁금", "어떻", "어떻게",
            "무엇", "대해", "대하", "설명", "부탁");
    private static final java.util.regex.Pattern SINGLE_HANGUL_TOKEN =
            java.util.regex.Pattern.compile("\\p{IsHangul}");

    private final SearchMapper searchMapper;
    private final ChatPiiGuard piiGuard;
    private final KoreanMorphAnalyzer morphAnalyzer;

    public SearchService(SearchMapper searchMapper, ChatPiiGuard piiGuard) {
        this(searchMapper, piiGuard, new KoreanMorphAnalyzer());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SearchService(
            SearchMapper searchMapper, ChatPiiGuard piiGuard, KoreanMorphAnalyzer morphAnalyzer) {
        this.searchMapper = searchMapper;
        this.piiGuard = piiGuard;
        this.morphAnalyzer = morphAnalyzer;
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
        String tokenQuery = String.join(" ", terms);
        long startedAt = System.nanoTime();
        List<SearchResultItem> results =
                searchMapper.searchScenarios(normalized, terms, tokenQuery, resolveLimit(limit));
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
        int days =
                retentionDays == null
                        ? (runtimeSettings == null
                                ? 90
                                : runtimeSettings
                                        .current()
                                        .integer(RuntimeSetting.SEARCH_RETENTION_DAYS))
                        : Math.max(30, retentionDays);
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

    /**
     * Splits the query into Korean morphemes (via Lucene Nori) so that query terms line up with the
     * morpheme tokens indexed at crawl time and with the stems of scenario/keyword/node text. Falls
     * back to whitespace splitting when morphological analysis yields nothing (e.g. symbol-only
     * input), so non-Korean queries keep working.
     */
    private List<String> tokenize(String query) {
        List<String> morphemes =
                morphAnalyzer.tokens(query).stream()
                        .map(String::trim)
                        .filter(this::isUsefulSearchTerm)
                        .distinct()
                        .limit(MAX_TERMS)
                        .toList();
        if (!morphemes.isEmpty()) {
            return morphemes;
        }
        return List.of(query.split(" ")).stream()
                .map(String::trim)
                .filter(this::isUsefulSearchTerm)
                .distinct()
                .limit(MAX_TERMS)
                .toList();
    }

    private boolean isUsefulSearchTerm(String term) {
        return term != null && !term.isBlank()
                && !SINGLE_HANGUL_TOKEN.matcher(term).matches()
                && !REQUEST_TERMS.contains(term);
    }

    private int resolveLimit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
