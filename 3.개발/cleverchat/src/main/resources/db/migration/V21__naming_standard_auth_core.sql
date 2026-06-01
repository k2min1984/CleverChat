-- V21: 인증핵심 6개 테이블 OverseasNPP 명명표준 정렬 (users/roles/user_roles + admin_* 死테이블)
-- enabled(boolean)->use_yn(char1 Y/N), id->{tbl}_no(단수: user_no/role_no), created_at->frst_reg_dt 등
-- 도메인 테이블의 users FK는 users.id->user_no 리네임 시 PostgreSQL이 자동 갱신
SET search_path TO cleverchat_dev, public;

-- ===== users -> tb_user =====
ALTER TABLE users ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE users SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE users DROP COLUMN enabled;
ALTER TABLE users RENAME COLUMN id TO user_no;
ALTER TABLE users RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE users RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE users RENAME TO tb_user;
ALTER SEQUENCE users_id_seq RENAME TO tb_user_user_no_seq;

-- ===== roles -> tb_role =====
ALTER TABLE roles RENAME COLUMN id TO role_no;
ALTER TABLE roles RENAME TO tb_role;
ALTER SEQUENCE roles_id_seq RENAME TO tb_role_role_no_seq;

-- ===== user_roles -> tb_user_role =====
ALTER TABLE user_roles RENAME COLUMN user_id TO user_no;
ALTER TABLE user_roles RENAME COLUMN role_id TO role_no;
ALTER TABLE user_roles RENAME TO tb_user_role;

-- ===== admin_code -> tb_admin_code (현재 미사용 테이블, 표준 정렬) =====
DROP INDEX IF EXISTS idx_admin_code_enabled;
ALTER TABLE admin_code ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE admin_code SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE admin_code DROP COLUMN enabled;
ALTER TABLE admin_code RENAME COLUMN id TO admin_code_no;
ALTER TABLE admin_code RENAME COLUMN parent_id TO p_admin_code_no;
ALTER TABLE admin_code RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE admin_code RENAME COLUMN updated_by TO lst_chgr_empno;
ALTER TABLE admin_code RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE admin_code RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE admin_code ADD COLUMN frst_regr_ip varchar(64);
ALTER TABLE admin_code ADD COLUMN lst_chgr_ip varchar(64);
ALTER TABLE admin_code RENAME TO tb_admin_code;
ALTER SEQUENCE admin_code_id_seq RENAME TO tb_admin_code_admin_code_no_seq;
CREATE INDEX ix_admin_code_use_yn ON tb_admin_code (use_yn);

-- ===== admin_menu -> tb_admin_menu (현재 미사용 테이블, 표준 정렬) =====
DROP INDEX IF EXISTS idx_admin_menu_enabled_visible;
ALTER TABLE admin_menu ADD COLUMN use_yn char(1) NOT NULL DEFAULT 'Y';
UPDATE admin_menu SET use_yn = CASE WHEN enabled THEN 'Y' ELSE 'N' END;
ALTER TABLE admin_menu DROP COLUMN enabled;
ALTER TABLE admin_menu RENAME COLUMN id TO admin_menu_no;
ALTER TABLE admin_menu RENAME COLUMN parent_id TO p_admin_menu_no;
ALTER TABLE admin_menu RENAME COLUMN created_by TO frst_regr_empno;
ALTER TABLE admin_menu RENAME COLUMN updated_by TO lst_chgr_empno;
ALTER TABLE admin_menu RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE admin_menu RENAME COLUMN updated_at TO lst_chg_dt;
ALTER TABLE admin_menu ADD COLUMN frst_regr_ip varchar(64);
ALTER TABLE admin_menu ADD COLUMN lst_chgr_ip varchar(64);
ALTER TABLE admin_menu RENAME TO tb_admin_menu;
ALTER SEQUENCE admin_menu_id_seq RENAME TO tb_admin_menu_admin_menu_no_seq;
CREATE INDEX ix_admin_menu_use_yn_visible ON tb_admin_menu (use_yn, visible);

-- ===== admin_role_menu -> tb_admin_role_menu (현재 미사용 테이블, 표준 정렬) =====
ALTER TABLE admin_role_menu RENAME COLUMN role_id TO role_no;
ALTER TABLE admin_role_menu RENAME COLUMN menu_id TO menu_no;
ALTER TABLE admin_role_menu RENAME COLUMN created_at TO frst_reg_dt;
ALTER TABLE admin_role_menu RENAME TO tb_admin_role_menu;
