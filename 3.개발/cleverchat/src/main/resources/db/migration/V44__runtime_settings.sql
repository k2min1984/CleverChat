ALTER TABLE tb_system_settings ADD COLUMN runtime_settings jsonb NOT NULL DEFAULT '{}'::jsonb;

-- Only application-created files registered here are eligible for JSON retention cleanup.
CREATE TABLE tb_crawl_export_file (
    export_file_no bigserial PRIMARY KEY,
    target_no bigint NOT NULL,
    root_path text NOT NULL,
    relative_path text NOT NULL,
    content_sha256 varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    cleaned_at timestamptz,
    cleanup_error varchar(500)
);
CREATE INDEX ix_crawl_export_pending_cleanup ON tb_crawl_export_file(created_at) WHERE cleaned_at IS NULL;

CREATE TABLE tb_maintenance_run (
    task_key varchar(40) NOT NULL,
    run_date date NOT NULL,
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    message varchar(1000),
    PRIMARY KEY(task_key,run_date)
);
