-- Rebuild tb_code in hierarchy order so the logical root starts at code_no 1.
-- No application table has a foreign key to tb_code.code_no at this baseline.

CREATE TEMP TABLE tmp_tb_code_catalog ON COMMIT DROP AS
SELECT code.code_no AS old_code_no,
       parent.code_group AS parent_code_group,
       code.code_group,
       code.code_nm,
       code.code_val,
       code.code_dc,
       code.code_depth,
       code.sort_order,
       code.use_yn,
       code.frst_regr_empno,
       code.frst_reg_dt,
       code.lst_chgr_empno,
       code.lst_chg_dt,
       code.frst_regr_ip,
       code.lst_chgr_ip
FROM tb_code code
LEFT JOIN tb_code parent ON parent.code_no = code.p_code_no;

TRUNCATE TABLE tb_code RESTART IDENTITY;

INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt,
    frst_regr_ip, lst_chgr_ip
)
SELECT 0, code_group, code_nm, code_val, code_dc, 1, sort_order,
       use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt,
       frst_regr_ip, lst_chgr_ip
FROM tmp_tb_code_catalog
WHERE code_depth = 1
ORDER BY sort_order, old_code_no;

INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt,
    frst_regr_ip, lst_chgr_ip
)
SELECT parent.code_no, child.code_group, child.code_nm, child.code_val, child.code_dc,
       2, child.sort_order, child.use_yn,
       child.frst_regr_empno, child.frst_reg_dt, child.lst_chgr_empno, child.lst_chg_dt,
       child.frst_regr_ip, child.lst_chgr_ip
FROM tmp_tb_code_catalog child
JOIN tb_code parent
  ON parent.code_group = child.parent_code_group
 AND parent.code_depth = 1
WHERE child.code_depth = 2
ORDER BY parent.sort_order, child.sort_order, child.old_code_no;

INSERT INTO tb_code (
    p_code_no, code_group, code_nm, code_val, code_dc, code_depth, sort_order,
    use_yn, frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt,
    frst_regr_ip, lst_chgr_ip
)
SELECT parent.code_no, child.code_group, child.code_nm, child.code_val, child.code_dc,
       3, child.sort_order, child.use_yn,
       child.frst_regr_empno, child.frst_reg_dt, child.lst_chgr_empno, child.lst_chg_dt,
       child.frst_regr_ip, child.lst_chgr_ip
FROM tmp_tb_code_catalog child
JOIN tb_code parent
  ON parent.code_group = child.parent_code_group
 AND parent.code_depth = 2
WHERE child.code_depth = 3
ORDER BY parent.sort_order, child.sort_order, child.old_code_no;

SELECT setval(
    pg_get_serial_sequence('tb_code', 'code_no'),
    GREATEST((SELECT COALESCE(MAX(code_no), 0) FROM tb_code), 1),
    true
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM tb_code
        WHERE code_no = 1
          AND p_code_no = 0
          AND code_group = 'SYSTEM_CODE'
          AND code_depth = 1
    ) THEN
        RAISE EXCEPTION 'SYSTEM_CODE must be rebuilt as code_no 1';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM tb_code child
        LEFT JOIN tb_code parent ON parent.code_no = child.p_code_no
        WHERE (child.code_depth = 1 AND child.p_code_no <> 0)
           OR (child.code_depth > 1 AND parent.code_depth <> child.code_depth - 1)
    ) THEN
        RAISE EXCEPTION 'Invalid tb_code hierarchy after renumbering';
    END IF;
END $$;
