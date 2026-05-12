package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioNodeMapper {
    List<ScenarioNode> findByVersionId(@Param("versionId") Long versionId);
    ScenarioNode findById(@Param("id") Long id);
    void insert(ScenarioNode node);
    int deleteByVersionId(@Param("versionId") Long versionId);
}
