package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kr.co.cleverchat.domain.chatbot.service.ScenarioMatchingService.MatchResult;
import kr.co.cleverchat.domain.chatbot.service.ScenarioMatchingService.MatchType;
import kr.co.cleverchat.domain.scenario.dto.ScenarioSynonymRow;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioKeywordMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioNodeOptionMapper;
import kr.co.cleverchat.domain.scenario.mapper.ScenarioSynonymMapper;
import kr.co.cleverchat.domain.scenario.model.ScenarioKeyword;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScenarioMatchingServiceTest {

    @Mock ScenarioNodeOptionMapper optionMapper;

    @Mock ScenarioKeywordMapper keywordMapper;

    @Mock ScenarioSynonymMapper synonymMapper;

    @InjectMocks ScenarioMatchingService service;

    @Test
    void optionLabelExactMatchScores100() {
        when(optionMapper.findEnabledByNodeId(10L))
                .thenReturn(List.of(option(1L, 20L, "예약 확인", 1)));

        Optional<MatchResult> result = service.match(100L, 10L, "예약 확인");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(100.0);
        assertThat(result.get().optionId()).isEqualTo(1L);
        assertThat(result.get().nextNodeId()).isEqualTo(20L);
        assertThat(result.get().matchType()).isEqualTo(MatchType.OPTION);
    }

    @Test
    void optionLabelPartialMatchScores85() {
        when(optionMapper.findEnabledByNodeId(10L))
                .thenReturn(List.of(option(1L, 20L, "예약 확인하기", 1)));

        Optional<MatchResult> result = service.match(100L, 10L, "예약 확인");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(85.0);
    }

    @Test
    void optionsOutsideCurrentEnabledNodeAreExcludedByMapperResult() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "비활성 옵션");

        assertThat(result).isEmpty();
    }

    @Test
    void currentScenarioKeywordExactMatchScores80AtWeight100() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L))
                .thenReturn(List.of(keyword(100L, "배송", 100)));
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "배송");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(80.0);
        assertThat(result.get().matchType()).isEqualTo(MatchType.KEYWORD);
    }

    @Test
    void currentScenarioSynonymMatchAppliesWeight() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(synonymMapper.findEnabledByScenarioId(100L))
                .thenReturn(List.of(synonym(100L, "운송", 80)));
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "운송");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(56.0);
        assertThat(result.get().matchType()).isEqualTo(MatchType.SYNONYM);
    }

    @Test
    void globalKeywordMatchAppliesWeight() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios())
                .thenReturn(List.of(keyword(200L, "환불", 100)));
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "환불");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(60.0);
        assertThat(result.get().scenarioId()).isEqualTo(200L);
    }

    @Test
    void globalKeywordBelowThresholdReturnsEmpty() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios())
                .thenReturn(List.of(keyword(200L, "환불", 80)));
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "환불");

        assertThat(result).isEmpty();
    }

    @Test
    void optionBeatsKeywordCandidate() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of(option(1L, 20L, "환불", 1)));

        Optional<MatchResult> result = service.match(100L, 10L, "환불");

        assertThat(result).isPresent();
        assertThat(result.get().matchType()).isEqualTo(MatchType.OPTION);
        assertThat(result.get().score()).isEqualTo(100.0);
    }

    @Test
    void normalizeHandlesFullWidthCaseAndHtml() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L))
                .thenReturn(List.of(keyword(100L, "abc 테스트", 100)));
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, 10L, "ＡＢＣ <b>테스트</b>!!!");

        assertThat(result).isPresent();
        assertThat(result.get().score()).isEqualTo(80.0);
    }

    @Test
    void nullCurrentNodeSkipsOptionMatching() {
        when(keywordMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios())
                .thenReturn(List.of(keyword(200L, "문의", 100)));
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.match(100L, null, "문의");

        assertThat(result).isPresent();
        assertThat(result.get().scenarioId()).isEqualTo(200L);
    }

    @Test
    void initialScenarioMatchAllowsKeywordContainedInQuestion() {
        when(keywordMapper.findEnabledForActiveScenarios())
                .thenReturn(List.of(keyword(200L, "요금", 100)));
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.matchInitialScenario("요금 알려줘");

        assertThat(result).isPresent();
        assertThat(result.get().scenarioId()).isEqualTo(200L);
        assertThat(result.get().matchType()).isEqualTo(MatchType.KEYWORD);
    }

    @Test
    void initialScenarioMatchIgnoresEmbeddedShortKeywordInsideName() {
        when(keywordMapper.findEnabledForActiveScenarios())
                .thenReturn(List.of(keyword(200L, "상호", 100)));
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        Optional<MatchResult> result = service.matchInitialScenario("김상호");

        assertThat(result).isEmpty();
    }

    @Test
    void blankAndNullInputNeverMatch() {
        assertThat(service.match(100L, 10L, "")).isEmpty();
        assertThat(service.match(100L, 10L, "   ")).isEmpty();
        assertThat(service.match(100L, 10L, null)).isEmpty();
        verifyNoInteractions(optionMapper, keywordMapper, synonymMapper);
    }

    @Test
    void matchingDataIsCachedUntilInvalidated() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L))
                .thenReturn(List.of(keyword(100L, "cache", 100)));
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        assertThat(service.match(100L, 10L, "cache")).isPresent();
        assertThat(service.match(100L, 10L, "cache")).isPresent();

        verify(optionMapper, times(1)).findEnabledByNodeId(10L);
        verify(keywordMapper, times(1)).findEnabledByScenarioId(100L);
        verify(synonymMapper, times(1)).findEnabledByScenarioId(100L);
        verify(keywordMapper, times(1)).findEnabledForActiveScenarios();
        verify(synonymMapper, times(1)).findEnabledForActiveScenarios();
    }

    @Test
    void scenarioInvalidationClearsScenarioAndGlobalCaches() {
        when(optionMapper.findEnabledByNodeId(10L)).thenReturn(List.of());
        when(keywordMapper.findEnabledByScenarioId(100L))
                .thenReturn(List.of(keyword(100L, "cache", 100)));
        when(synonymMapper.findEnabledByScenarioId(100L)).thenReturn(List.of());
        when(keywordMapper.findEnabledForActiveScenarios()).thenReturn(List.of());
        when(synonymMapper.findEnabledForActiveScenarios()).thenReturn(List.of());

        assertThat(service.match(100L, 10L, "cache")).isPresent();
        service.onScenarioChanged(100L);
        assertThat(service.match(100L, 10L, "cache")).isPresent();

        verify(optionMapper, times(2)).findEnabledByNodeId(10L);
        verify(keywordMapper, times(2)).findEnabledByScenarioId(100L);
        verify(synonymMapper, times(2)).findEnabledByScenarioId(100L);
        verify(keywordMapper, times(2)).findEnabledForActiveScenarios();
        verify(synonymMapper, times(2)).findEnabledForActiveScenarios();
    }

    private ScenarioNodeOption option(Long id, Long nextNodeId, String label, int sortOrder) {
        ScenarioNodeOption option = new ScenarioNodeOption();
        option.setScenarioNodeOptionNo(id);
        option.setNextNodeNo(nextNodeId);
        option.setLabel(label);
        option.setSortOrder(sortOrder);
        option.setUseYn("Y");
        return option;
    }

    private ScenarioKeyword keyword(Long scenarioId, String value, int weight) {
        ScenarioKeyword keyword = new ScenarioKeyword();
        keyword.setScenarioNo(scenarioId);
        keyword.setKeyword(value);
        keyword.setWeight(weight);
        keyword.setUseYn("Y");
        return keyword;
    }

    private ScenarioSynonymRow synonym(Long scenarioId, String value, int weight) {
        ScenarioSynonymRow row = new ScenarioSynonymRow();
        row.setScenarioNo(scenarioId);
        row.setSynonym(value);
        row.setWeight(weight);
        return row;
    }
}
