-- Register application code values as a hierarchical catalog.
-- Seed data uses depth 1/2, while management supports an optional depth 3.

ALTER TABLE tb_code
    DROP CONSTRAINT IF EXISTS ck_tb_code_two_level;

ALTER TABLE tb_code
    ADD CONSTRAINT ck_tb_code_two_level CHECK (
        (code_depth = 1 AND p_code_no = 0)
        OR (code_depth IN (2, 3) AND p_code_no > 0)
    );

-- V19 copied explicit identifiers from admin_code, so align the BIGSERIAL sequence
-- before inserting any new catalog rows.
SELECT setval(
    pg_get_serial_sequence('tb_code', 'code_no'),
    GREATEST((SELECT COALESCE(MAX(code_no), 0) FROM tb_code), 1),
    true
);

WITH groups(code_group, code_nm, code_dc, sort_order) AS (
    VALUES
        ('NOTICE_STATUS', '공지 노출 상태', '공지사항 노출 여부', 10),
        ('NOTIFICATION_CHANNEL_TYPE', '알림 채널 유형', '운영 알림 발송 채널', 20),
        ('CRAWL_STATUS', '크롤링 결과 상태', '크롤 문서 및 실행 결과', 30),
        ('SCENARIO_STATUS', '시나리오 상태', '시나리오 생명주기 상태', 40),
        ('SCENARIO_VERSION_STATUS', '시나리오 버전 상태', '시나리오 버전 생명주기 상태', 50),
        ('SCENARIO_NODE_TYPE', '시나리오 노드 유형', '시나리오 그래프 노드 유형', 60),
        ('SCENARIO_LINK_TYPE', '시나리오 링크 유형', '답변 링크 이동 유형', 70),
        ('CHAT_SESSION_STATE', '채팅 세션 상태', '채팅 세션 진행 상태', 80),
        ('CHAT_SESSION_TYPE', '채팅 세션 유형', '시나리오/검색 세션 구분', 90),
        ('CHAT_MESSAGE_DIRECTION', '채팅 메시지 방향', '메시지 작성 주체', 100),
        ('CHAT_FEEDBACK_RATING', '채팅 답변 평가', '답변 만족도 평가', 110),
        ('CHAT_FAILURE_REASON', '채팅 실패 사유', '채팅 처리 실패 분류', 120),
        ('SEARCH_SOURCE', '검색 요청 출처', '통합검색 호출 출처', 130),
        ('CRAWL_SCHEDULE_MODE', '크롤링 일정 방식', '크롤링 실행 주기 방식', 140),
        ('CRAWL_JOB_TRIGGER_TYPE', '크롤링 작업 실행 유형', '브라우저 크롤링 작업 실행 원인', 150),
        ('CRAWL_JOB_STATUS', '크롤링 작업 상태', '브라우저 크롤링 작업 진행 상태', 160),
        ('NOTIFICATION_EVENT_SEVERITY', '알림 심각도', '운영 알림 중요도', 170),
        ('NOTIFICATION_EVENT_STATUS', '알림 발송 상태', '운영 알림 발송 처리 상태', 180),
        ('CRAWL_NATIVE_LIB_STATUS', '크롤러 라이브러리 상태', '브라우저 네이티브 라이브러리 상태', 190)
)
INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt
)
SELECT 0, g.code_group, g.code_nm, 'GROUP', g.code_dc, 1, g.sort_order,
       'Y', 'system', now(), 'system', now()
FROM groups g
WHERE NOT EXISTS (
    SELECT 1 FROM tb_code c WHERE c.code_group = g.code_group AND c.use_yn = 'Y'
);

UPDATE tb_code
SET code_nm = CASE code_group
        WHEN 'NOTICE_STATUS' THEN '공지 노출 상태'
        WHEN 'NOTIFICATION_CHANNEL_TYPE' THEN '알림 채널 유형'
        WHEN 'CRAWL_STATUS' THEN '크롤링 결과 상태'
        ELSE code_nm
    END,
    code_dc = CASE code_group
        WHEN 'NOTICE_STATUS' THEN '공지사항 노출 여부'
        WHEN 'NOTIFICATION_CHANNEL_TYPE' THEN '운영 알림 발송 채널'
        WHEN 'CRAWL_STATUS' THEN '크롤 문서 및 실행 결과'
        ELSE code_dc
    END,
    lst_chgr_empno = 'system',
    lst_chg_dt = now()
WHERE code_depth = 1
  AND code_group IN ('NOTICE_STATUS', 'NOTIFICATION_CHANNEL_TYPE', 'CRAWL_STATUS');

WITH code_values(parent_group, code_key, code_nm, code_val, code_dc, sort_order) AS (
    VALUES
        ('NOTICE_STATUS', 'NOTICE_ENABLED', '노출', 'ENABLED', NULL, 10),
        ('NOTICE_STATUS', 'NOTICE_DISABLED', '미노출', 'DISABLED', NULL, 20),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_WEBHOOK', '웹훅', 'WEBHOOK', NULL, 10),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_SLACK_WEBHOOK', '슬랙 웹훅', 'SLACK_WEBHOOK', NULL, 20),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_EMAIL_SMTP', '이메일 SMTP', 'EMAIL_SMTP', NULL, 30),
        ('CRAWL_STATUS', 'CRAWL_SUCCESS', '성공', 'SUCCESS', NULL, 10),
        ('CRAWL_STATUS', 'CRAWL_FAILED', '실패', 'FAILED', NULL, 20),
        ('CRAWL_STATUS', 'CRAWL_DUPLICATE', '중복', 'DUPLICATE', NULL, 30),
        ('SCENARIO_STATUS', 'SCENARIO_DRAFT', '작성 중', 'DRAFT', NULL, 10),
        ('SCENARIO_STATUS', 'SCENARIO_ACTIVE', '활성', 'ACTIVE', NULL, 20),
        ('SCENARIO_STATUS', 'SCENARIO_INACTIVE', '비활성', 'INACTIVE', NULL, 30),
        ('SCENARIO_STATUS', 'SCENARIO_DELETED', '삭제', 'DELETED', NULL, 40),
        ('SCENARIO_VERSION_STATUS', 'SCENARIO_VERSION_DRAFT', '초안', 'DRAFT', NULL, 10),
        ('SCENARIO_VERSION_STATUS', 'SCENARIO_VERSION_PUBLISHED', '게시', 'PUBLISHED', NULL, 20),
        ('SCENARIO_VERSION_STATUS', 'SCENARIO_VERSION_ARCHIVED', '보관', 'ARCHIVED', NULL, 30),
        ('SCENARIO_NODE_TYPE', 'NODE_QUESTION', '질문', 'QUESTION', NULL, 10),
        ('SCENARIO_NODE_TYPE', 'NODE_ANSWER', '답변', 'ANSWER', NULL, 20),
        ('SCENARIO_NODE_TYPE', 'NODE_BRANCH', '분기', 'BRANCH', NULL, 30),
        ('SCENARIO_NODE_TYPE', 'NODE_END', '종료', 'END', NULL, 40),
        ('SCENARIO_LINK_TYPE', 'LINK_EXTERNAL', '외부 링크', 'EXTERNAL', NULL, 10),
        ('SCENARIO_LINK_TYPE', 'LINK_INTERNAL', '내부 링크', 'INTERNAL', NULL, 20),
        ('SCENARIO_LINK_TYPE', 'LINK_DOWNLOAD', '파일 다운로드', 'DOWNLOAD', NULL, 30),
        ('CHAT_SESSION_STATE', 'CHAT_STATE_ACTIVE', '진행 중', 'ACTIVE', NULL, 10),
        ('CHAT_SESSION_STATE', 'CHAT_STATE_COMPLETED', '완료', 'COMPLETED', NULL, 20),
        ('CHAT_SESSION_STATE', 'CHAT_STATE_ABANDONED', '중단', 'ABANDONED', NULL, 30),
        ('CHAT_SESSION_STATE', 'CHAT_STATE_EXPIRED', '만료', 'EXPIRED', NULL, 40),
        ('CHAT_SESSION_TYPE', 'CHAT_TYPE_SCENARIO', '시나리오 상담', 'SCENARIO', NULL, 10),
        ('CHAT_SESSION_TYPE', 'CHAT_TYPE_SEARCH', '통합검색', 'SEARCH', NULL, 20),
        ('CHAT_MESSAGE_DIRECTION', 'MESSAGE_USER', '사용자', 'USER', NULL, 10),
        ('CHAT_MESSAGE_DIRECTION', 'MESSAGE_BOT', '챗봇', 'BOT', NULL, 20),
        ('CHAT_MESSAGE_DIRECTION', 'MESSAGE_SYSTEM', '시스템', 'SYSTEM', NULL, 30),
        ('CHAT_FEEDBACK_RATING', 'FEEDBACK_UP', '만족', 'UP', NULL, 10),
        ('CHAT_FEEDBACK_RATING', 'FEEDBACK_DOWN', '불만족', 'DOWN', NULL, 20),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_NO_MATCH', '검색 결과 없음', 'NO_MATCH', NULL, 10),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_EXPIRED', '세션 만료', 'EXPIRED', NULL, 20),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_INVALID_OPTION', '잘못된 선택', 'INVALID_OPTION', NULL, 30),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_SYSTEM_ERROR', '시스템 오류', 'SYSTEM_ERROR', NULL, 40),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_PII_BLOCKED', '개인정보 차단', 'PII_BLOCKED', NULL, 50),
        ('CHAT_FAILURE_REASON', 'CHAT_FAIL_RATE_LIMITED', '요청 한도 초과', 'RATE_LIMITED', NULL, 60),
        ('SEARCH_SOURCE', 'SEARCH_CHAT_FALLBACK', '챗봇 대체 검색', 'CHAT_FALLBACK', NULL, 10),
        ('SEARCH_SOURCE', 'SEARCH_ADMIN_TEST', '관리자 테스트', 'ADMIN_TEST', NULL, 20),
        ('SEARCH_SOURCE', 'SEARCH_API', '외부 API', 'API', NULL, 30),
        ('CRAWL_SCHEDULE_MODE', 'CRAWL_SCHEDULE_INTERVAL', '주기 실행', 'INTERVAL', NULL, 10),
        ('CRAWL_SCHEDULE_MODE', 'CRAWL_SCHEDULE_CRON', 'Cron 실행', 'CRON', NULL, 20),
        ('CRAWL_JOB_TRIGGER_TYPE', 'CRAWL_TRIGGER_MANUAL', '수동 실행', 'MANUAL', NULL, 10),
        ('CRAWL_JOB_TRIGGER_TYPE', 'CRAWL_TRIGGER_SCHEDULE', '예약 실행', 'SCHEDULE', NULL, 20),
        ('CRAWL_JOB_STATUS', 'CRAWL_JOB_PENDING', '대기', 'PENDING', NULL, 10),
        ('CRAWL_JOB_STATUS', 'CRAWL_JOB_RUNNING', '실행 중', 'RUNNING', NULL, 20),
        ('CRAWL_JOB_STATUS', 'CRAWL_JOB_SUCCESS', '성공', 'SUCCESS', NULL, 30),
        ('CRAWL_JOB_STATUS', 'CRAWL_JOB_FAILED', '실패', 'FAILED', NULL, 40),
        ('CRAWL_JOB_STATUS', 'CRAWL_JOB_CANCELED', '취소', 'CANCELED', NULL, 50),
        ('NOTIFICATION_EVENT_SEVERITY', 'NOTIFY_SEVERITY_INFO', '정보', 'INFO', NULL, 10),
        ('NOTIFICATION_EVENT_SEVERITY', 'NOTIFY_SEVERITY_WARN', '경고', 'WARN', NULL, 20),
        ('NOTIFICATION_EVENT_SEVERITY', 'NOTIFY_SEVERITY_ERROR', '오류', 'ERROR', NULL, 30),
        ('NOTIFICATION_EVENT_STATUS', 'NOTIFY_STATUS_PENDING', '대기', 'PENDING', NULL, 10),
        ('NOTIFICATION_EVENT_STATUS', 'NOTIFY_STATUS_SENT', '발송 완료', 'SENT', NULL, 20),
        ('NOTIFICATION_EVENT_STATUS', 'NOTIFY_STATUS_RETRY', '재시도', 'RETRY', NULL, 30),
        ('NOTIFICATION_EVENT_STATUS', 'NOTIFY_STATUS_FAILED', '발송 실패', 'FAILED', NULL, 40),
        ('CRAWL_NATIVE_LIB_STATUS', 'NATIVE_LIB_VERIFIED', '검증 완료', 'VERIFIED', NULL, 10),
        ('CRAWL_NATIVE_LIB_STATUS', 'NATIVE_LIB_ACTIVE', '적용 중', 'ACTIVE', NULL, 20),
        ('CRAWL_NATIVE_LIB_STATUS', 'NATIVE_LIB_REJECTED', '반려', 'REJECTED', NULL, 30)
)
INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt
)
SELECT p.code_no, v.code_key, v.code_nm, v.code_val, v.code_dc, 2, v.sort_order,
       'Y', 'system', now(), 'system', now()
FROM code_values v
JOIN tb_code p
  ON p.code_group = v.parent_group
 AND p.code_depth = 1
 AND p.use_yn = 'Y'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_code c
    WHERE c.p_code_no = p.code_no
      AND c.code_val = v.code_val
      AND c.use_yn = 'Y'
);

UPDATE tb_code
SET code_nm = CASE code_group
        WHEN 'NOTICE_ENABLED' THEN '노출'
        WHEN 'NOTICE_DISABLED' THEN '미노출'
        WHEN 'CHANNEL_WEBHOOK' THEN '웹훅'
        WHEN 'CHANNEL_SLACK_WEBHOOK' THEN '슬랙 웹훅'
        WHEN 'CHANNEL_EMAIL_SMTP' THEN '이메일 SMTP'
        WHEN 'CRAWL_SUCCESS' THEN '성공'
        WHEN 'CRAWL_FAILED' THEN '실패'
        WHEN 'CRAWL_DUPLICATE' THEN '중복'
        ELSE code_nm
    END,
    lst_chgr_empno = 'system',
    lst_chg_dt = now()
WHERE code_depth = 2
  AND code_group IN (
      'NOTICE_ENABLED', 'NOTICE_DISABLED',
      'CHANNEL_WEBHOOK', 'CHANNEL_SLACK_WEBHOOK', 'CHANNEL_EMAIL_SMTP',
      'CRAWL_SUCCESS', 'CRAWL_FAILED', 'CRAWL_DUPLICATE'
  );
