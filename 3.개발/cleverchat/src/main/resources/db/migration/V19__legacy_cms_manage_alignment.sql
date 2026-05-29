-- Align CleverChat admin management with the legacy CMS source baseline.
-- Canonical CMS tables are TB_CODE, TB_MENU, TB_AUTH, TB_AUTH_MENU_ADM.

CREATE SEQUENCE IF NOT EXISTS seq_tb_menu_menu_no START WITH 1000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_tb_auth_auth_no START WITH 1000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS tb_code (
    code_no          BIGSERIAL PRIMARY KEY,
    p_code_no        BIGINT NOT NULL DEFAULT 0,
    code_group       VARCHAR(64),
    code_nm          VARCHAR(200) NOT NULL,
    code_val         VARCHAR(200),
    code_dc          VARCHAR(500),
    code_depth       BIGINT NOT NULL DEFAULT 1,
    sort_order       BIGINT NOT NULL DEFAULT 1,
    use_yn           CHAR(1) NOT NULL DEFAULT 'Y',
    frst_regr_empno  VARCHAR(64),
    frst_reg_dt      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lst_chgr_empno   VARCHAR(64),
    lst_chg_dt       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    frst_regr_ip     VARCHAR(64),
    lst_chgr_ip      VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_tb_code_parent_sort ON tb_code(p_code_no, sort_order, code_no);
CREATE INDEX IF NOT EXISTS idx_tb_code_use_yn ON tb_code(use_yn);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tb_code_code_group_active ON tb_code(code_group) WHERE use_yn = 'Y' AND code_group IS NOT NULL;

CREATE TABLE IF NOT EXISTS tb_menu (
    menu_no          BIGINT PRIMARY KEY DEFAULT nextval('seq_tb_menu_menu_no'),
    frst_reg_dt      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    frst_regr_empno  VARCHAR(64),
    lst_chg_dt       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lst_chgr_empno   VARCHAR(64),
    p_menu_no        BIGINT NOT NULL DEFAULT 0,
    menu_nm          VARCHAR(100) NOT NULL,
    menu_type        VARCHAR(30) NOT NULL DEFAULT 'ADM',
    board_mng_no     BIGINT,
    menu_depth       BIGINT NOT NULL DEFAULT 1,
    sort_order       BIGINT NOT NULL DEFAULT 1,
    target_blank_yn  CHAR(1) NOT NULL DEFAULT 'N',
    menu_url         VARCHAR(300),
    rel_url          VARCHAR(1000),
    open_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    use_yn           CHAR(1) NOT NULL DEFAULT 'Y',
    frst_regr_ip     VARCHAR(64),
    lst_chgr_ip      VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_tb_menu_parent_sort ON tb_menu(p_menu_no, sort_order, menu_no);
CREATE INDEX IF NOT EXISTS idx_tb_menu_url ON tb_menu(menu_url);
CREATE INDEX IF NOT EXISTS idx_tb_menu_open_use ON tb_menu(open_yn, use_yn);

CREATE TABLE IF NOT EXISTS tb_auth (
    auth_no          BIGINT PRIMARY KEY DEFAULT nextval('seq_tb_auth_auth_no'),
    auth_nm          VARCHAR(100) NOT NULL,
    auth_dc          VARCHAR(500),
    sort_order       BIGINT NOT NULL DEFAULT 1,
    use_yn           CHAR(1) NOT NULL DEFAULT 'Y',
    frst_regr_empno  VARCHAR(64),
    frst_reg_dt      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lst_chgr_empno   VARCHAR(64),
    lst_chg_dt       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dept_cd          VARCHAR(64),
    auth_rank        VARCHAR(64),
    frst_regr_ip     VARCHAR(64),
    lst_chgr_ip      VARCHAR(64)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tb_auth_nm_active ON tb_auth(auth_nm) WHERE use_yn = 'Y';
CREATE INDEX IF NOT EXISTS idx_tb_auth_sort ON tb_auth(sort_order, auth_no);

CREATE TABLE IF NOT EXISTS tb_auth_menu_adm (
    auth_no          BIGINT NOT NULL REFERENCES tb_auth(auth_no) ON DELETE CASCADE,
    menu_no          BIGINT NOT NULL REFERENCES tb_menu(menu_no) ON DELETE CASCADE,
    p_menu_no        BIGINT,
    select_yn        CHAR(1) NOT NULL DEFAULT 'Y',
    insert_yn        CHAR(1) NOT NULL DEFAULT 'Y',
    update_yn        CHAR(1) NOT NULL DEFAULT 'Y',
    delete_yn        CHAR(1) NOT NULL DEFAULT 'Y',
    proc_yn          CHAR(1) NOT NULL DEFAULT 'Y',
    use_yn           CHAR(1) NOT NULL DEFAULT 'Y',
    frst_regr_empno  VARCHAR(64),
    frst_reg_dt      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    lst_chgr_empno   VARCHAR(64),
    lst_chg_dt       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    frst_regr_ip     VARCHAR(64),
    lst_chgr_ip      VARCHAR(64),
    PRIMARY KEY (auth_no, menu_no)
);
CREATE INDEX IF NOT EXISTS idx_tb_auth_menu_adm_menu_no ON tb_auth_menu_adm(menu_no);
CREATE INDEX IF NOT EXISTS idx_tb_auth_menu_adm_use_yn ON tb_auth_menu_adm(use_yn);

INSERT INTO tb_auth (auth_no, auth_nm, auth_dc, sort_order, use_yn, frst_regr_empno, lst_chgr_empno)
SELECT r.id, r.code, r.description, row_number() OVER (ORDER BY r.code), 'Y', 'system', 'system'
FROM roles r
ON CONFLICT (auth_no) DO UPDATE
SET auth_nm = EXCLUDED.auth_nm,
    auth_dc = EXCLUDED.auth_dc,
    lst_chg_dt = NOW(),
    lst_chgr_empno = 'system';

INSERT INTO tb_code (
    code_no, p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, lst_chgr_empno
)
SELECT id,
       COALESCE(parent_id, 0),
       code,
       name,
       value,
       description,
       CASE WHEN parent_id IS NULL THEN 1 ELSE 2 END,
       COALESCE(sort_order, 1),
       CASE WHEN enabled THEN 'Y' ELSE 'N' END,
       'system',
       'system'
FROM admin_code
ON CONFLICT (code_no) DO NOTHING;

INSERT INTO tb_menu (
    menu_no, p_menu_no, menu_nm, menu_type, menu_depth, sort_order, target_blank_yn,
    menu_url, open_yn, use_yn, frst_regr_empno, lst_chgr_empno
)
SELECT id,
       COALESCE(parent_id, 0),
       title,
       'ADM',
       CASE WHEN parent_id IS NULL THEN 1 ELSE 2 END,
       COALESCE(sort_order, 1),
       'N',
       url,
       CASE WHEN visible THEN 'Y' ELSE 'N' END,
       CASE WHEN enabled THEN 'Y' ELSE 'N' END,
       'system',
       'system'
FROM admin_menu
ON CONFLICT (menu_no) DO NOTHING;

INSERT INTO tb_auth_menu_adm (
    auth_no, menu_no, p_menu_no, select_yn, insert_yn, update_yn, delete_yn, proc_yn,
    use_yn, frst_regr_empno, lst_chgr_empno
)
SELECT arm.role_id,
       arm.menu_id,
       m.p_menu_no,
       CASE WHEN arm.can_read THEN 'Y' ELSE 'N' END,
       'Y',
       'Y',
       'Y',
       'Y',
       'Y',
       'system',
       'system'
FROM admin_role_menu arm
JOIN tb_menu m ON m.menu_no = arm.menu_id
ON CONFLICT (auth_no, menu_no) DO NOTHING;

SELECT setval('seq_tb_menu_menu_no', GREATEST((SELECT COALESCE(MAX(menu_no), 0) FROM tb_menu) + 1, 1000), false);
SELECT setval('seq_tb_auth_auth_no', GREATEST((SELECT COALESCE(MAX(auth_no), 0) FROM tb_auth) + 1, 1000), false);
