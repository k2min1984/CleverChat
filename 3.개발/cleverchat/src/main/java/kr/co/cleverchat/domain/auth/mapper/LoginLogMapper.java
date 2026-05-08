package kr.co.cleverchat.domain.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginLogMapper {

    void insert(
        @Param("username") String username,
        @Param("success") boolean success,
        @Param("failureMsg") String failureMsg,
        @Param("ip") String ip,
        @Param("userAgent") String userAgent
    );
}
