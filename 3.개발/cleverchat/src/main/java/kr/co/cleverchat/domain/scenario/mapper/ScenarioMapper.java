package kr.co.cleverchat.domain.scenario.mapper;

import java.util.List;
import kr.co.cleverchat.domain.scenario.model.Scenario;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScenarioMapper {
    List<Scenario> findAll(@Param("status") String status);

    List<Scenario> findActiveForMatching();

    Scenario findById(@Param("id") Long id);

    void insert(Scenario scenario);

    int update(Scenario scenario);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int activate(@Param("id") Long id, @Param("activeVersionId") Long activeVersionId);
}
