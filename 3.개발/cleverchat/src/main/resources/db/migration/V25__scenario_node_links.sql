CREATE TABLE IF NOT EXISTS tb_scenario_node_link (
    scenario_node_link_no BIGSERIAL PRIMARY KEY,
    node_no BIGINT NOT NULL,
    label VARCHAR(150) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    link_type VARCHAR(20) NOT NULL DEFAULT 'EXTERNAL',
    sort_order INTEGER NOT NULL DEFAULT 0,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    frst_reg_dt TIMESTAMPTZ NOT NULL DEFAULT now(),
    lst_chg_dt TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_node_link_node
        FOREIGN KEY (node_no) REFERENCES tb_scenario_node(scenario_node_no) ON DELETE CASCADE,
    CONSTRAINT ck_scenario_node_link_type
        CHECK (link_type IN ('EXTERNAL', 'INTERNAL', 'DOWNLOAD')),
    CONSTRAINT ck_scenario_node_link_useyn
        CHECK (use_yn IN ('Y', 'N'))
);

CREATE INDEX IF NOT EXISTS ix_scenario_node_link_node_sort
    ON tb_scenario_node_link (node_no, sort_order, scenario_node_link_no);

CREATE INDEX IF NOT EXISTS ix_scenario_node_link_node_useyn
    ON tb_scenario_node_link (node_no, use_yn, sort_order, scenario_node_link_no);
