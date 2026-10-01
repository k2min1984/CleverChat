-- Organize the application catalog as:
-- SYSTEM_CODE (depth 1) -> code groups (depth 2) -> code values (depth 3).

ALTER TABLE tb_code
    DROP CONSTRAINT IF EXISTS ck_tb_code_two_level;

ALTER TABLE tb_code
    DROP CONSTRAINT IF EXISTS ck_tb_code_hierarchy;

ALTER TABLE tb_code
    ADD CONSTRAINT ck_tb_code_hierarchy CHECK (
        (code_depth = 1 AND p_code_no = 0)
        OR (code_depth IN (2, 3) AND p_code_no > 0)
    );

SELECT setval(
    pg_get_serial_sequence('tb_code', 'code_no'),
    GREATEST((SELECT COALESCE(MAX(code_no), 0) FROM tb_code), 1),
    true
);

INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt
)
SELECT 0, 'SYSTEM_CODE', '시스템코드', 'GROUP', 'CleverChat 시스템 공통코드', 1, 10,
       'Y', 'system', now(), 'system', now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_code WHERE code_group = 'SYSTEM_CODE' AND use_yn = 'Y'
);

WITH managed_groups(code_group) AS (
    VALUES
        ('NOTICE_STATUS'),
        ('NOTIFICATION_CHANNEL_TYPE'),
        ('CRAWL_STATUS'),
        ('SCENARIO_STATUS'),
        ('SCENARIO_VERSION_STATUS'),
        ('SCENARIO_NODE_TYPE'),
        ('SCENARIO_LINK_TYPE'),
        ('CHAT_SESSION_STATE'),
        ('CHAT_SESSION_TYPE'),
        ('CHAT_MESSAGE_DIRECTION'),
        ('CHAT_FEEDBACK_RATING'),
        ('CHAT_FAILURE_REASON'),
        ('SEARCH_SOURCE'),
        ('CRAWL_SCHEDULE_MODE'),
        ('CRAWL_JOB_TRIGGER_TYPE'),
        ('CRAWL_JOB_STATUS'),
        ('NOTIFICATION_EVENT_SEVERITY'),
        ('NOTIFICATION_EVENT_STATUS'),
        ('CRAWL_NATIVE_LIB_STATUS')
), system_root AS (
    SELECT code_no
    FROM tb_code
    WHERE code_group = 'SYSTEM_CODE'
      AND use_yn = 'Y'
    ORDER BY code_no
    LIMIT 1
)
UPDATE tb_code code_group
SET p_code_no = system_root.code_no,
    code_depth = 2,
    lst_chgr_empno = 'system',
    lst_chg_dt = now()
FROM managed_groups, system_root
WHERE code_group.code_group = managed_groups.code_group;

WITH managed_groups(code_group) AS (
    VALUES
        ('NOTICE_STATUS'),
        ('NOTIFICATION_CHANNEL_TYPE'),
        ('CRAWL_STATUS'),
        ('SCENARIO_STATUS'),
        ('SCENARIO_VERSION_STATUS'),
        ('SCENARIO_NODE_TYPE'),
        ('SCENARIO_LINK_TYPE'),
        ('CHAT_SESSION_STATE'),
        ('CHAT_SESSION_TYPE'),
        ('CHAT_MESSAGE_DIRECTION'),
        ('CHAT_FEEDBACK_RATING'),
        ('CHAT_FAILURE_REASON'),
        ('SEARCH_SOURCE'),
        ('CRAWL_SCHEDULE_MODE'),
        ('CRAWL_JOB_TRIGGER_TYPE'),
        ('CRAWL_JOB_STATUS'),
        ('NOTIFICATION_EVENT_SEVERITY'),
        ('NOTIFICATION_EVENT_STATUS'),
        ('CRAWL_NATIVE_LIB_STATUS')
)
UPDATE tb_code code_value
SET code_depth = 3,
    lst_chgr_empno = 'system',
    lst_chg_dt = now()
FROM tb_code code_group, managed_groups
WHERE code_value.p_code_no = code_group.code_no
  AND code_group.code_group = managed_groups.code_group
  AND code_value.code_no <> code_group.code_no;
