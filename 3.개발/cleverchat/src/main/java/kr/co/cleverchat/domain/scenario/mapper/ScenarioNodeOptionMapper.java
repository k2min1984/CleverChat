package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioNodeOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioNodeOptionMapper {
    List<ScenarioNodeOption> findByNodeId(@Param("nodeId") Long nodeId);

    List<ScenarioNodeOption> findEnabledByNodeId(@Param("nodeId") Long nodeId);

    void insert(ScenarioNodeOption option);

    int deleteByVersionId(@Param("versionId") Long versionId);
}
