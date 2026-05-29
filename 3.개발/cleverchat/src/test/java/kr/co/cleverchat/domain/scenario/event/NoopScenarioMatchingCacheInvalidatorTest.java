package kr.co.cleverchat.domain.scenario.event;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class NoopScenarioMatchingCacheInvalidatorTest {

    private final ScenarioMatchingCacheInvalidator invalidator =
            new NoopScenarioMatchingCacheInvalidator();

    @Test
    void acceptsScenarioChangeWithoutSideEffect() {
        assertThatCode(() -> invalidator.onScenarioChanged(1L)).doesNotThrowAnyException();
    }

    @Test
    void acceptsGlobalKeywordChangeWithoutSideEffect() {
        assertThatCode(invalidator::onGlobalKeywordChanged).doesNotThrowAnyException();
    }
}
