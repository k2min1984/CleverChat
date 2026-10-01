CREATE TABLE tb_system_settings (
    id integer PRIMARY KEY CHECK (id = 1),
    operation_mode varchar(10) NOT NULL DEFAULT 'LOCAL' CHECK (operation_mode IN ('LOCAL','GATEWAY')),
    gateway_url varchar(1000),
    service_id varchar(32) NOT NULL DEFAULT 'cleverchat',
    crawl_export_directory varchar(1000),
    version bigint NOT NULL DEFAULT 0,
    auth_version bigint NOT NULL DEFAULT 0,
    updated_by varchar(64),
    updated_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO tb_system_settings(id) VALUES(1);

-- A NULL target folder now means to inherit the system default at export time.
ALTER TABLE tb_crawl_target DROP CONSTRAINT ck_crawl_target_json_export_directory;

ALTER TABLE tb_user ADD COLUMN auth_source varchar(10) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN emp_no varchar(64), ADD COLUMN unit varchar(200);
ALTER TABLE tb_user ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE tb_user ADD CONSTRAINT ck_user_identity_source CHECK (
    (auth_source='LOCAL' AND password_hash IS NOT NULL) OR
    (auth_source='GATEWAY' AND password_hash IS NULL AND emp_no IS NOT NULL));
CREATE UNIQUE INDEX ux_user_gateway_emp_no ON tb_user(emp_no) WHERE auth_source='GATEWAY';

DO $$
DECLARE parent_no bigint; settings_no bigint;
BEGIN
    SELECT p_menu_no INTO parent_no FROM tb_menu WHERE menu_url='/admin/manage/permissions' AND use_yn='Y' ORDER BY menu_no LIMIT 1;
    IF parent_no IS NOT NULL THEN
        INSERT INTO tb_menu(p_menu_no,menu_nm,menu_type,menu_depth,sort_order,target_blank_yn,menu_url,open_yn,use_yn,frst_regr_empno,lst_chgr_empno)
        VALUES(parent_no,'시스템 설정','ADM',2,50,'N','/admin/system-settings','Y','Y','system','system') RETURNING menu_no INTO settings_no;
        INSERT INTO tb_auth_menu_adm(auth_no,menu_no,p_menu_no,select_yn,insert_yn,update_yn,delete_yn,proc_yn,use_yn,frst_regr_empno,lst_chgr_empno)
        SELECT auth_no,settings_no,parent_no,'Y','N','Y','N','Y','Y','system','system' FROM tb_auth WHERE auth_nm='ADMIN' AND use_yn='Y';
    END IF;
END $$;
