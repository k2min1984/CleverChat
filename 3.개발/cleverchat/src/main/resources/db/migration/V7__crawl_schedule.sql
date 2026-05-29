ALTER TABLE crawl_target
    ADD COLUMN schedule_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN schedule_interval_minutes INTEGER NOT NULL DEFAULT 1440,
    ADD COLUMN next_run_at TIMESTAMPTZ NULL,
    ADD COLUMN robots_allowed BOOLEAN NULL,
    ADD COLUMN robots_checked_at TIMESTAMPTZ NULL;

ALTER TABLE crawl_target
    ADD CONSTRAINT ck_crawl_target_schedule_interval
        CHECK (schedule_interval_minutes BETWEEN 5 AND 10080);

CREATE INDEX ix_crawl_target_schedule_due
    ON crawl_target (next_run_at)
    WHERE enabled = TRUE AND schedule_enabled = TRUE;
