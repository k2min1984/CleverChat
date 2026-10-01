-- Add the crawl failure classification that was previously hard-coded in the admin filter.

INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt
)
SELECT system_root.code_no, 'CRAWL_FAILURE_CODE', '크롤링 실패 코드', 'GROUP',
       '크롤링 실패 원인 분류', 2, 125, 'Y', 'system', now(), 'system', now()
FROM tb_code system_root
WHERE system_root.code_group = 'SYSTEM_CODE'
  AND system_root.code_depth = 1
  AND system_root.use_yn = 'Y'
  AND NOT EXISTS (
      SELECT 1 FROM tb_code WHERE code_group = 'CRAWL_FAILURE_CODE' AND use_yn = 'Y'
  );

WITH values_to_seed(code_key, code_nm, code_val, sort_order) AS (
    VALUES
        ('CRAWL_FAIL_ROBOTS_BLOCKED', 'robots.txt 차단', 'ROBOTS_BLOCKED', 10),
        ('CRAWL_FAIL_HTTP_ERROR', 'HTTP 오류', 'HTTP_ERROR', 20),
        ('CRAWL_FAIL_TIMEOUT', '시간 초과', 'TIMEOUT', 30),
        ('CRAWL_FAIL_PARSE_ERROR', '본문 분석 오류', 'PARSE_ERROR', 40),
        ('CRAWL_FAIL_DUP_HASH', '중복 문서', 'DUP_HASH', 50),
        ('CRAWL_FAIL_FETCH_ERROR', '수집 오류', 'FETCH_ERROR', 60),
        ('CRAWL_FAIL_NOT_HTML', 'HTML 아님', 'NOT_HTML', 70),
        ('CRAWL_FAIL_URL_BLOCKED', 'URL 차단', 'URL_BLOCKED', 80),
        ('CRAWL_FAIL_SCHEDULE_INVALID', '일정 설정 오류', 'SCHEDULE_INVALID', 90),
        ('CRAWL_FAIL_SYSTEM_ERROR', '시스템 오류', 'SYSTEM_ERROR', 100)
)
INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt
)
SELECT code_group.code_no, value.code_key, value.code_nm, value.code_val, 3, value.sort_order,
       'Y', 'system', now(), 'system', now()
FROM values_to_seed value
JOIN tb_code code_group
  ON code_group.code_group = 'CRAWL_FAILURE_CODE'
 AND code_group.code_depth = 2
 AND code_group.use_yn = 'Y'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_code existing
    WHERE existing.p_code_no = code_group.code_no
      AND existing.code_val = value.code_val
      AND existing.use_yn = 'Y'
);
