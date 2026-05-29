package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioKeywordMapper {
    List<ScenarioKeyword> findByScenarioId(@Param("scenarioId") Long scenarioId);

    List<ScenarioKeyword> findEnabledByScenarioId(@Param("scenarioId") Long scenarioId);

    List<ScenarioKeyword> findEnabledForActiveScenarios();

    void insert(ScenarioKeyword keyword);

    int deleteByScenarioId(@Param("scenarioId") Long scenarioId);
}
