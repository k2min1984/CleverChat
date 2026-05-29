ALTER TABLE chat_message
    ADD COLUMN content_ciphertext TEXT NULL,
    ADD COLUMN content_key_id VARCHAR(100) NULL,
    ADD COLUMN content_encryption_version INTEGER NULL;

ALTER TABLE chat_feedback
    ADD COLUMN comment_ciphertext TEXT NULL,
    ADD COLUMN comment_key_id VARCHAR(100) NULL,
    ADD COLUMN comment_encryption_version INTEGER NULL;

ALTER TABLE chat_failure
    ADD COLUMN review_comment_ciphertext TEXT NULL,
    ADD COLUMN review_comment_key_id VARCHAR(100) NULL,
    ADD COLUMN review_comment_encryption_version INTEGER NULL;
