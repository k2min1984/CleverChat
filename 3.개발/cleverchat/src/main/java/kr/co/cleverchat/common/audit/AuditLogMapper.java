package kr.co.cleverchat.common.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper {

    void insert(
        @Param("actor") String actor,
        @Param("action") String action,
        @Param("targetType") String targetType,
        @Param("targetId") String targetId,
        @Param("detail") String detail,
        @Param("ip") String ip
    );
}
