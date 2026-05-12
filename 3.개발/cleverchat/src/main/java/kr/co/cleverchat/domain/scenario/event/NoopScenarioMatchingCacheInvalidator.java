package kr.co.cleverchat.domain.scenario.event;

import org.springframework.stereotype.Component;

@Component
public class NoopScenarioMatchingCacheInvalidator implements ScenarioMatchingCacheInvalidator {

    @Override
    public void onScenarioChanged(long scenarioId) {
        // M3에서 ChatMatchingCacheInvalidator로 교체된다.
    }

    @Override
    public void onGlobalKeywordChanged() {
        // M3에서 ChatMatchingCacheInvalidator로 교체된다.
    }
}
