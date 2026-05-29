ALTER TABLE notification_channel
    ADD COLUMN previous_endpoint_env_key VARCHAR(100);

ALTER TABLE notification_channel
    DROP CONSTRAINT IF EXISTS ck_notification_channel_type;

ALTER TABLE notification_channel
    ADD CONSTRAINT ck_notification_channel_type CHECK (type IN ('WEBHOOK', 'SLACK_WEBHOOK'));
