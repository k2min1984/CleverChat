package kr.co.cleverchat.domain.adminmanage.mapper;

import java.util.List;
import kr.co.cleverchat.domain.adminmanage.model.AdminIpWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminIpWhitelistMapper {
    List<AdminIpWhitelist> findActive();

    List<AdminIpWhitelist> findAll();

    AdminIpWhitelist findById(@Param("id") Long id);

    void insert(AdminIpWhitelist entry);

    int update(AdminIpWhitelist entry);

    int disable(
            @Param("id") Long id, @Param("actor") String actor, @Param("clientIp") String clientIp);
}
