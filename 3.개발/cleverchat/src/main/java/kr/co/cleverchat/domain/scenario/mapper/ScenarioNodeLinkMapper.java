package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioNodeLinkMapper {
    List<ScenarioNodeLink> findByNodeId(@Param("nodeId") Long nodeId);

    List<ScenarioNodeLink> findEnabledByNodeId(@Param("nodeId") Long nodeId);

    void insert(ScenarioNodeLink link);

    int deleteByVersionId(@Param("versionId") Long versionId);
}
