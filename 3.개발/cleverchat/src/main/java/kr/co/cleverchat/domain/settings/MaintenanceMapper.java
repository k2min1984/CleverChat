package kr.co.cleverchat.domain.settings;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MaintenanceMapper {
    @Insert(
            "INSERT INTO tb_maintenance_run(task_key,run_date) VALUES('retention',#{date}) ON CONFLICT DO NOTHING")
    int claim(LocalDate date);

    @Update(
            "UPDATE tb_maintenance_run SET finished_at=now(),message=#{message} WHERE task_key='retention' AND run_date=#{date}")
    void finish(@Param("date") LocalDate date, @Param("message") String message);

    @Insert(
            "INSERT INTO tb_crawl_export_file(target_no,root_path,relative_path,content_sha256) VALUES(#{target},#{root},#{relative},#{hash})")
    void register(
            @Param("target") Long target,
            @Param("root") String root,
            @Param("relative") String relative,
            @Param("hash") String hash);

    @Select(
            "SELECT export_file_no,root_path,relative_path,content_sha256 FROM tb_crawl_export_file WHERE cleaned_at IS NULL AND created_at < #{cutoff} ORDER BY created_at LIMIT 1000")
    List<ExportFile> expired(OffsetDateTime cutoff);

    @Update(
            "UPDATE tb_crawl_export_file SET cleaned_at=now(),cleanup_error=NULL WHERE export_file_no=#{id}")
    void cleaned(long id);

    @Update("UPDATE tb_crawl_export_file SET cleanup_error=#{message} WHERE export_file_no=#{id}")
    void failed(@Param("id") long id, @Param("message") String message);

    @Select(
            "SELECT to_char(started_at AT TIME ZONE 'Asia/Seoul','YYYY-MM-DD HH24:MI') || ' · ' || coalesce(message,'실행 중 또는 결과 확인 필요') FROM tb_maintenance_run WHERE task_key='retention' ORDER BY run_date DESC LIMIT 1")
    String lastResult();

    record ExportFile(
            long exportFileNo, String rootPath, String relativePath, String contentSha256) {}
}
