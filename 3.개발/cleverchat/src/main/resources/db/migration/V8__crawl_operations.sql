ALTER TABLE crawl_run_log
    ADD COLUMN reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reviewed_by BIGINT NULL,
    ADD COLUMN reviewed_at TIMESTAMPTZ NULL,
    ADD COLUMN review_comment VARCHAR(1000) NULL,
    ADD CONSTRAINT fk_crawl_run_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX ix_crawl_run_failed_review
    ON crawl_run_log (reviewed, created_at DESC)
    WHERE status = 'FAILED';
