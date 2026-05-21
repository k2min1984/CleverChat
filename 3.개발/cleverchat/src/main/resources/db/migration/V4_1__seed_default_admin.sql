-- Default admin account seed
-- 작성일: 2026-05-19
--
-- 요청 기준: 환경변수 시딩 없이 DB 마이그레이션으로 기본 관리자 계정을 생성한다.
-- 계정: admin / admin
-- V2의 환경변수 시딩 주석은 이 마이그레이션으로 대체한다.
-- spring.flyway.placeholders.default-admin-seed-enabled=true 프로파일에서만 계정을 생성한다.

WITH seed_enabled AS (
    SELECT '${default-admin-seed-enabled}' = 'true' AS enabled
),
upsert_admin AS (
    INSERT INTO users (
        username,
        password_hash,
        display_name,
        enabled,
        failed_attempts,
        locked_until,
        must_change_password
    )
    SELECT
        'admin',
        '$2a$10$FE8fC2xcY82n3tEJ9cpELOIoQdhCwB1NyQ/t34ILEFZNDRwtbb9s.',
        '시스템 관리자',
        TRUE,
        0,
        NULL,
        FALSE
    FROM seed_enabled
    WHERE enabled
    ON CONFLICT (username) DO UPDATE
    SET password_hash = EXCLUDED.password_hash,
        display_name = EXCLUDED.display_name,
        enabled = TRUE,
        failed_attempts = 0,
        locked_until = NULL,
        must_change_password = FALSE,
        updated_at = NOW()
    RETURNING id
)
INSERT INTO user_roles (user_id, role_id)
SELECT upsert_admin.id, roles.id
FROM upsert_admin
JOIN roles ON roles.code = 'ADMIN'
ON CONFLICT DO NOTHING;
