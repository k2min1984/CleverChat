package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.ScenarioCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioCategoryMapper {
    List<ScenarioCategory> findAll();

    ScenarioCategory findById(@Param("id") Long id);

    void insert(ScenarioCategory category);

    int update(ScenarioCategory category);
}
