DO $$
DECLARE
    scenario_group_no BIGINT;
    order_menu_no BIGINT;
BEGIN
    IF to_regclass('tb_menu') IS NOT NULL THEN
        SELECT p_menu_no
        INTO scenario_group_no
        FROM tb_menu
        WHERE menu_url = '/admin/scenarios'
          AND use_yn = 'Y'
        ORDER BY menu_no
        LIMIT 1;

        IF scenario_group_no IS NOT NULL THEN
            SELECT menu_no
            INTO order_menu_no
            FROM tb_menu
            WHERE menu_url = '/admin/scenarios/order'
            ORDER BY menu_no
            LIMIT 1;

            IF order_menu_no IS NULL THEN
                INSERT INTO tb_menu (
                    p_menu_no, menu_nm, menu_type, menu_depth, sort_order, target_blank_yn,
                    menu_url, open_yn, use_yn, frst_regr_empno, lst_chgr_empno
                )
                VALUES (
                    scenario_group_no,
                    '시나리오 정렬',
                    'ADM',
                    2,
                    20,
                    'N',
                    '/admin/scenarios/order',
                    'Y',
                    'Y',
                    'system',
                    'system'
                )
                RETURNING menu_no INTO order_menu_no;
            ELSE
                UPDATE tb_menu
                SET p_menu_no = scenario_group_no,
                    menu_nm = '시나리오 정렬',
                    sort_order = 20,
                    open_yn = 'Y',
                    use_yn = 'Y',
                    lst_chg_dt = NOW(),
                    lst_chgr_empno = 'system'
                WHERE menu_no = order_menu_no;
            END IF;

            INSERT INTO tb_auth_menu_adm (
                auth_no, menu_no, p_menu_no, select_yn, insert_yn, update_yn, delete_yn, proc_yn,
                use_yn, frst_regr_empno, lst_chgr_empno
            )
            SELECT auth_no,
                   order_menu_no,
                   scenario_group_no,
                   'Y',
                   'N',
                   'Y',
                   'N',
                   'Y',
                   'Y',
                   'system',
                   'system'
            FROM tb_auth_menu_adm
            WHERE menu_no = (
                SELECT menu_no
                FROM tb_menu
                WHERE menu_url = '/admin/scenarios'
                  AND use_yn = 'Y'
                ORDER BY menu_no
                LIMIT 1
            )
              AND use_yn = 'Y'
              AND select_yn = 'Y'
            ON CONFLICT (auth_no, menu_no) DO UPDATE
            SET p_menu_no = EXCLUDED.p_menu_no,
                select_yn = 'Y',
                update_yn = 'Y',
                proc_yn = 'Y',
                use_yn = 'Y',
                lst_chg_dt = NOW(),
                lst_chgr_empno = 'system';
        END IF;
    END IF;
END $$;
