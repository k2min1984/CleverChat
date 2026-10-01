CREATE TABLE tb_crawl_job (
    crawl_job_no BIGSERIAL PRIMARY KEY,
    target_no BIGINT NOT NULL,
    trigger_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    requested_by BIGINT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ NULL,
    finished_at TIMESTAMPTZ NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    message varchar(1000) NULL,
    CONSTRAINT fk_crawl_job_target
        FOREIGN KEY (target_no) REFERENCES tb_crawl_target(crawl_target_no) ON DELETE CASCADE,
    CONSTRAINT fk_crawl_job_requested_by
        FOREIGN KEY (requested_by) REFERENCES tb_user(user_no) ON DELETE SET NULL,
    CONSTRAINT ck_crawl_job_trigger_type
        CHECK (trigger_type IN ('MANUAL', 'SCHEDULE')),
    CONSTRAINT ck_crawl_job_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_crawl_job_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX ix_crawl_job_status_requested
    ON tb_crawl_job (status, requested_at, crawl_job_no);

CREATE INDEX ix_crawl_job_target_active
    ON tb_crawl_job (target_no, status)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE TABLE tb_crawl_coverage (
    coverage_no BIGSERIAL PRIMARY KEY,
    target_no BIGINT NOT NULL,
    run_log_no BIGINT NULL,
    list_pages INTEGER NOT NULL DEFAULT 0,
    list_items_found INTEGER NOT NULL DEFAULT 0,
    details_fetched INTEGER NOT NULL DEFAULT 0,
    details_failed INTEGER NOT NULL DEFAULT 0,
    truncated_yn char(1) NOT NULL DEFAULT 'N',
    frst_reg_dt TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_crawl_coverage_target
        FOREIGN KEY (target_no) REFERENCES tb_crawl_target(crawl_target_no) ON DELETE CASCADE,
    CONSTRAINT fk_crawl_coverage_run_log
        FOREIGN KEY (run_log_no) REFERENCES tb_crawl_run_log(crawl_run_log_no) ON DELETE SET NULL,
    CONSTRAINT ck_crawl_coverage_truncated
        CHECK (truncated_yn IN ('Y', 'N'))
);

CREATE INDEX ix_crawl_coverage_target_time
    ON tb_crawl_coverage (target_no, frst_reg_dt DESC, coverage_no DESC);
