ALTER TABLE crawl_run_log
    ADD COLUMN failure_code VARCHAR(40) NULL;

CREATE INDEX ix_crawl_run_failure_code_created
    ON crawl_run_log (failure_code, created_at DESC)
    WHERE failure_code IS NOT NULL;
