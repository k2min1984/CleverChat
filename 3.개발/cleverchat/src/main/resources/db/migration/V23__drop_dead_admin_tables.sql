-- V23: 死테이블 제거 (admin_* 중복 정리)
-- tb_admin_code/tb_admin_menu/tb_admin_role_menu 는 V18에서 생성됐으나
-- V19(legacy_cms_manage_alignment)에서 tb_code/tb_menu/tb_auth 로 일원화되며 폐기됨.
-- 살아있는 소스가 참조하지 않고(FK 없음) 데이터도 이미 이관 완료 → 안전 제거.
SET search_path TO cleverchat_dev, public;

DROP TABLE IF EXISTS tb_admin_role_menu;
DROP TABLE IF EXISTS tb_admin_menu;
DROP TABLE IF EXISTS tb_admin_code;
