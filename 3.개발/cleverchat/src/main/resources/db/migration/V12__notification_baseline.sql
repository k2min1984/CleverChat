CREATE TABLE notification_channel (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'WEBHOOK',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    endpoint_env_key VARCHAR(100) NOT NULL,
    rate_limit_per_hour INTEGER NOT NULL DEFAULT 60,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_channel_type CHECK (type IN ('WEBHOOK')),
    CONSTRAINT ck_notification_channel_rate CHECK (rate_limit_per_hour BETWEEN 1 AND 1000),
    CONSTRAINT fk_notification_channel_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_channel_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_notification_channel_enabled
    ON notification_channel (enabled, id DESC);

CREATE TABLE notification_event (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    source_type VARCHAR(80) NOT NULL,
    source_id VARCHAR(120) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARN',
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    next_retry_at TIMESTAMPTZ NULL,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notification_event_severity CHECK (severity IN ('INFO', 'WARN', 'ERROR')),
    CONSTRAINT ck_notification_event_status CHECK (status IN ('PENDING', 'SENT', 'RETRY', 'FAILED')),
    CONSTRAINT fk_notification_event_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX ix_notification_event_status_retry
    ON notification_event (status, next_retry_at, id);

CREATE INDEX ix_notification_event_reviewed_created
    ON notification_event (reviewed, created_at DESC, id DESC);

CREATE UNIQUE INDEX ux_notification_event_open_source
    ON notification_event (event_type, source_type, source_id)
    WHERE reviewed = FALSE AND status IN ('PENDING', 'RETRY', 'FAILED');
