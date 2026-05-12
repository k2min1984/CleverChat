CREATE TABLE scenario_category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_category_parent
        FOREIGN KEY (parent_id) REFERENCES scenario_category(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX ux_scenario_category_root_name
    ON scenario_category (name)
    WHERE parent_id IS NULL;

CREATE UNIQUE INDEX ux_scenario_category_parent_name
    ON scenario_category (parent_id, name)
    WHERE parent_id IS NOT NULL;

CREATE INDEX ix_scenario_category_parent_sort
    ON scenario_category (parent_id, sort_order, id);

CREATE TABLE scenario (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    active_version_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_category
        FOREIGN KEY (category_id) REFERENCES scenario_category(id),
    CONSTRAINT ck_scenario_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX ix_scenario_category_status
    ON scenario (category_id, status, id DESC);

CREATE INDEX ix_scenario_title
    ON scenario (title);

CREATE TABLE scenario_version (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_node_id BIGINT NULL,
    created_by VARCHAR(100) NOT NULL,
    published_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_version_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenario(id) ON DELETE CASCADE,
    CONSTRAINT ux_scenario_version_no
        UNIQUE (scenario_id, version_no),
    CONSTRAINT ck_scenario_version_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX ux_scenario_version_published_one
    ON scenario_version (scenario_id)
    WHERE status = 'PUBLISHED';

CREATE INDEX ix_scenario_version_scenario_status
    ON scenario_version (scenario_id, status, version_no DESC);

CREATE TABLE scenario_node (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL,
    node_key VARCHAR(80) NOT NULL,
    node_type VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    content TEXT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_node_version
        FOREIGN KEY (version_id) REFERENCES scenario_version(id) ON DELETE CASCADE,
    CONSTRAINT ux_scenario_node_key
        UNIQUE (version_id, node_key),
    CONSTRAINT ck_scenario_node_type
        CHECK (node_type IN ('QUESTION', 'ANSWER', 'BRANCH', 'END'))
);

CREATE INDEX ix_scenario_node_version_sort
    ON scenario_node (version_id, sort_order, id);

CREATE TABLE scenario_node_option (
    id BIGSERIAL PRIMARY KEY,
    node_id BIGINT NOT NULL,
    next_node_id BIGINT NULL,
    label VARCHAR(150) NOT NULL,
    condition_expr VARCHAR(500) NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_node_option_node
        FOREIGN KEY (node_id) REFERENCES scenario_node(id) ON DELETE CASCADE,
    CONSTRAINT fk_scenario_node_option_next
        FOREIGN KEY (next_node_id) REFERENCES scenario_node(id) ON DELETE SET NULL
);

CREATE INDEX ix_scenario_node_option_node_sort
    ON scenario_node_option (node_id, sort_order, id);

CREATE INDEX ix_scenario_node_option_next
    ON scenario_node_option (next_node_id);

CREATE TABLE scenario_keyword (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    weight INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_keyword_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenario(id) ON DELETE CASCADE,
    CONSTRAINT ux_scenario_keyword
        UNIQUE (scenario_id, keyword),
    CONSTRAINT ck_scenario_keyword_weight
        CHECK (weight BETWEEN 0 AND 100)
);

CREATE INDEX ix_scenario_keyword_scenario_enabled
    ON scenario_keyword (scenario_id, enabled, weight DESC);

CREATE TABLE scenario_synonym (
    id BIGSERIAL PRIMARY KEY,
    keyword_id BIGINT NOT NULL,
    synonym VARCHAR(100) NOT NULL,
    weight INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_scenario_synonym_keyword
        FOREIGN KEY (keyword_id) REFERENCES scenario_keyword(id) ON DELETE CASCADE,
    CONSTRAINT ux_scenario_synonym
        UNIQUE (keyword_id, synonym),
    CONSTRAINT ck_scenario_synonym_weight
        CHECK (weight BETWEEN 0 AND 100)
);

CREATE INDEX ix_scenario_synonym_keyword_enabled
    ON scenario_synonym (keyword_id, enabled, weight DESC);

ALTER TABLE scenario
    ADD CONSTRAINT fk_scenario_active_version
    FOREIGN KEY (active_version_id) REFERENCES scenario_version(id) ON DELETE SET NULL;

ALTER TABLE scenario_version
    ADD CONSTRAINT fk_scenario_version_start_node
    FOREIGN KEY (start_node_id) REFERENCES scenario_node(id) ON DELETE SET NULL;
