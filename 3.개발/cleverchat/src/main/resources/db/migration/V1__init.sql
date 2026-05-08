-- CleverChat initial schema
-- 작성일: 2026-05-07

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ----------------------------------------------------------------
-- 사용자 / 권한
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts INT          NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (code, description) VALUES
    ('ADMIN',    '관리자'),
    ('OPERATOR', '운영자'),
    ('USER',     '일반 사용자')
ON CONFLICT (code) DO NOTHING;

-- ----------------------------------------------------------------
-- 감사 / 접근 로그 (M1, M6 공용 베이스)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor       VARCHAR(64),
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(64),
    target_id   VARCHAR(128),
    detail      JSONB,
    ip          VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor      ON audit_log(actor);

CREATE TABLE IF NOT EXISTS login_log (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64) NOT NULL,
    success     BOOLEAN     NOT NULL,
    failure_msg VARCHAR(200),
    ip          VARCHAR(45),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_login_log_created_at ON login_log(created_at DESC);

-- ----------------------------------------------------------------
-- 이후 마이그레이션 자리 (시나리오/챗봇/검색/크롤링은 V2_xx 이후)
-- ----------------------------------------------------------------
