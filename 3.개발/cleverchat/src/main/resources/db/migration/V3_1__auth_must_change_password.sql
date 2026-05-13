-- M1 auth must-change-password
-- 작성일: 2026-05-13

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;
