ALTER TABLE tb_crawl_target
    ADD COLUMN json_export_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN json_export_directory VARCHAR(1000),
    ADD CONSTRAINT ck_crawl_target_json_export_directory
        CHECK (NOT json_export_enabled OR
            (json_export_directory IS NOT NULL AND length(trim(json_export_directory)) > 0));

COMMENT ON COLUMN tb_crawl_target.json_export_enabled IS '수집 페이지 JSON 파일 저장 여부';
COMMENT ON COLUMN tb_crawl_target.json_export_directory IS 'JSON 저장 서버 절대 폴더 경로';

ALTER TABLE tb_crawl_run_log
    ADD COLUMN export_status VARCHAR(20),
    ADD COLUMN export_path TEXT,
    ADD COLUMN export_message VARCHAR(1000),
    ADD CONSTRAINT ck_crawl_run_export_status
        CHECK (export_status IS NULL OR export_status IN ('DISABLED', 'SUCCESS', 'FAILED'));

COMMENT ON COLUMN tb_crawl_run_log.export_status IS 'JSON 저장 결과 (NULL: 저장 단계 미도달)';
COMMENT ON COLUMN tb_crawl_run_log.export_path IS 'JSON 저장 파일 또는 게시판 저장 폴더';
COMMENT ON COLUMN tb_crawl_run_log.export_message IS 'JSON 저장 건수 또는 실패 원인';

WITH values_to_seed(code_key, code_nm, code_val, sort_order) AS (
    VALUES ('CRAWL_FAIL_BROWSER_ERROR', '브라우저 오류', 'BROWSER_ERROR', 110),
           ('CRAWL_FAIL_EXPORT_ERROR', 'JSON 파일 저장 오류', 'EXPORT_ERROR', 120)
)
INSERT INTO tb_code (p_code_no, code_group, code_nm, code_val, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt)
SELECT parent.code_no, value.code_key, value.code_nm, value.code_val, 3, value.sort_order,
    'Y', 'system', now(), 'system', now()
FROM values_to_seed value
JOIN tb_code parent ON parent.code_group = 'CRAWL_FAILURE_CODE'
    AND parent.code_depth = 2 AND parent.use_yn = 'Y'
WHERE NOT EXISTS (SELECT 1 FROM tb_code existing WHERE existing.p_code_no = parent.code_no
    AND existing.code_val = value.code_val AND existing.use_yn = 'Y');
