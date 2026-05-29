CREATE TABLE crawl_target (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(1000) NOT NULL,
    label VARCHAR(200) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_run_at TIMESTAMPTZ NULL,
    last_status VARCHAR(20) NULL,
    last_message VARCHAR(1000) NULL,
    CONSTRAINT ux_crawl_target_url UNIQUE (url),
    CONSTRAINT fk_crawl_target_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_crawl_target_last_status CHECK (last_status IS NULL OR last_status IN ('SUCCESS', 'FAILED', 'DUPLICATE'))
);

CREATE INDEX ix_crawl_target_enabled_updated ON crawl_target (enabled, updated_at DESC);

CREATE TABLE crawl_document (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    title VARCHAR(500) NULL,
    content TEXT NOT NULL,
    url_hash CHAR(64) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    http_status INTEGER NULL,
    error_message VARCHAR(1000) NULL,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_crawl_document_target FOREIGN KEY (target_id) REFERENCES crawl_target(id) ON DELETE CASCADE,
    CONSTRAINT ck_crawl_document_status CHECK (status IN ('SUCCESS', 'FAILED', 'DUPLICATE'))
);

CREATE UNIQUE INDEX ux_crawl_document_target_content_hash
    ON crawl_document (target_id, content_hash)
    WHERE status = 'SUCCESS';

CREATE INDEX ix_crawl_document_target_fetched ON crawl_document (target_id, fetched_at DESC);

CREATE TABLE crawl_run_log (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT NOT NULL,
    document_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    http_status INTEGER NULL,
    message VARCHAR(1000) NULL,
    duration_ms INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_crawl_run_target FOREIGN KEY (target_id) REFERENCES crawl_target(id) ON DELETE CASCADE,
    CONSTRAINT fk_crawl_run_document FOREIGN KEY (document_id) REFERENCES crawl_document(id) ON DELETE SET NULL,
    CONSTRAINT ck_crawl_run_status CHECK (status IN ('SUCCESS', 'FAILED', 'DUPLICATE'))
);

CREATE INDEX ix_crawl_run_target_created ON crawl_run_log (target_id, created_at DESC);
