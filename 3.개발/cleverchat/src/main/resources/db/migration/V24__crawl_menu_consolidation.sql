-- Consolidate crawl operation screens into one administrator entry.
DO $$
BEGIN
    IF to_regclass('tb_menu') IS NOT NULL THEN
        UPDATE tb_menu
        SET menu_nm = '크롤링 관리',
            sort_order = 40,
            open_yn = 'Y',
            use_yn = 'Y',
            lst_chg_dt = NOW(),
            lst_chgr_empno = 'system'
        WHERE menu_url = '/admin/crawl-targets';

        UPDATE tb_menu
        SET open_yn = 'N',
            lst_chg_dt = NOW(),
            lst_chgr_empno = 'system'
        WHERE menu_url IN ('/admin/crawl-documents', '/admin/crawl-runs');
    END IF;
END $$;
