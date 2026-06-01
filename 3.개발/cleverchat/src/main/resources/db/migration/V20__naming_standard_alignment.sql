-- V20: 도메인 테이블 OverseasNPP 명명표준 정렬 (auth-core/framework 제외)
-- 대상 23개. enabled(boolean)->use_yn(char1), id->{tbl}_no, created_at->frst_reg_dt 등
SET search_path TO cleverchat_dev, public;

-- ===== audit_log -> tb_audit_log =====
ALTER TABLE audit_log RENAME COLUMN id TO audit_log_no;
ALTER TABLE audit_log RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE audit_log RENAME TO tb_audit_log;
ALTER SEQUENCE audit_log_id_seq RENAME TO tb_audit_log_audit_log_no_seq;

-- ===== chat_failure -> tb_chat_failure =====
ALTER TABLE chat_failure RENAME COLUMN id TO chat_failure_no;
ALTER TABLE chat_failure RENAME COLUMN session_id TO session_no;
ALTER TABLE chat_failure RENAME COLUMN message_id TO message_no;
ALTER TABLE chat_failure RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE chat_failure RENAME TO tb_chat_failure;
ALTER SEQUENCE chat_failure_id_seq RENAME TO tb_chat_failure_chat_failure_no_seq;

-- ===== chat_feedback -> tb_chat_feedback =====
ALTER TABLE chat_feedback RENAME COLUMN id TO chat_feedback_no;
ALTER TABLE chat_feedback RENAME COLUMN message_id TO message_no;
ALTER TABLE chat_feedback RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE chat_feedback RENAME TO tb_chat_feedback;
ALTER SEQUENCE chat_feedback_id_seq RENAME TO tb_chat_feedback_chat_feedback_no_seq;

-- ===== chat_message -> tb_chat_message =====
ALTER TABLE chat_message RENAME COLUMN id TO chat_message_no;
ALTER TABLE chat_message RENAME COLUMN session_id TO session_no;
ALTER TABLE chat_message RENAME COLUMN node_id TO node_no;
ALTER TABLE chat_message RENAME COLUMN option_id TO option_no;
ALTER TABLE chat_message RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE chat_message RENAME TO tb_chat_message;
ALTER SEQUENCE chat_message_id_seq RENAME TO tb_chat_message_chat_message_no_seq;

-- ===== chat_recommendation -> tb_chat_recommendation =====
DROP INDEX IF EXISTS ix_chat_recommendation_enabled_priority;
ALTER TABLE chat_recommendation RENAME COLUMN id TO chat_recommendation_no;
ALTER TABLE chat_recommendation RENAME COLUMN scenario_id TO scenario_no;
ALTER TABLE chat_recommendation ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE chat_recommendation SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE chat_recommendation DROP COLUMN enabled;
ALTER TABLE chat_recommendation RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE chat_recommendation RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE chat_recommendation RENAME TO tb_chat_recommendation;
ALTER SEQUENCE chat_recommendation_id_seq RENAME TO tb_chat_recommendation_chat_recommendation_no_seq;
CREATE INDEX ix_chat_recommendation_useyn_priority ON tb_chat_recommendation (use_yn, priority, chat_recommendation_no);

-- ===== chat_session -> tb_chat_session =====
ALTER TABLE chat_session RENAME COLUMN id TO chat_session_no;
ALTER TABLE chat_session RENAME COLUMN user_id TO user_no;
ALTER TABLE chat_session RENAME COLUMN scenario_id TO scenario_no;
ALTER TABLE chat_session RENAME COLUMN version_id TO version_no;
ALTER TABLE chat_session RENAME COLUMN current_node_id TO current_node_no;
ALTER TABLE chat_session RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE chat_session RENAME TO tb_chat_session;

-- ===== crawl_document -> tb_crawl_document =====
ALTER TABLE crawl_document RENAME COLUMN id TO crawl_document_no;
ALTER TABLE crawl_document RENAME COLUMN target_id TO target_no;
ALTER TABLE crawl_document RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE crawl_document RENAME TO tb_crawl_document;
ALTER SEQUENCE crawl_document_id_seq RENAME TO tb_crawl_document_crawl_document_no_seq;

-- ===== crawl_run_log -> tb_crawl_run_log =====
ALTER TABLE crawl_run_log RENAME COLUMN id TO crawl_run_log_no;
ALTER TABLE crawl_run_log RENAME COLUMN target_id TO target_no;
ALTER TABLE crawl_run_log RENAME COLUMN document_id TO document_no;
ALTER TABLE crawl_run_log RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE crawl_run_log RENAME TO tb_crawl_run_log;
ALTER SEQUENCE crawl_run_log_id_seq RENAME TO tb_crawl_run_log_crawl_run_log_no_seq;

-- ===== crawl_target -> tb_crawl_target =====
DROP INDEX IF EXISTS ix_crawl_target_enabled_updated;
DROP INDEX IF EXISTS ix_crawl_target_schedule_mode_next_run;
DROP INDEX IF EXISTS ix_crawl_target_schedule_due;
ALTER TABLE crawl_target RENAME COLUMN id TO crawl_target_no;
ALTER TABLE crawl_target ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE crawl_target SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE crawl_target DROP COLUMN enabled;
ALTER TABLE crawl_target RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE crawl_target RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE crawl_target RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE crawl_target ADD COLUMN frst_regr_ip varchar(64);
ALTER TABLE crawl_target ADD COLUMN lst_chgr_ip varchar(64);
ALTER TABLE crawl_target RENAME TO tb_crawl_target;
ALTER SEQUENCE crawl_target_id_seq RENAME TO tb_crawl_target_crawl_target_no_seq;
CREATE INDEX ix_crawl_target_useyn_updated ON tb_crawl_target (use_yn, lst_chg_dt DESC);
CREATE INDEX ix_crawl_target_schedule_mode_next_run ON tb_crawl_target (schedule_mode, next_run_at) WHERE (use_yn='Y' AND schedule_enabled=true);
CREATE INDEX ix_crawl_target_schedule_due ON tb_crawl_target (next_run_at) WHERE (use_yn='Y' AND schedule_enabled=true);

-- ===== login_log -> tb_login_log =====
ALTER TABLE login_log RENAME COLUMN id TO login_log_no;
ALTER TABLE login_log RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE login_log RENAME TO tb_login_log;
ALTER SEQUENCE login_log_id_seq RENAME TO tb_login_log_login_log_no_seq;

-- ===== notice -> tb_notice =====
DROP INDEX IF EXISTS ix_notice_enabled_period_priority;
ALTER TABLE notice RENAME COLUMN id TO notice_no;
ALTER TABLE notice ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE notice SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE notice DROP COLUMN enabled;
ALTER TABLE notice RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE notice RENAME COLUMN updated_by TO lst_chgr_empno;
ALTER TABLE notice RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE notice RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE notice ADD COLUMN frst_regr_ip varchar(64);
ALTER TABLE notice ADD COLUMN lst_chgr_ip varchar(64);
ALTER TABLE notice RENAME TO tb_notice;
ALTER SEQUENCE notice_id_seq RENAME TO tb_notice_notice_no_seq;
CREATE INDEX ix_notice_useyn_period_priority ON tb_notice (use_yn, starts_at, ends_at, priority, notice_no);

-- ===== notification_channel -> tb_notification_channel =====
DROP INDEX IF EXISTS ix_notification_channel_enabled;
ALTER TABLE notification_channel RENAME COLUMN id TO notification_channel_no;
ALTER TABLE notification_channel ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE notification_channel SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE notification_channel DROP COLUMN enabled;
ALTER TABLE notification_channel RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE notification_channel RENAME COLUMN updated_by TO lst_chgr_empno;
ALTER TABLE notification_channel RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE notification_channel RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE notification_channel ADD COLUMN frst_regr_ip varchar(64);
ALTER TABLE notification_channel ADD COLUMN lst_chgr_ip varchar(64);
ALTER TABLE notification_channel RENAME TO tb_notification_channel;
ALTER SEQUENCE notification_channel_id_seq RENAME TO tb_notification_channel_notification_channel_no_seq;
CREATE INDEX ix_notification_channel_useyn ON tb_notification_channel (use_yn, notification_channel_no DESC);

-- ===== notification_event -> tb_notification_event =====
ALTER TABLE notification_event RENAME COLUMN id TO notification_event_no;
ALTER TABLE notification_event RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE notification_event RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE notification_event RENAME TO tb_notification_event;
ALTER SEQUENCE notification_event_id_seq RENAME TO tb_notification_event_notification_event_no_seq;

-- ===== popular_query_daily -> tb_popular_query_daily =====
ALTER TABLE popular_query_daily RENAME COLUMN top_result_scenario_id TO top_result_scenario_no;
ALTER TABLE popular_query_daily RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE popular_query_daily RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE popular_query_daily RENAME TO tb_popular_query_daily;

-- ===== scenario -> tb_scenario =====
ALTER TABLE scenario RENAME COLUMN id TO scenario_no;
ALTER TABLE scenario RENAME COLUMN category_id TO category_no;
ALTER TABLE scenario RENAME COLUMN active_version_id TO active_version_no;
ALTER TABLE scenario RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario RENAME TO tb_scenario;
ALTER SEQUENCE scenario_id_seq RENAME TO tb_scenario_scenario_no_seq;

-- ===== scenario_category -> tb_scenario_category =====
ALTER TABLE scenario_category RENAME COLUMN id TO scenario_category_no;
ALTER TABLE scenario_category RENAME COLUMN parent_id TO p_scenario_category_no;
ALTER TABLE scenario_category ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE scenario_category SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE scenario_category DROP COLUMN enabled;
ALTER TABLE scenario_category RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_category RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario_category RENAME TO tb_scenario_category;
ALTER SEQUENCE scenario_category_id_seq RENAME TO tb_scenario_category_scenario_category_no_seq;

-- ===== scenario_keyword -> tb_scenario_keyword =====
DROP INDEX IF EXISTS ix_scenario_keyword_keyword_trgm;
DROP INDEX IF EXISTS ix_scenario_keyword_scenario_enabled;
ALTER TABLE scenario_keyword RENAME COLUMN id TO scenario_keyword_no;
ALTER TABLE scenario_keyword RENAME COLUMN scenario_id TO scenario_no;
ALTER TABLE scenario_keyword ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE scenario_keyword SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE scenario_keyword DROP COLUMN enabled;
ALTER TABLE scenario_keyword RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_keyword RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario_keyword RENAME TO tb_scenario_keyword;
ALTER SEQUENCE scenario_keyword_id_seq RENAME TO tb_scenario_keyword_scenario_keyword_no_seq;
CREATE INDEX ix_scenario_keyword_keyword_trgm ON tb_scenario_keyword USING gin (keyword gin_trgm_ops) WHERE (use_yn='Y');
CREATE INDEX ix_scenario_keyword_scenario_useyn ON tb_scenario_keyword (scenario_no, use_yn, weight DESC);

-- ===== scenario_node -> tb_scenario_node =====
ALTER TABLE scenario_node RENAME COLUMN id TO scenario_node_no;
ALTER TABLE scenario_node RENAME COLUMN version_id TO version_no;
ALTER TABLE scenario_node RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_node RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario_node RENAME TO tb_scenario_node;
ALTER SEQUENCE scenario_node_id_seq RENAME TO tb_scenario_node_scenario_node_no_seq;

-- ===== scenario_node_option -> tb_scenario_node_option =====
ALTER TABLE scenario_node_option RENAME COLUMN id TO scenario_node_option_no;
ALTER TABLE scenario_node_option RENAME COLUMN node_id TO node_no;
ALTER TABLE scenario_node_option RENAME COLUMN next_node_id TO next_node_no;
ALTER TABLE scenario_node_option ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE scenario_node_option SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE scenario_node_option DROP COLUMN enabled;
ALTER TABLE scenario_node_option RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_node_option RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario_node_option RENAME TO tb_scenario_node_option;
ALTER SEQUENCE scenario_node_option_id_seq RENAME TO tb_scenario_node_option_scenario_node_option_no_seq;

-- ===== scenario_synonym -> tb_scenario_synonym =====
DROP INDEX IF EXISTS ix_scenario_synonym_synonym_trgm;
DROP INDEX IF EXISTS ix_scenario_synonym_keyword_enabled;
ALTER TABLE scenario_synonym RENAME COLUMN id TO scenario_synonym_no;
ALTER TABLE scenario_synonym RENAME COLUMN keyword_id TO keyword_no;
ALTER TABLE scenario_synonym ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE scenario_synonym SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE scenario_synonym DROP COLUMN enabled;
ALTER TABLE scenario_synonym RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_synonym RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE scenario_synonym RENAME TO tb_scenario_synonym;
ALTER SEQUENCE scenario_synonym_id_seq RENAME TO tb_scenario_synonym_scenario_synonym_no_seq;
CREATE INDEX ix_scenario_synonym_synonym_trgm ON tb_scenario_synonym USING gin (synonym gin_trgm_ops) WHERE (use_yn='Y');
CREATE INDEX ix_scenario_synonym_keyword_useyn ON tb_scenario_synonym (keyword_no, use_yn, weight DESC);

-- ===== scenario_version -> tb_scenario_version =====
ALTER TABLE scenario_version RENAME COLUMN id TO scenario_version_no;
ALTER TABLE scenario_version RENAME COLUMN scenario_id TO scenario_no;
ALTER TABLE scenario_version RENAME COLUMN start_node_id TO start_node_no;
ALTER TABLE scenario_version RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE scenario_version RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE scenario_version RENAME TO tb_scenario_version;
ALTER SEQUENCE scenario_version_id_seq RENAME TO tb_scenario_version_scenario_version_no_seq;

-- ===== search_block_log -> tb_search_block_log =====
ALTER TABLE search_block_log RENAME COLUMN id TO search_block_log_no;
ALTER TABLE search_block_log RENAME COLUMN user_id TO user_no;
ALTER TABLE search_block_log RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE search_block_log RENAME TO tb_search_block_log;
ALTER SEQUENCE search_block_log_id_seq RENAME TO tb_search_block_log_search_block_log_no_seq;

-- ===== search_log -> tb_search_log =====
ALTER TABLE search_log RENAME COLUMN id TO search_log_no;
ALTER TABLE search_log RENAME COLUMN top_scenario_id TO top_scenario_no;
ALTER TABLE search_log RENAME COLUMN user_id TO user_no;
ALTER TABLE search_log RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE search_log RENAME TO tb_search_log;
ALTER SEQUENCE search_log_id_seq RENAME TO tb_search_log_search_log_no_seq;
