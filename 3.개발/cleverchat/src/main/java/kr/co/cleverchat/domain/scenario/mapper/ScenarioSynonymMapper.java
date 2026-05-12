package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.dto.ScenarioSynonymRow;
import kr.co.cleverchat.domain.scenario.model.ScenarioSynonym;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioSynonymMapper {
    List<ScenarioSynonym> findByKeywordId(@Param("keywordId") Long keywordId);
    List<ScenarioSynonymRow> findEnabledByScenarioId(@Param("scenarioId") Long scenarioId);
    List<ScenarioSynonymRow> findEnabledForActiveScenarios();
    void insert(ScenarioSynonym synonym);
}
