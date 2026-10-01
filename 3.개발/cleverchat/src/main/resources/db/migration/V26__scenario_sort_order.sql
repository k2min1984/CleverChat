ALTER TABLE tb_scenario
    ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;

UPDATE tb_scenario
SET sort_order = scenario_no
WHERE sort_order = 0;

CREATE INDEX IF NOT EXISTS ix_scenario_sort_order
    ON tb_scenario (sort_order DESC, scenario_no DESC);

COMMENT ON COLUMN tb_scenario.sort_order IS '시나리오 노출 정렬 순서';
