package kr.co.cleverchat.domain.settings;

import org.apache.ibatis.annotations.*;

@Mapper
public interface GatewayUserMapper {
    @Select(
            """
        INSERT INTO tb_user(username,password_hash,display_name,use_yn,must_change_password,auth_source,emp_no,unit)
        VALUES('gw_' || md5(#{empNo}),NULL,#{name},'Y',FALSE,'GATEWAY',#{empNo},#{unit})
        ON CONFLICT (emp_no) WHERE auth_source='GATEWAY' DO UPDATE
        SET display_name=EXCLUDED.display_name,unit=EXCLUDED.unit
        RETURNING user_no,emp_no,display_name,use_yn
        """)
    @Options(flushCache = Options.FlushCachePolicy.TRUE, useCache = false)
    Account provision(
            @Param("empNo") String empNo, @Param("name") String name, @Param("unit") String unit);

    record Account(Long userNo, String empNo, String displayName, String useYn) {}
}
