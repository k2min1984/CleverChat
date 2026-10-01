package kr.co.cleverchat.domain.chatbot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import kr.co.cleverchat.domain.scenario.dto.ScenarioSynonymRow;
import kr.co.cleverchat.domain.scenario.event.ScenarioMatchingCacheInvalidator;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioKeywordMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioSynonymMapper;
import kr.co.cleverchat.domain.scenario.model.ScenarioKeyword;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import org.springframework.stereotype.Service;

@Service
public class ScenarioMatchingService implements ScenarioMatchingCacheInvalidator {

    private static final double THRESHOLD = 50.0;
    private static final Duration MATCHING_CACHE_TTL = Duration.ofSeconds(300);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("\\p{Cntrl}");
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\p{IsHangul}a-z0-9\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioKeywordMapper keywordMapper;
    private final ScenarioSynonymMapper synonymMapper;
    private final Cache<Long, List<ScenarioNodeOption>> optionCache = matchingCache();
    private final Cache<Long, List<ScenarioKeyword>> scenarioKeywordCache = matchingCache();
    private final Cache<Long, List<ScenarioSynonymRow>> scenarioSynonymCache = matchingCache();
    private final Cache<String, List<ScenarioKeyword>> globalKeywordCache = matchingCache();
    private final Cache<String, List<ScenarioSynonymRow>> globalSynonymCache = matchingCache();

    public ScenarioMatchingService(
            ScenarioNodeOptionMapper optionMapper,
            ScenarioKeywordMapper keywordMapper,
            ScenarioSynonymMapper synonymMapper) {
        this.optionMapper = optionMapper;
        this.keywordMapper = keywordMapper;
        this.synonymMapper = synonymMapper;
    }

    public Optional<MatchResult> match(Long currentScenarioId, Long currentNodeId, String input) {
        String normalizedInput = normalize(input);
        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }
        Optional<MatchResult> optionMatch =
                matchCurrentOptions(currentScenarioId, currentNodeId, normalizedInput);
        if (optionMatch.isPresent()) {
            return optionMatch;
        }

        return bestKeywordMatch(currentScenarioId, normalizedInput)
                .filter(result -> result.score() >= THRESHOLD);
    }

    public Optional<MatchResult> matchInitialScenario(String input) {
        String normalizedInput = normalize(input);
        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }
        return bestGlobalKeywordMatch(normalizedInput)
                .filter(result -> result.score() >= THRESHOLD);
    }

    private Optional<MatchResult> matchCurrentOptions(Long scenarioId, Long nodeId, String input) {
        if (nodeId == null) {
            return Optional.empty();
        }
        List<ScenarioNodeOption> options =
                optionCache.get(nodeId, optionMapper::findEnabledByNodeId);
        return options.stream()
                .map(option -> optionMatch(scenarioId, option, input))
                .flatMap(Optional::stream)
                .max(matchComparator());
    }

    private Optional<MatchResult> optionMatch(
            Long scenarioId, ScenarioNodeOption option, String input) {
        String label = normalize(option.getLabel());
        if (label.equals(input)) {
            return Optional.of(
                    new MatchResult(
                            scenarioId,
                            option.getScenarioNodeOptionNo(),
                            option.getNextNodeNo(),
                            100.0,
                            100,
                            option.getSortOrder(),
                            MatchType.OPTION));
        }
        if (label.contains(input) || input.contains(label)) {
            return Optional.of(
                    new MatchResult(
                            scenarioId,
                            option.getScenarioNodeOptionNo(),
                            option.getNextNodeNo(),
                            85.0,
                            85,
                            option.getSortOrder(),
                            MatchType.OPTION));
        }
        return Optional.empty();
    }

    private Optional<MatchResult> bestKeywordMatch(Long currentScenarioId, String input) {
        Optional<MatchResult> currentKeyword =
                scenarioKeywordCache
                        .get(currentScenarioId, keywordMapper::findEnabledByScenarioId)
                        .stream()
                        .filter(keyword -> normalize(keyword.getKeyword()).equals(input))
                        .map(keyword -> keywordResult(keyword, 80.0, true))
                        .max(matchComparator());
        Optional<MatchResult> currentSynonym =
                scenarioSynonymCache
                        .get(currentScenarioId, synonymMapper::findEnabledByScenarioId)
                        .stream()
                        .filter(row -> normalize(row.getSynonym()).equals(input))
                        .map(row -> synonymResult(row, 70.0, true))
                        .max(matchComparator());
        Optional<MatchResult> globalKeyword =
                globalKeywordCache
                        .get("active", ignored -> keywordMapper.findEnabledForActiveScenarios())
                        .stream()
                        .filter(keyword -> normalize(keyword.getKeyword()).equals(input))
                        .map(
                                keyword ->
                                        keywordResult(
                                                keyword,
                                                60.0,
                                                currentScenarioId.equals(keyword.getScenarioNo())))
                        .max(matchComparator());
        Optional<MatchResult> globalSynonym =
                globalSynonymCache
                        .get("active", ignored -> synonymMapper.findEnabledForActiveScenarios())
                        .stream()
                        .filter(row -> normalize(row.getSynonym()).equals(input))
                        .map(
                                row ->
                                        synonymResult(
                                                row,
                                                50.0,
                                                currentScenarioId.equals(row.getScenarioNo())))
                        .max(matchComparator());

        return List.of(currentKeyword, currentSynonym, globalKeyword, globalSynonym).stream()
                .flatMap(Optional::stream)
                .max(matchComparator());
    }

    private Optional<MatchResult> bestGlobalKeywordMatch(String input) {
        Optional<MatchResult> globalKeyword =
                globalKeywordCache
                        .get("active", ignored -> keywordMapper.findEnabledForActiveScenarios())
                        .stream()
                        .map(keyword -> initialKeywordResult(keyword, input))
                        .flatMap(Optional::stream)
                        .max(matchComparator());
        Optional<MatchResult> globalSynonym =
                globalSynonymCache
                        .get("active", ignored -> synonymMapper.findEnabledForActiveScenarios())
                        .stream()
                        .map(row -> initialSynonymResult(row, input))
                        .flatMap(Optional::stream)
                        .max(matchComparator());

        return List.of(globalKeyword, globalSynonym).stream()
                .flatMap(Optional::stream)
                .max(matchComparator());
    }

    private Optional<MatchResult> initialKeywordResult(ScenarioKeyword keyword, String input) {
        String normalizedKeyword = normalize(keyword.getKeyword());
        if (normalizedKeyword.isBlank()) {
            return Optional.empty();
        }
        if (normalizedKeyword.equals(input)) {
            return Optional.of(keywordResult(keyword, 60.0, false));
        }
        if (containsWholeTerm(input, normalizedKeyword)) {
            return Optional.of(keywordResult(keyword, 55.0, false));
        }
        return Optional.empty();
    }

    private Optional<MatchResult> initialSynonymResult(ScenarioSynonymRow row, String input) {
        String normalizedSynonym = normalize(row.getSynonym());
        if (normalizedSynonym.isBlank()) {
            return Optional.empty();
        }
        if (normalizedSynonym.equals(input)) {
            return Optional.of(synonymResult(row, 50.0, false));
        }
        if (containsWholeTerm(input, normalizedSynonym)) {
            return Optional.of(synonymResult(row, 50.0, false));
        }
        return Optional.empty();
    }

    private boolean containsWholeTerm(String input, String term) {
        if (input == null || term == null || term.isBlank() || input.equals(term)) {
            return false;
        }
        for (String token : input.split("\\s+")) {
            if (token.equals(term)) {
                return true;
            }
        }
        return false;
    }

    private MatchResult keywordResult(
            ScenarioKeyword keyword, double baseScore, boolean currentScenario) {
        double score = baseScore * keyword.getWeight() / 100.0;
        return new MatchResult(
                keyword.getScenarioNo(),
                null,
                null,
                score,
                keyword.getWeight(),
                currentScenario ? 0 : 1,
                MatchType.KEYWORD);
    }

    private MatchResult synonymResult(
            ScenarioSynonymRow row, double baseScore, boolean currentScenario) {
        double score = baseScore * row.getWeight() / 100.0;
        return new MatchResult(
                row.getScenarioNo(),
                null,
                null,
                score,
                row.getWeight(),
                currentScenario ? 0 : 1,
                MatchType.SYNONYM);
    }

    private Comparator<MatchResult> matchComparator() {
        return Comparator.comparingDouble(MatchResult::score)
                .thenComparing(MatchResult::weight)
                .thenComparing(Comparator.comparingInt(MatchResult::sortOrder).reversed())
                .thenComparing(result -> -result.scenarioId());
    }

    private String normalize(String value) {
        String normalized =
                Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC)
                        .toLowerCase();
        normalized = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = CONTROL_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NON_WORD_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    @Override
    public void onScenarioChanged(long scenarioId) {
        optionCache.invalidateAll();
        scenarioKeywordCache.invalidate(scenarioId);
        scenarioSynonymCache.invalidate(scenarioId);
        onGlobalKeywordChanged();
    }

    @Override
    public void onGlobalKeywordChanged() {
        globalKeywordCache.invalidateAll();
        globalSynonymCache.invalidateAll();
    }

    private static <K, V> Cache<K, V> matchingCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(MATCHING_CACHE_TTL)
                .maximumSize(1_000)
                .build();
    }

    public enum MatchType {
        OPTION,
        KEYWORD,
        SYNONYM
    }

    public record MatchResult(
            Long scenarioId,
            Long optionId,
            Long nextNodeId,
            double score,
            int weight,
            int sortOrder,
            MatchType matchType) {}
}
