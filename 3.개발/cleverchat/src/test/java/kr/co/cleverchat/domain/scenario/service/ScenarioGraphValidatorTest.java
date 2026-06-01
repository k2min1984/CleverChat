package kr.co.cleverchat.domain.scenario.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.NodeRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.OptionRequest;
import kr.co.cleverchat.domain.scenario.dto.ScenarioGraphDtos.SaveRequest;
import org.junit.jupiter.api.Test;

class ScenarioGraphValidatorTest {

    private final ScenarioGraphValidator validator = new ScenarioGraphValidator();

    @Test
    void rejectsMissingStartNode() {
        SaveRequest request = new SaveRequest("missing", List.of(node("start", "END")));

        assertThatThrownBy(() -> validator.validateForSave(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsEndNodeWithOptions() {
        SaveRequest request =
                new SaveRequest(
                        "end",
                        List.of(
                                new NodeRequest(
                                        "end",
                                        "END",
                                        "종료",
                                        null,
                                        0,
                                        "{}",
                                        List.of(option("다음", null)))));

        assertThatThrownBy(() -> validator.validateForSave(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsUnreachableNodeOnPublish() {
        SaveRequest request =
                new SaveRequest("start", List.of(node("start", "END"), node("orphan", "END")));

        assertThatThrownBy(() -> validator.validateForPublish(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void acceptsReachableGraph() {
        SaveRequest request =
                new SaveRequest(
                        "start",
                        List.of(
                                new NodeRequest(
                                        "start",
                                        "QUESTION",
                                        "시작",
                                        null,
                                        0,
                                        "{}",
                                        List.of(option("끝", "end"))),
                                node("end", "END")));

        assertThatCode(() -> validator.validateForPublish(request)).doesNotThrowAnyException();
    }

    private NodeRequest node(String key, String type) {
        return new NodeRequest(key, type, key, null, 0, "{}", List.of());
    }

    private OptionRequest option(String label, String nextNodeKey) {
        return new OptionRequest(label, nextNodeKey, null, 0, "Y");
    }
}
