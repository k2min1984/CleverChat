package kr.co.cleverchat.domain.search.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import kr.co.cleverchat.domain.search.model.PopularQueryDaily;
import kr.co.cleverchat.domain.search.model.SearchBlockLog;
import kr.co.cleverchat.domain.search.model.SearchLog;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchMapper {
    List<SearchResultItem> searchScenarios(
            @Param("query") String query,
            @Param("terms") List<String> terms,
            @Param("tokenQuery") String tokenQuery,
            @Param("limit") int limit);

    void insertLog(SearchLog log);

    void insertBlockLog(SearchBlockLog log);

    List<SearchLog> findLogs(
            @Param("query") String query,
            @Param("source") String source,
            @Param("limit") int limit);

    List<SearchBlockLog> findBlockLogs(
            @Param("piiType") String piiType,
            @Param("source") String source,
            @Param("limit") int limit);

    List<PopularQueryDaily> findPopular(
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("limit") int limit);

    int deletePopularByDate(@Param("statDate") LocalDate statDate);

    int rebuildPopularByDate(@Param("statDate") LocalDate statDate);

    long countSearchLogsBefore(@Param("cutoff") OffsetDateTime cutoff);

    long countBlockLogsBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteSearchLogsBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteBlockLogsBefore(@Param("cutoff") OffsetDateTime cutoff);
}
