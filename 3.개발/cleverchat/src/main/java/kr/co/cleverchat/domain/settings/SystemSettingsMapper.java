package kr.co.cleverchat.domain.settings;

import org.apache.ibatis.annotations.*;

@Mapper
public interface SystemSettingsMapper {
    @Select("SELECT runtime_settings::text FROM tb_system_settings WHERE id=1")
    String getRuntimeJson();

    @Update(
            "UPDATE tb_system_settings SET runtime_settings=CAST(#{json} AS jsonb),version=version+1,updated_by=#{actor},updated_at=now() WHERE id=1 AND version=#{version}")
    int updateRuntime(
            @Param("json") String json,
            @Param("version") long version,
            @Param("actor") String actor);

    @Select(
            "SELECT operation_mode, gateway_url, service_id, crawl_export_directory, version, auth_version, updated_by, updated_at FROM tb_system_settings WHERE id=1")
    SystemSettings get();

    @Select(
            "SELECT count(*) FROM tb_crawl_target WHERE json_export_enabled=TRUE AND (json_export_directory IS NULL OR trim(json_export_directory)='')")
    long inheritedExportTargets();

    @Update(
            """
        UPDATE tb_system_settings SET operation_mode=#{value.operationMode}, gateway_url=#{value.gatewayUrl},
          service_id=#{value.serviceId}, crawl_export_directory=#{value.crawlExportDirectory},
          auth_version=auth_version + CASE WHEN operation_mode <> #{value.operationMode}
            OR (operation_mode='GATEWAY' AND (gateway_url IS DISTINCT FROM #{value.gatewayUrl} OR service_id <> #{value.serviceId})) THEN 1 ELSE 0 END,
          version=version+1, updated_by=#{actor}, updated_at=now()
        WHERE id=1 AND version=#{value.version}
        """)
    int update(@Param("value") SystemSettings value, @Param("actor") String actor);
}
