-- CMS-style admin management baseline.
-- Keeps the existing users/roles/user_roles model and adds common codes,
-- menu metadata, and role-menu visibility mapping for the admin CMS.

CREATE TABLE IF NOT EXISTS admin_code (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES admin_code(id) ON DELETE CASCADE,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    value       VARCHAR(200),
    description VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  BIGINT,
    updated_by  BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_admin_code_parent_sort ON admin_code(parent_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_admin_code_enabled ON admin_code(enabled);

CREATE TABLE IF NOT EXISTS admin_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES admin_menu(id) ON DELETE CASCADE,
    menu_key    VARCHAR(100) NOT NULL UNIQUE,
    title       VARCHAR(100) NOT NULL,
    url         VARCHAR(300),
    sort_order  INT          NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  BIGINT,
    updated_by  BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_admin_menu_parent_sort ON admin_menu(parent_id, sort_order, id);
CREATE INDEX IF NOT EXISTS idx_admin_menu_url ON admin_menu(url);
CREATE INDEX IF NOT EXISTS idx_admin_menu_enabled_visible ON admin_menu(enabled, visible);

CREATE TABLE IF NOT EXISTS admin_role_menu (
    role_id    BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    menu_id    BIGINT NOT NULL REFERENCES admin_menu(id) ON DELETE CASCADE,
    can_read   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, menu_id)
);
CREATE INDEX IF NOT EXISTS idx_admin_role_menu_menu_id ON admin_role_menu(menu_id);

INSERT INTO admin_code (code, name, value, description, sort_order, enabled)
VALUES
    ('NOTICE_STATUS', 'Notice status', 'GROUP', 'Notice exposure status codes', 10, TRUE),
    ('NOTIFICATION_CHANNEL_TYPE', 'Notification channel type', 'GROUP', 'Notification channel type codes', 20, TRUE),
    ('CRAWL_STATUS', 'Crawl status', 'GROUP', 'Crawl run status codes', 30, TRUE)
ON CONFLICT (code) DO NOTHING;

WITH parents AS (
    SELECT id, code
    FROM admin_code
    WHERE code IN ('NOTICE_STATUS', 'NOTIFICATION_CHANNEL_TYPE', 'CRAWL_STATUS')
)
INSERT INTO admin_code (parent_id, code, name, value, sort_order, enabled)
SELECT p.id, v.code, v.name, v.value, v.sort_order, TRUE
FROM parents p
JOIN (
    VALUES
        ('NOTICE_STATUS', 'NOTICE_ENABLED', 'Enabled', 'ENABLED', 10),
        ('NOTICE_STATUS', 'NOTICE_DISABLED', 'Disabled', 'DISABLED', 20),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_WEBHOOK', 'Generic webhook', 'WEBHOOK', 10),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_SLACK_WEBHOOK', 'Slack webhook', 'SLACK_WEBHOOK', 20),
        ('NOTIFICATION_CHANNEL_TYPE', 'CHANNEL_EMAIL_SMTP', 'Email SMTP', 'EMAIL_SMTP', 30),
        ('CRAWL_STATUS', 'CRAWL_SUCCESS', 'Success', 'SUCCESS', 10),
        ('CRAWL_STATUS', 'CRAWL_FAILED', 'Failed', 'FAILED', 20),
        ('CRAWL_STATUS', 'CRAWL_DUPLICATE', 'Duplicate', 'DUPLICATE', 30)
) AS v(parent_code, code, name, value, sort_order)
  ON v.parent_code = p.code
ON CONFLICT (code) DO NOTHING;

INSERT INTO admin_menu (menu_key, parent_id, title, url, sort_order, enabled, visible)
VALUES
    ('dashboard', NULL, 'Dashboard', '/admin', 10, TRUE, TRUE),
    ('scenario', NULL, 'Scenario', NULL, 20, TRUE, TRUE),
    ('chat', NULL, 'Chat operations', NULL, 30, TRUE, TRUE),
    ('search_crawl', NULL, 'Search and crawl', NULL, 40, TRUE, TRUE),
    ('ops', NULL, 'Operations', NULL, 50, TRUE, TRUE),
    ('cms_manage', NULL, 'CMS management', NULL, 60, TRUE, TRUE)
ON CONFLICT (menu_key) DO NOTHING;

WITH parent AS (SELECT id, menu_key FROM admin_menu)
INSERT INTO admin_menu (menu_key, parent_id, title, url, sort_order, enabled, visible)
SELECT v.menu_key, p.id, v.title, v.url, v.sort_order, TRUE, TRUE
FROM (
    VALUES
        ('scenario.list', 'scenario', 'Scenario list', '/admin/scenarios', 10),
        ('chat.sessions', 'chat', 'Chat sessions', '/admin/chat/sessions', 10),
        ('chat.failures', 'chat', 'Failure queue', '/admin/chat/failures', 20),
        ('chat.feedback', 'chat', 'Feedback', '/admin/chat/feedback', 30),
        ('chat.recommendations', 'chat', 'Recommended questions', '/admin/chat/recommendations', 40),
        ('search.logs', 'search_crawl', 'Search logs', '/admin/search/logs', 10),
        ('search.blocks', 'search_crawl', 'PII block logs', '/admin/search/blocks', 20),
        ('search.popular', 'search_crawl', 'Popular queries', '/admin/search/popular', 30),
        ('crawl.targets', 'search_crawl', 'Crawl targets', '/admin/crawl-targets', 40),
        ('crawl.documents', 'search_crawl', 'Crawl documents', '/admin/crawl-documents', 50),
        ('crawl.runs', 'search_crawl', 'Crawl runs', '/admin/crawl-runs', 60),
        ('ops.statistics', 'ops', 'Statistics', '/admin/statistics', 10),
        ('ops.audit', 'ops', 'Audit logs', '/admin/audit-logs', 20),
        ('ops.notices', 'ops', 'Notices', '/admin/notices', 30),
        ('ops.notifications', 'ops', 'Notifications', '/admin/notifications', 40),
        ('manage.codes', 'cms_manage', 'Code management', '/admin/manage/codes', 10),
        ('manage.menus', 'cms_manage', 'Menu management', '/admin/manage/menus', 20),
        ('manage.permissions', 'cms_manage', 'Permission management', '/admin/manage/permissions', 30)
) AS v(menu_key, parent_key, title, url, sort_order)
JOIN parent p ON p.menu_key = v.parent_key
ON CONFLICT (menu_key) DO NOTHING;

INSERT INTO admin_role_menu (role_id, menu_id, can_read)
SELECT r.id, m.id, TRUE
FROM roles r
CROSS JOIN admin_menu m
WHERE r.code = 'ADMIN'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO admin_role_menu (role_id, menu_id, can_read)
SELECT r.id, m.id, TRUE
FROM roles r
JOIN admin_menu m ON m.menu_key NOT LIKE 'manage.%'
WHERE r.code = 'OPERATOR'
ON CONFLICT (role_id, menu_id) DO NOTHING;
