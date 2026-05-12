package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioVersionMapper {
    List<ScenarioVersion> findByScenarioId(@Param("scenarioId") Long scenarioId);
    ScenarioVersion findById(@Param("id") Long id);
    ScenarioVersion findPublishedByScenarioId(@Param("scenarioId") Long scenarioId);
    int nextVersionNo(@Param("scenarioId") Long scenarioId);
    void insert(ScenarioVersion version);
    int setStartNode(@Param("id") Long id, @Param("startNodeId") Long startNodeId);
    int archivePublished(@Param("scenarioId") Long scenarioId);
    int publish(@Param("id") Long id);
}
