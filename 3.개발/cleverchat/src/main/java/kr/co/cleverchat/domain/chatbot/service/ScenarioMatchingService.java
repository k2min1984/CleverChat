package kr.co.cleverchat.domain.chatbot.service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import kr.co.cleverchat.domain.scenario.dto.ScenarioSynonymRow;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioKeywordMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioSynonymMapper;
import kr.co.cleverchat.domain.scenario.model.ScenarioKeyword;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import org.springframework.stereotype.Service;

@Service
public class ScenarioMatchingService {

    private static final double THRESHOLD = 50.0;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("\\p{Cntrl}");
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\p{IsHangul}a-z0-9\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final ScenarioNodeOptionMapper optionMapper;
    private final ScenarioKeywordMapper keywordMapper;
    private final ScenarioSynonymMapper synonymMapper;

    public ScenarioMatchingService(
        ScenarioNodeOptionMapper optionMapper,
        ScenarioKeywordMapper keywordMapper,
        ScenarioSynonymMapper synonymMapper
    ) {
        this.optionMapper = optionMapper;
        this.keywordMapper = keywordMapper;
        this.synonymMapper = synonymMapper;
    }

    public Optional<MatchResult> match(Long currentScenarioId, Long currentNodeId, String input) {
        String normalizedInput = normalize(input);
        if (normalizedInput.isBlank()) {
            return Optional.empty();
        }
        Optional<MatchResult> optionMatch = matchCurrentOptions(currentScenarioId, currentNodeId, normalizedInput);
        if (optionMatch.isPresent()) {
            return optionMatch;
        }

        return bestKeywordMatch(currentScenarioId, normalizedInput)
            .filter(result -> result.score() >= THRESHOLD);
    }

    private Optional<MatchResult> matchCurrentOptions(Long scenarioId, Long nodeId, String input) {
        if (nodeId == null) {
            return Optional.empty();
        }
        List<ScenarioNodeOption> options = optionMapper.findEnabledByNodeId(nodeId);
        return options.stream()
            .map(option -> optionMatch(scenarioId, option, input))
            .flatMap(Optional::stream)
            .max(matchComparator());
    }

    private Optional<MatchResult> optionMatch(Long scenarioId, ScenarioNodeOption option, String input) {
        String label = normalize(option.getLabel());
        if (label.equals(input)) {
            return Optional.of(new MatchResult(scenarioId, option.getId(), option.getNextNodeId(), 100.0, 100, option.getSortOrder(), MatchType.OPTION));
        }
        if (label.contains(input) || input.contains(label)) {
            return Optional.of(new MatchResult(scenarioId, option.getId(), option.getNextNodeId(), 85.0, 85, option.getSortOrder(), MatchType.OPTION));
        }
        return Optional.empty();
    }

    private Optional<MatchResult> bestKeywordMatch(Long currentScenarioId, String input) {
        Optional<MatchResult> currentKeyword = keywordMapper.findEnabledByScenarioId(currentScenarioId).stream()
            .filter(keyword -> normalize(keyword.getKeyword()).equals(input))
            .map(keyword -> keywordResult(keyword, 80.0, true))
            .max(matchComparator());
        Optional<MatchResult> currentSynonym = synonymMapper.findEnabledByScenarioId(currentScenarioId).stream()
            .filter(row -> normalize(row.getSynonym()).equals(input))
            .map(row -> synonymResult(row, 70.0, true))
            .max(matchComparator());
        Optional<MatchResult> globalKeyword = keywordMapper.findEnabledForActiveScenarios().stream()
            .filter(keyword -> normalize(keyword.getKeyword()).equals(input))
            .map(keyword -> keywordResult(keyword, 60.0, currentScenarioId.equals(keyword.getScenarioId())))
            .max(matchComparator());
        Optional<MatchResult> globalSynonym = synonymMapper.findEnabledForActiveScenarios().stream()
            .filter(row -> normalize(row.getSynonym()).equals(input))
            .map(row -> synonymResult(row, 50.0, currentScenarioId.equals(row.getScenarioId())))
            .max(matchComparator());

        return List.of(currentKeyword, currentSynonym, globalKeyword, globalSynonym).stream()
            .flatMap(Optional::stream)
            .max(matchComparator());
    }

    private MatchResult keywordResult(ScenarioKeyword keyword, double baseScore, boolean currentScenario) {
        double score = baseScore * keyword.getWeight() / 100.0;
        return new MatchResult(keyword.getScenarioId(), null, null, score, keyword.getWeight(), currentScenario ? 0 : 1, MatchType.KEYWORD);
    }

    private MatchResult synonymResult(ScenarioSynonymRow row, double baseScore, boolean currentScenario) {
        double score = baseScore * row.getWeight() / 100.0;
        return new MatchResult(row.getScenarioId(), null, null, score, row.getWeight(), currentScenario ? 0 : 1, MatchType.SYNONYM);
    }

    private Comparator<MatchResult> matchComparator() {
        return Comparator.comparingDouble(MatchResult::score)
            .thenComparing(MatchResult::weight)
            .thenComparing(Comparator.comparingInt(MatchResult::sortOrder).reversed())
            .thenComparing(result -> -result.scenarioId());
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC)
            .toLowerCase();
        normalized = HTML_TAG_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = CONTROL_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NON_WORD_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ")
            .trim();
        return normalized;
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
        MatchType matchType
    ) {
    }
}
