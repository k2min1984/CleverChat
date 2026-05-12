CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE chat_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anonymous_id UUID NOT NULL,
    user_id BIGINT NULL,
    scenario_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    current_node_id BIGINT NULL,
    state VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    user_agent_hash VARCHAR(64) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_session_scenario FOREIGN KEY (scenario_id) REFERENCES scenario(id),
    CONSTRAINT fk_chat_session_version FOREIGN KEY (version_id) REFERENCES scenario_version(id),
    CONSTRAINT fk_chat_session_current_node FOREIGN KEY (current_node_id) REFERENCES scenario_node(id),
    CONSTRAINT ck_chat_session_state CHECK (state IN ('ACTIVE', 'COMPLETED', 'ABANDONED', 'EXPIRED'))
);

CREATE INDEX ix_chat_session_anonymous_started
    ON chat_session (anonymous_id, started_at DESC);

CREATE INDEX ix_chat_session_state_expires
    ON chat_session (state, expires_at);

CREATE TABLE chat_message (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    seq INTEGER NOT NULL,
    direction VARCHAR(10) NOT NULL,
    node_id BIGINT NULL,
    option_id BIGINT NULL,
    content TEXT NOT NULL,
    payload JSONB NULL,
    latency_ms INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_node FOREIGN KEY (node_id) REFERENCES scenario_node(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_message_option FOREIGN KEY (option_id) REFERENCES scenario_node_option(id) ON DELETE SET NULL,
    CONSTRAINT ux_chat_message_session_seq UNIQUE (session_id, seq),
    CONSTRAINT ck_chat_message_direction CHECK (direction IN ('USER', 'BOT', 'SYSTEM'))
);

CREATE INDEX ix_chat_message_session_created
    ON chat_message (session_id, created_at, id);

CREATE TABLE chat_feedback (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    rating VARCHAR(10) NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_hash VARCHAR(64) NOT NULL,
    CONSTRAINT fk_chat_feedback_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
    CONSTRAINT ux_chat_feedback_message UNIQUE (message_id),
    CONSTRAINT ck_chat_feedback_rating CHECK (rating IN ('UP', 'DOWN'))
);

CREATE INDEX ix_chat_feedback_rating_created
    ON chat_feedback (rating, created_at DESC);

CREATE TABLE chat_failure (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    message_id BIGINT NULL,
    reason VARCHAR(30) NOT NULL,
    detail JSONB NULL,
    reviewed BOOLEAN NOT NULL DEFAULT false,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMPTZ NULL,
    review_comment TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_failure_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_failure_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_failure_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_failure_reason CHECK (reason IN ('NO_MATCH', 'EXPIRED', 'INVALID_OPTION', 'SYSTEM_ERROR', 'PII_BLOCKED', 'RATE_LIMITED'))
);

CREATE INDEX ix_chat_failure_reviewed_created
    ON chat_failure (reviewed, created_at DESC);

CREATE TABLE chat_recommendation (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    label VARCHAR(200) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_recommendation_scenario FOREIGN KEY (scenario_id) REFERENCES scenario(id) ON DELETE CASCADE
);

CREATE INDEX ix_chat_recommendation_enabled_priority
    ON chat_recommendation (enabled, priority, id);

CREATE INDEX ix_chat_recommendation_scenario
    ON chat_recommendation (scenario_id);
