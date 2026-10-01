CREATE TABLE tb_admin_ip_whitelist (
    admin_ip_whitelist_no BIGSERIAL PRIMARY KEY,
    ip_cidr varchar(64) NOT NULL,
    description varchar(200),
    use_yn char(1) NOT NULL DEFAULT 'Y',
    frst_regr_empno varchar(64),
    frst_reg_dt timestamptz NOT NULL DEFAULT now(),
    lst_chgr_empno varchar(64),
    lst_chg_dt timestamptz NOT NULL DEFAULT now(),
    frst_regr_ip varchar(64),
    lst_chgr_ip varchar(64),
    CONSTRAINT ck_admin_ip_whitelist_useyn CHECK (use_yn IN ('Y','N'))
);

CREATE INDEX ix_admin_ip_whitelist_useyn ON tb_admin_ip_whitelist (use_yn);

DO $$
DECLARE
    manage_group_no BIGINT;
    ip_menu_no BIGINT;
BEGIN
    IF to_regclass('tb_menu') IS NOT NULL THEN
        SELECT menu_no
        INTO manage_group_no
        FROM tb_menu
        WHERE menu_url IS NULL
          AND use_yn = 'Y'
          AND (menu_nm = 'CMS management' OR menu_nm = 'CMS 관리' OR menu_nm = '관리')
        ORDER BY menu_no
        LIMIT 1;

        IF manage_group_no IS NULL THEN
            SELECT p_menu_no
            INTO manage_group_no
            FROM tb_menu
            WHERE menu_url = '/admin/manage/permissions'
              AND use_yn = 'Y'
            ORDER BY menu_no
            LIMIT 1;
        END IF;

        IF manage_group_no IS NOT NULL THEN
            SELECT menu_no
            INTO ip_menu_no
            FROM tb_menu
            WHERE menu_url = '/admin/manage/ip-whitelist'
            ORDER BY menu_no
            LIMIT 1;

            IF ip_menu_no IS NULL THEN
                INSERT INTO tb_menu (
                    p_menu_no, menu_nm, menu_type, menu_depth, sort_order, target_blank_yn,
                    menu_url, open_yn, use_yn, frst_regr_empno, lst_chgr_empno
                )
                VALUES (
                    manage_group_no,
                    '관리자 IP 허용목록',
                    'ADM',
                    2,
                    40,
                    'N',
                    '/admin/manage/ip-whitelist',
                    'Y',
                    'Y',
                    'system',
                    'system'
                )
                RETURNING menu_no INTO ip_menu_no;
            ELSE
                UPDATE tb_menu
                SET p_menu_no = manage_group_no,
                    menu_nm = '관리자 IP 허용목록',
                    sort_order = 40,
                    open_yn = 'Y',
                    use_yn = 'Y',
                    lst_chg_dt = now(),
                    lst_chgr_empno = 'system'
                WHERE menu_no = ip_menu_no;
            END IF;

            INSERT INTO tb_auth_menu_adm (
                auth_no, menu_no, p_menu_no, select_yn, insert_yn, update_yn, delete_yn, proc_yn,
                use_yn, frst_regr_empno, lst_chgr_empno
            )
            SELECT auth_no,
                   ip_menu_no,
                   manage_group_no,
                   'Y',
                   'Y',
                   'Y',
                   'Y',
                   'Y',
                   'Y',
                   'system',
                   'system'
            FROM tb_auth
            WHERE auth_nm = 'ADMIN'
              AND use_yn = 'Y'
            ON CONFLICT (auth_no, menu_no) DO UPDATE
            SET p_menu_no = EXCLUDED.p_menu_no,
                select_yn = 'Y',
                insert_yn = 'Y',
                update_yn = 'Y',
                delete_yn = 'Y',
                proc_yn = 'Y',
                use_yn = 'Y',
                lst_chg_dt = now(),
                lst_chgr_empno = 'system';
        END IF;
    END IF;
END $$;
