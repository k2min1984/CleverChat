-- V22: 전체 tb_* 테이블/컬럼 코멘트 (한글 설명)
SET search_path TO cleverchat_dev, public;

-- ===== tb_admin_code =====
COMMENT ON TABLE tb_admin_code IS '관리자 공통코드 (현재 미사용 — adminmanage는 tb_code 사용)';
COMMENT ON COLUMN tb_admin_code.admin_code_no IS 'PK 코드 번호';
COMMENT ON COLUMN tb_admin_code.p_admin_code_no IS '상위 코드 번호(self FK)';
COMMENT ON COLUMN tb_admin_code.code IS '코드값';
COMMENT ON COLUMN tb_admin_code.name IS '코드명';
COMMENT ON COLUMN tb_admin_code.value IS '코드 매핑값';
COMMENT ON COLUMN tb_admin_code.description IS '설명';
COMMENT ON COLUMN tb_admin_code.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_admin_code.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_admin_code.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_admin_code.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_admin_code.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_admin_code.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_admin_code.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_admin_code.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_admin_menu =====
COMMENT ON TABLE tb_admin_menu IS '관리자 메뉴 (현재 미사용 — adminmanage는 tb_menu 사용)';
COMMENT ON COLUMN tb_admin_menu.admin_menu_no IS 'PK 메뉴 번호';
COMMENT ON COLUMN tb_admin_menu.p_admin_menu_no IS '상위 메뉴 번호(self FK)';
COMMENT ON COLUMN tb_admin_menu.menu_key IS '메뉴 키';
COMMENT ON COLUMN tb_admin_menu.title IS '메뉴 제목';
COMMENT ON COLUMN tb_admin_menu.url IS '메뉴 URL';
COMMENT ON COLUMN tb_admin_menu.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_admin_menu.visible IS '노출 여부(boolean)';
COMMENT ON COLUMN tb_admin_menu.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_admin_menu.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_admin_menu.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_admin_menu.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_admin_menu.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_admin_menu.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_admin_menu.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_admin_role_menu =====
COMMENT ON TABLE tb_admin_role_menu IS '관리자 역할-메뉴 권한 (현재 미사용)';
COMMENT ON COLUMN tb_admin_role_menu.role_no IS '역할 번호(FK)';
COMMENT ON COLUMN tb_admin_role_menu.menu_no IS '메뉴 번호(FK)';
COMMENT ON COLUMN tb_admin_role_menu.can_read IS '읽기 권한 여부(boolean)';
COMMENT ON COLUMN tb_admin_role_menu.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_audit_log =====
COMMENT ON TABLE tb_audit_log IS '감사 로그';
COMMENT ON COLUMN tb_audit_log.audit_log_no IS 'PK';
COMMENT ON COLUMN tb_audit_log.actor IS '행위자';
COMMENT ON COLUMN tb_audit_log.action IS '행위(액션)';
COMMENT ON COLUMN tb_audit_log.target_type IS '대상 유형';
COMMENT ON COLUMN tb_audit_log.target_id IS '대상 식별자';
COMMENT ON COLUMN tb_audit_log.detail IS '상세 내용(JSONB)';
COMMENT ON COLUMN tb_audit_log.ip IS '요청 IP';
COMMENT ON COLUMN tb_audit_log.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_auth =====
COMMENT ON TABLE tb_auth IS '권한(인증) 그룹 — 레거시 CMS 프레임워크';
COMMENT ON COLUMN tb_auth.auth_no IS 'PK 권한 번호';
COMMENT ON COLUMN tb_auth.auth_nm IS '권한명';
COMMENT ON COLUMN tb_auth.auth_dc IS '권한 설명';
COMMENT ON COLUMN tb_auth.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_auth.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_auth.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_auth.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_auth.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_auth.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_auth.dept_cd IS '부서 코드';
COMMENT ON COLUMN tb_auth.auth_rank IS '권한 등급';
COMMENT ON COLUMN tb_auth.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_auth.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_auth_menu_adm =====
COMMENT ON TABLE tb_auth_menu_adm IS '권한별 관리자 메뉴 접근권한 — 레거시 CMS';
COMMENT ON COLUMN tb_auth_menu_adm.auth_no IS '권한 번호(FK)';
COMMENT ON COLUMN tb_auth_menu_adm.menu_no IS '메뉴 번호(FK)';
COMMENT ON COLUMN tb_auth_menu_adm.p_menu_no IS '상위 메뉴 번호';
COMMENT ON COLUMN tb_auth_menu_adm.select_yn IS '조회 권한';
COMMENT ON COLUMN tb_auth_menu_adm.insert_yn IS '등록 권한';
COMMENT ON COLUMN tb_auth_menu_adm.update_yn IS '수정 권한';
COMMENT ON COLUMN tb_auth_menu_adm.delete_yn IS '삭제 권한';
COMMENT ON COLUMN tb_auth_menu_adm.proc_yn IS '처리(실행) 권한';
COMMENT ON COLUMN tb_auth_menu_adm.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_auth_menu_adm.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_auth_menu_adm.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_auth_menu_adm.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_auth_menu_adm.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_auth_menu_adm.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_auth_menu_adm.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_chat_failure =====
COMMENT ON TABLE tb_chat_failure IS '챗봇 실패/미응답 기록';
COMMENT ON COLUMN tb_chat_failure.chat_failure_no IS 'PK';
COMMENT ON COLUMN tb_chat_failure.session_no IS '세션 번호(FK)';
COMMENT ON COLUMN tb_chat_failure.message_no IS '메시지 번호(FK)';
COMMENT ON COLUMN tb_chat_failure.reason IS '실패 사유 코드';
COMMENT ON COLUMN tb_chat_failure.detail IS '상세(JSONB)';
COMMENT ON COLUMN tb_chat_failure.reviewed IS '검토 완료 여부(boolean)';
COMMENT ON COLUMN tb_chat_failure.reviewed_by IS '검토자 사용자 번호(FK)';
COMMENT ON COLUMN tb_chat_failure.reviewed_at IS '검토 일시';
COMMENT ON COLUMN tb_chat_failure.review_comment IS '검토 코멘트';
COMMENT ON COLUMN tb_chat_failure.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_chat_failure.review_comment_ciphertext IS '검토코멘트 암호문';
COMMENT ON COLUMN tb_chat_failure.review_comment_key_id IS '암호화 키 ID';
COMMENT ON COLUMN tb_chat_failure.review_comment_encryption_version IS '암호화 버전';

-- ===== tb_chat_feedback =====
COMMENT ON TABLE tb_chat_feedback IS '챗봇 답변 피드백';
COMMENT ON COLUMN tb_chat_feedback.chat_feedback_no IS 'PK';
COMMENT ON COLUMN tb_chat_feedback.message_no IS '메시지 번호(FK)';
COMMENT ON COLUMN tb_chat_feedback.rating IS '평가(좋아요/싫어요 등)';
COMMENT ON COLUMN tb_chat_feedback.comment IS '피드백 코멘트';
COMMENT ON COLUMN tb_chat_feedback.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_chat_feedback.ip_hash IS '요청 IP 해시';
COMMENT ON COLUMN tb_chat_feedback.comment_ciphertext IS '코멘트 암호문';
COMMENT ON COLUMN tb_chat_feedback.comment_key_id IS '암호화 키 ID';
COMMENT ON COLUMN tb_chat_feedback.comment_encryption_version IS '암호화 버전';

-- ===== tb_chat_message =====
COMMENT ON TABLE tb_chat_message IS '챗봇 대화 메시지';
COMMENT ON COLUMN tb_chat_message.chat_message_no IS 'PK';
COMMENT ON COLUMN tb_chat_message.session_no IS '세션 번호(FK)';
COMMENT ON COLUMN tb_chat_message.seq IS '세션 내 순번';
COMMENT ON COLUMN tb_chat_message.direction IS '방향(USER/BOT)';
COMMENT ON COLUMN tb_chat_message.node_no IS '시나리오 노드 번호(FK)';
COMMENT ON COLUMN tb_chat_message.option_no IS '선택 옵션 번호(FK)';
COMMENT ON COLUMN tb_chat_message.content IS '메시지 내용';
COMMENT ON COLUMN tb_chat_message.payload IS '부가 데이터(JSONB)';
COMMENT ON COLUMN tb_chat_message.latency_ms IS '응답 지연(ms)';
COMMENT ON COLUMN tb_chat_message.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_chat_message.content_ciphertext IS '내용 암호문';
COMMENT ON COLUMN tb_chat_message.content_key_id IS '암호화 키 ID';
COMMENT ON COLUMN tb_chat_message.content_encryption_version IS '암호화 버전';

-- ===== tb_chat_recommendation =====
COMMENT ON TABLE tb_chat_recommendation IS '시나리오 추천 항목';
COMMENT ON COLUMN tb_chat_recommendation.chat_recommendation_no IS 'PK';
COMMENT ON COLUMN tb_chat_recommendation.scenario_no IS '시나리오 번호(FK)';
COMMENT ON COLUMN tb_chat_recommendation.label IS '추천 라벨';
COMMENT ON COLUMN tb_chat_recommendation.priority IS '우선순위';
COMMENT ON COLUMN tb_chat_recommendation.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_chat_recommendation.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_chat_recommendation.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_chat_session =====
COMMENT ON TABLE tb_chat_session IS '챗봇 대화 세션';
COMMENT ON COLUMN tb_chat_session.chat_session_no IS 'PK(UUID)';
COMMENT ON COLUMN tb_chat_session.anonymous_id IS '익명 방문자 식별자(UUID)';
COMMENT ON COLUMN tb_chat_session.user_no IS '사용자 번호(FK, 로그인 시)';
COMMENT ON COLUMN tb_chat_session.scenario_no IS '시나리오 번호(FK)';
COMMENT ON COLUMN tb_chat_session.version_no IS '시나리오 버전 번호(FK)';
COMMENT ON COLUMN tb_chat_session.current_node_no IS '현재 노드 번호(FK)';
COMMENT ON COLUMN tb_chat_session.state IS '세션 상태';
COMMENT ON COLUMN tb_chat_session.started_at IS '시작 일시';
COMMENT ON COLUMN tb_chat_session.last_activity_at IS '최종 활동 일시';
COMMENT ON COLUMN tb_chat_session.expires_at IS '만료 일시';
COMMENT ON COLUMN tb_chat_session.ip_hash IS 'IP 해시';
COMMENT ON COLUMN tb_chat_session.user_agent_hash IS 'User-Agent 해시';
COMMENT ON COLUMN tb_chat_session.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_code =====
COMMENT ON TABLE tb_code IS '공통코드 — 레거시 CMS';
COMMENT ON COLUMN tb_code.code_no IS 'PK 코드 번호';
COMMENT ON COLUMN tb_code.p_code_no IS '상위 코드 번호';
COMMENT ON COLUMN tb_code.code_group IS '코드 그룹';
COMMENT ON COLUMN tb_code.code_nm IS '코드명';
COMMENT ON COLUMN tb_code.code_val IS '코드값';
COMMENT ON COLUMN tb_code.code_dc IS '코드 설명';
COMMENT ON COLUMN tb_code.code_depth IS '코드 깊이';
COMMENT ON COLUMN tb_code.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_code.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_code.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_code.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_code.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_code.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_code.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_code.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_crawl_document =====
COMMENT ON TABLE tb_crawl_document IS '크롤링 수집 문서';
COMMENT ON COLUMN tb_crawl_document.crawl_document_no IS 'PK';
COMMENT ON COLUMN tb_crawl_document.target_no IS '크롤 대상 번호(FK)';
COMMENT ON COLUMN tb_crawl_document.url IS '문서 URL';
COMMENT ON COLUMN tb_crawl_document.title IS '문서 제목';
COMMENT ON COLUMN tb_crawl_document.content IS '본문';
COMMENT ON COLUMN tb_crawl_document.url_hash IS 'URL 해시';
COMMENT ON COLUMN tb_crawl_document.content_hash IS '본문 해시(중복 판별)';
COMMENT ON COLUMN tb_crawl_document.status IS '상태';
COMMENT ON COLUMN tb_crawl_document.http_status IS 'HTTP 상태코드';
COMMENT ON COLUMN tb_crawl_document.error_message IS '오류 메시지';
COMMENT ON COLUMN tb_crawl_document.fetched_at IS '수집 일시';
COMMENT ON COLUMN tb_crawl_document.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_crawl_run_log =====
COMMENT ON TABLE tb_crawl_run_log IS '크롤링 실행 로그';
COMMENT ON COLUMN tb_crawl_run_log.crawl_run_log_no IS 'PK';
COMMENT ON COLUMN tb_crawl_run_log.target_no IS '크롤 대상 번호(FK)';
COMMENT ON COLUMN tb_crawl_run_log.document_no IS '수집 문서 번호(FK)';
COMMENT ON COLUMN tb_crawl_run_log.status IS '상태';
COMMENT ON COLUMN tb_crawl_run_log.http_status IS 'HTTP 상태코드';
COMMENT ON COLUMN tb_crawl_run_log.message IS '메시지';
COMMENT ON COLUMN tb_crawl_run_log.duration_ms IS '소요(ms)';
COMMENT ON COLUMN tb_crawl_run_log.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_crawl_run_log.reviewed IS '검토 완료 여부(boolean)';
COMMENT ON COLUMN tb_crawl_run_log.reviewed_by IS '검토자 사용자 번호(FK)';
COMMENT ON COLUMN tb_crawl_run_log.reviewed_at IS '검토 일시';
COMMENT ON COLUMN tb_crawl_run_log.review_comment IS '검토 코멘트';
COMMENT ON COLUMN tb_crawl_run_log.failure_code IS '실패 코드';

-- ===== tb_crawl_target =====
COMMENT ON TABLE tb_crawl_target IS '크롤링 대상';
COMMENT ON COLUMN tb_crawl_target.crawl_target_no IS 'PK';
COMMENT ON COLUMN tb_crawl_target.url IS '대상 URL';
COMMENT ON COLUMN tb_crawl_target.label IS '대상 라벨';
COMMENT ON COLUMN tb_crawl_target.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_crawl_target.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_crawl_target.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_crawl_target.last_run_at IS '최근 실행 일시';
COMMENT ON COLUMN tb_crawl_target.last_status IS '최근 상태';
COMMENT ON COLUMN tb_crawl_target.last_message IS '최근 메시지';
COMMENT ON COLUMN tb_crawl_target.schedule_enabled IS '스케줄 사용 여부(boolean)';
COMMENT ON COLUMN tb_crawl_target.schedule_interval_minutes IS '스케줄 주기(분)';
COMMENT ON COLUMN tb_crawl_target.next_run_at IS '다음 실행 일시';
COMMENT ON COLUMN tb_crawl_target.robots_allowed IS 'robots 허용 여부(boolean)';
COMMENT ON COLUMN tb_crawl_target.robots_checked_at IS 'robots 확인 일시';
COMMENT ON COLUMN tb_crawl_target.schedule_mode IS '스케줄 모드(INTERVAL/CRON)';
COMMENT ON COLUMN tb_crawl_target.schedule_cron IS '크론 표현식';
COMMENT ON COLUMN tb_crawl_target.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_crawl_target.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_crawl_target.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_login_log =====
COMMENT ON TABLE tb_login_log IS '로그인 시도 로그';
COMMENT ON COLUMN tb_login_log.login_log_no IS 'PK';
COMMENT ON COLUMN tb_login_log.username IS '로그인 사용자명';
COMMENT ON COLUMN tb_login_log.success IS '성공 여부(boolean)';
COMMENT ON COLUMN tb_login_log.failure_msg IS '실패 사유';
COMMENT ON COLUMN tb_login_log.ip IS '요청 IP';
COMMENT ON COLUMN tb_login_log.user_agent IS 'User-Agent';
COMMENT ON COLUMN tb_login_log.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_menu =====
COMMENT ON TABLE tb_menu IS '메뉴 — 레거시 CMS';
COMMENT ON COLUMN tb_menu.menu_no IS 'PK 메뉴 번호';
COMMENT ON COLUMN tb_menu.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_menu.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_menu.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_menu.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_menu.p_menu_no IS '상위 메뉴 번호';
COMMENT ON COLUMN tb_menu.menu_nm IS '메뉴명';
COMMENT ON COLUMN tb_menu.menu_type IS '메뉴 유형';
COMMENT ON COLUMN tb_menu.board_mng_no IS '게시판 관리 번호';
COMMENT ON COLUMN tb_menu.menu_depth IS '메뉴 깊이';
COMMENT ON COLUMN tb_menu.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_menu.target_blank_yn IS '새창 여부(Y/N)';
COMMENT ON COLUMN tb_menu.menu_url IS '메뉴 URL';
COMMENT ON COLUMN tb_menu.rel_url IS '연관 URL';
COMMENT ON COLUMN tb_menu.open_yn IS '공개 여부(Y/N)';
COMMENT ON COLUMN tb_menu.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_menu.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_menu.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_notice =====
COMMENT ON TABLE tb_notice IS '공지사항';
COMMENT ON COLUMN tb_notice.notice_no IS 'PK';
COMMENT ON COLUMN tb_notice.title IS '제목';
COMMENT ON COLUMN tb_notice.content IS '내용';
COMMENT ON COLUMN tb_notice.starts_at IS '게시 시작 일시';
COMMENT ON COLUMN tb_notice.ends_at IS '게시 종료 일시';
COMMENT ON COLUMN tb_notice.priority IS '우선순위';
COMMENT ON COLUMN tb_notice.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_notice.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_notice.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_notice.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_notice.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_notice.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_notice.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_notification_channel =====
COMMENT ON TABLE tb_notification_channel IS '알림 채널';
COMMENT ON COLUMN tb_notification_channel.notification_channel_no IS 'PK';
COMMENT ON COLUMN tb_notification_channel.name IS '채널명';
COMMENT ON COLUMN tb_notification_channel.type IS '채널 유형(WEBHOOK 등)';
COMMENT ON COLUMN tb_notification_channel.endpoint_env_key IS '엔드포인트 환경변수 키';
COMMENT ON COLUMN tb_notification_channel.rate_limit_per_hour IS '시간당 발송 제한';
COMMENT ON COLUMN tb_notification_channel.frst_regr_empno IS '최초 등록자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_notification_channel.lst_chgr_empno IS '최종 변경자(사번/사용자 식별자)';
COMMENT ON COLUMN tb_notification_channel.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_notification_channel.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_notification_channel.previous_endpoint_env_key IS '이전 엔드포인트 키';
COMMENT ON COLUMN tb_notification_channel.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';
COMMENT ON COLUMN tb_notification_channel.frst_regr_ip IS '최초 등록자 IP';
COMMENT ON COLUMN tb_notification_channel.lst_chgr_ip IS '최종 변경자 IP';

-- ===== tb_notification_event =====
COMMENT ON TABLE tb_notification_event IS '알림 이벤트 큐';
COMMENT ON COLUMN tb_notification_event.notification_event_no IS 'PK';
COMMENT ON COLUMN tb_notification_event.event_type IS '이벤트 유형';
COMMENT ON COLUMN tb_notification_event.source_type IS '출처 유형';
COMMENT ON COLUMN tb_notification_event.source_id IS '출처 식별자';
COMMENT ON COLUMN tb_notification_event.severity IS '심각도';
COMMENT ON COLUMN tb_notification_event.summary IS '요약';
COMMENT ON COLUMN tb_notification_event.status IS '처리 상태';
COMMENT ON COLUMN tb_notification_event.attempt_count IS '시도 횟수';
COMMENT ON COLUMN tb_notification_event.last_error IS '마지막 오류';
COMMENT ON COLUMN tb_notification_event.next_retry_at IS '다음 재시도 일시';
COMMENT ON COLUMN tb_notification_event.reviewed IS '검토 완료 여부(boolean)';
COMMENT ON COLUMN tb_notification_event.reviewed_by IS '검토자 사용자 번호(FK)';
COMMENT ON COLUMN tb_notification_event.reviewed_at IS '검토 일시';
COMMENT ON COLUMN tb_notification_event.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_notification_event.lst_chg_dt IS '최종 변경 일시';

-- ===== tb_popular_query_daily =====
COMMENT ON TABLE tb_popular_query_daily IS '일별 인기 검색어 집계';
COMMENT ON COLUMN tb_popular_query_daily.stat_date IS '집계 일자(복합 PK)';
COMMENT ON COLUMN tb_popular_query_daily.normalized_query IS '정규화 질의(복합 PK)';
COMMENT ON COLUMN tb_popular_query_daily.query_text_sample IS '질의 원문 샘플';
COMMENT ON COLUMN tb_popular_query_daily.search_count IS '검색 횟수';
COMMENT ON COLUMN tb_popular_query_daily.no_result_count IS '무결과 횟수';
COMMENT ON COLUMN tb_popular_query_daily.top_result_scenario_no IS '최다 매칭 시나리오 번호(FK)';
COMMENT ON COLUMN tb_popular_query_daily.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_popular_query_daily.lst_chg_dt IS '최종 변경 일시';

-- ===== tb_role =====
COMMENT ON TABLE tb_role IS '보안 역할(로그인 권한)';
COMMENT ON COLUMN tb_role.role_no IS 'PK';
COMMENT ON COLUMN tb_role.code IS '역할 코드(ADMIN/OPERATOR 등)';
COMMENT ON COLUMN tb_role.description IS '설명';

-- ===== tb_scenario =====
COMMENT ON TABLE tb_scenario IS '시나리오';
COMMENT ON COLUMN tb_scenario.scenario_no IS 'PK';
COMMENT ON COLUMN tb_scenario.category_no IS '카테고리 번호(FK)';
COMMENT ON COLUMN tb_scenario.title IS '제목';
COMMENT ON COLUMN tb_scenario.description IS '설명';
COMMENT ON COLUMN tb_scenario.status IS '상태(DRAFT/ACTIVE/DELETED 등)';
COMMENT ON COLUMN tb_scenario.active_version_no IS '활성 버전 번호(FK)';
COMMENT ON COLUMN tb_scenario.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario.lst_chg_dt IS '최종 변경 일시';

-- ===== tb_scenario_category =====
COMMENT ON TABLE tb_scenario_category IS '시나리오 카테고리';
COMMENT ON COLUMN tb_scenario_category.scenario_category_no IS 'PK';
COMMENT ON COLUMN tb_scenario_category.p_scenario_category_no IS '상위 카테고리 번호(self FK)';
COMMENT ON COLUMN tb_scenario_category.name IS '카테고리명';
COMMENT ON COLUMN tb_scenario_category.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_scenario_category.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario_category.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_scenario_category.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_scenario_keyword =====
COMMENT ON TABLE tb_scenario_keyword IS '시나리오 매칭 키워드';
COMMENT ON COLUMN tb_scenario_keyword.scenario_keyword_no IS 'PK';
COMMENT ON COLUMN tb_scenario_keyword.scenario_no IS '시나리오 번호(FK)';
COMMENT ON COLUMN tb_scenario_keyword.keyword IS '키워드';
COMMENT ON COLUMN tb_scenario_keyword.weight IS '가중치';
COMMENT ON COLUMN tb_scenario_keyword.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario_keyword.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_scenario_keyword.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_scenario_node =====
COMMENT ON TABLE tb_scenario_node IS '시나리오 노드(대화 단계)';
COMMENT ON COLUMN tb_scenario_node.scenario_node_no IS 'PK';
COMMENT ON COLUMN tb_scenario_node.version_no IS '시나리오 버전 번호(FK)';
COMMENT ON COLUMN tb_scenario_node.node_key IS '노드 키';
COMMENT ON COLUMN tb_scenario_node.node_type IS '노드 유형';
COMMENT ON COLUMN tb_scenario_node.title IS '노드 제목';
COMMENT ON COLUMN tb_scenario_node.content IS '노드 본문';
COMMENT ON COLUMN tb_scenario_node.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_scenario_node.metadata IS '메타데이터(JSONB)';
COMMENT ON COLUMN tb_scenario_node.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario_node.lst_chg_dt IS '최종 변경 일시';

-- ===== tb_scenario_node_option =====
COMMENT ON TABLE tb_scenario_node_option IS '시나리오 노드 선택지';
COMMENT ON COLUMN tb_scenario_node_option.scenario_node_option_no IS 'PK';
COMMENT ON COLUMN tb_scenario_node_option.node_no IS '노드 번호(FK)';
COMMENT ON COLUMN tb_scenario_node_option.next_node_no IS '다음 노드 번호(FK)';
COMMENT ON COLUMN tb_scenario_node_option.label IS '선택지 라벨';
COMMENT ON COLUMN tb_scenario_node_option.condition_expr IS '분기 조건식';
COMMENT ON COLUMN tb_scenario_node_option.sort_order IS '정렬 순서';
COMMENT ON COLUMN tb_scenario_node_option.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario_node_option.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_scenario_node_option.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_scenario_synonym =====
COMMENT ON TABLE tb_scenario_synonym IS '키워드 동의어';
COMMENT ON COLUMN tb_scenario_synonym.scenario_synonym_no IS 'PK';
COMMENT ON COLUMN tb_scenario_synonym.keyword_no IS '키워드 번호(FK)';
COMMENT ON COLUMN tb_scenario_synonym.synonym IS '동의어';
COMMENT ON COLUMN tb_scenario_synonym.weight IS '가중치';
COMMENT ON COLUMN tb_scenario_synonym.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_scenario_synonym.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_scenario_synonym.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_scenario_version =====
COMMENT ON TABLE tb_scenario_version IS '시나리오 버전';
COMMENT ON COLUMN tb_scenario_version.scenario_version_no IS 'PK';
COMMENT ON COLUMN tb_scenario_version.scenario_no IS '시나리오 번호(FK)';
COMMENT ON COLUMN tb_scenario_version.version_no IS '버전 번호(정수 시퀀스)';
COMMENT ON COLUMN tb_scenario_version.status IS '상태(DRAFT/PUBLISHED/ARCHIVED)';
COMMENT ON COLUMN tb_scenario_version.start_node_no IS '시작 노드 번호(FK)';
COMMENT ON COLUMN tb_scenario_version.frst_regr_empno IS '최초 등록자(사용자명)';
COMMENT ON COLUMN tb_scenario_version.published_at IS '발행 일시';
COMMENT ON COLUMN tb_scenario_version.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_search_block_log =====
COMMENT ON TABLE tb_search_block_log IS '검색 차단 로그(PII 등 민감정보 탐지)';
COMMENT ON COLUMN tb_search_block_log.search_block_log_no IS 'PK';
COMMENT ON COLUMN tb_search_block_log.query_length IS '질의 길이';
COMMENT ON COLUMN tb_search_block_log.pii_types IS '탐지된 PII 유형';
COMMENT ON COLUMN tb_search_block_log.source IS '출처';
COMMENT ON COLUMN tb_search_block_log.anonymous_id_hash IS '익명 식별자 해시';
COMMENT ON COLUMN tb_search_block_log.user_no IS '사용자 번호(FK)';
COMMENT ON COLUMN tb_search_block_log.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_search_log =====
COMMENT ON TABLE tb_search_log IS '검색 로그';
COMMENT ON COLUMN tb_search_log.search_log_no IS 'PK';
COMMENT ON COLUMN tb_search_log.query_text IS '질의 원문';
COMMENT ON COLUMN tb_search_log.normalized_query IS '정규화 질의';
COMMENT ON COLUMN tb_search_log.result_count IS '결과 수';
COMMENT ON COLUMN tb_search_log.top_scenario_no IS '최상위 매칭 시나리오 번호(FK)';
COMMENT ON COLUMN tb_search_log.source IS '출처';
COMMENT ON COLUMN tb_search_log.latency_ms IS '지연(ms)';
COMMENT ON COLUMN tb_search_log.anonymous_id_hash IS '익명 식별자 해시';
COMMENT ON COLUMN tb_search_log.user_no IS '사용자 번호(FK)';
COMMENT ON COLUMN tb_search_log.frst_reg_dt IS '최초 등록 일시';

-- ===== tb_user =====
COMMENT ON TABLE tb_user IS '사용자(관리자/운영자 계정)';
COMMENT ON COLUMN tb_user.user_no IS 'PK';
COMMENT ON COLUMN tb_user.username IS '로그인 ID';
COMMENT ON COLUMN tb_user.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN tb_user.display_name IS '표시 이름';
COMMENT ON COLUMN tb_user.failed_attempts IS '로그인 실패 횟수';
COMMENT ON COLUMN tb_user.locked_until IS '잠금 해제 일시';
COMMENT ON COLUMN tb_user.last_login_at IS '최근 로그인 일시';
COMMENT ON COLUMN tb_user.frst_reg_dt IS '최초 등록 일시';
COMMENT ON COLUMN tb_user.lst_chg_dt IS '최종 변경 일시';
COMMENT ON COLUMN tb_user.must_change_password IS '비밀번호 변경 필요 여부(boolean)';
COMMENT ON COLUMN tb_user.use_yn IS '사용 여부 (Y/N, 논리 삭제 플래그)';

-- ===== tb_user_role =====
COMMENT ON TABLE tb_user_role IS '사용자-역할 매핑';
COMMENT ON COLUMN tb_user_role.user_no IS '사용자 번호(FK)';
COMMENT ON COLUMN tb_user_role.role_no IS '역할 번호(FK)';
