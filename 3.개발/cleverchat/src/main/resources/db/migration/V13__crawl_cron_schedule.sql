ALTER TABLE crawl_target
    ADD COLUMN schedule_mode VARCHAR(20) NOT NULL DEFAULT 'INTERVAL',
    ADD COLUMN schedule_cron VARCHAR(120) NULL,
    ADD CONSTRAINT ck_crawl_target_schedule_mode CHECK (schedule_mode IN ('INTERVAL', 'CRON'));

CREATE INDEX ix_crawl_target_schedule_mode_next_run
    ON crawl_target (schedule_mode, next_run_at)
    WHERE enabled = TRUE AND schedule_enabled = TRUE;
