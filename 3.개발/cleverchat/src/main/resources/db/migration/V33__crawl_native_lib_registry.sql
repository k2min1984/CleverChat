CREATE TABLE tb_crawl_native_lib_registry (
    native_lib_registry_no BIGSERIAL PRIMARY KEY,
    bundle_version varchar(100) NOT NULL,
    original_file_name varchar(255) NOT NULL,
    staging_path varchar(1000) NOT NULL,
    active_path varchar(1000),
    checksum_sha256 varchar(64) NOT NULL,
    signature varchar(1000),
    allowed_sonames text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'VERIFIED',
    uploaded_by BIGINT,
    uploaded_at timestamptz NOT NULL DEFAULT now(),
    activated_by BIGINT,
    activated_at timestamptz,
    message varchar(1000),
    CONSTRAINT fk_crawl_native_lib_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES tb_user(user_no) ON DELETE SET NULL,
    CONSTRAINT fk_crawl_native_lib_activated_by
        FOREIGN KEY (activated_by) REFERENCES tb_user(user_no) ON DELETE SET NULL,
    CONSTRAINT ck_crawl_native_lib_status
        CHECK (status IN ('VERIFIED', 'ACTIVE', 'REJECTED'))
);

CREATE INDEX ix_crawl_native_lib_registry_status_time
    ON tb_crawl_native_lib_registry (status, uploaded_at DESC, native_lib_registry_no DESC);
