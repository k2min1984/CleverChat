package kr.co.cleverchat.domain.scenario.event;

public interface ScenarioMatchingCacheInvalidator {

    void onScenarioChanged(long scenarioId);

    void onGlobalKeywordChanged();
}
