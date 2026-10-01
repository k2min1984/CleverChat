-- A detail code is grouped by p_code_no. Only hierarchy group rows need a unique code_group.
DROP INDEX IF EXISTS uq_tb_code_code_group_active;

CREATE UNIQUE INDEX uq_tb_code_code_group_active
    ON tb_code(code_group)
    WHERE use_yn = 'Y'
      AND code_group IS NOT NULL
      AND code_depth IN (1, 2);
