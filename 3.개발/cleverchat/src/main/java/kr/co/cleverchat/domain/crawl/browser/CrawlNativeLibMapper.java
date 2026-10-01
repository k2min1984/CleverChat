package kr.co.cleverchat.domain.crawl.browser;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CrawlNativeLibMapper {
    void insert(CrawlNativeLibBundle bundle);

    CrawlNativeLibBundle findById(@Param("id") Long id);

    List<CrawlNativeLibBundle> findRecent(@Param("limit") int limit);

    int markActive(
            @Param("id") Long id,
            @Param("activePath") String activePath,
            @Param("activatedBy") Long activatedBy,
            @Param("message") String message);

    int deactivateOthers(@Param("id") Long id);
}
