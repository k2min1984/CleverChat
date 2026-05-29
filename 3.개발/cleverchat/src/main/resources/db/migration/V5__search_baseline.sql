CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX ix_scenario_search_tsv
    ON scenario USING GIN (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, ''))
    );

CREATE INDEX ix_scenario_title_trgm
    ON scenario USING GIN (title gin_trgm_ops);

CREATE INDEX ix_scenario_node_search_tsv
    ON scenario_node USING GIN (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
    );

CREATE INDEX ix_scenario_node_content_trgm
    ON scenario_node USING GIN (content gin_trgm_ops);

CREATE TABLE search_log (
    id BIGSERIAL PRIMARY KEY,
    query_text VARCHAR(200) NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    result_count INTEGER NOT NULL DEFAULT 0,
    top_scenario_id BIGINT NULL,
    source VARCHAR(30) NOT NULL,
    latency_ms INTEGER NULL,
    anonymous_id_hash VARCHAR(64) NULL,
    user_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_search_log_top_scenario FOREIGN KEY (top_scenario_id) REFERENCES scenario(id) ON DELETE SET NULL,
    CONSTRAINT fk_search_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_search_log_source CHECK (source IN ('CHAT_FALLBACK', 'ADMIN_TEST', 'API'))
);

CREATE INDEX ix_search_log_created
    ON search_log (created_at DESC);

CREATE INDEX ix_search_log_normalized_created
    ON search_log (normalized_query, created_at DESC);

CREATE TABLE search_block_log (
    id BIGSERIAL PRIMARY KEY,
    query_length INTEGER NOT NULL,
    pii_types VARCHAR(200) NOT NULL,
    source VARCHAR(30) NOT NULL,
    anonymous_id_hash VARCHAR(64) NULL,
    user_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_search_block_log_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_search_block_log_source CHECK (source IN ('CHAT_FALLBACK', 'ADMIN_TEST', 'API'))
);

CREATE INDEX ix_search_block_log_created
    ON search_block_log (created_at DESC);

CREATE TABLE popular_query_daily (
    stat_date DATE NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    query_text_sample VARCHAR(200) NOT NULL,
    search_count INTEGER NOT NULL DEFAULT 0,
    no_result_count INTEGER NOT NULL DEFAULT 0,
    top_result_scenario_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (stat_date, normalized_query),
    CONSTRAINT fk_popular_query_top_scenario FOREIGN KEY (top_result_scenario_id) REFERENCES scenario(id) ON DELETE SET NULL
);

CREATE INDEX ix_popular_query_daily_stat
    ON popular_query_daily (stat_date DESC, search_count DESC);
