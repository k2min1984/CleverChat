CREATE INDEX ix_audit_log_actor_created
    ON audit_log (actor, created_at DESC);

CREATE INDEX ix_audit_log_action_created
    ON audit_log (action, created_at DESC);

CREATE INDEX ix_audit_log_target_type_created
    ON audit_log (target_type, created_at DESC);

CREATE INDEX ix_search_log_source_created
    ON search_log (source, created_at DESC);

CREATE INDEX ix_search_block_log_source_created
    ON search_block_log (source, created_at DESC);

CREATE INDEX ix_crawl_run_status_created
    ON crawl_run_log (status, created_at DESC);

CREATE INDEX ix_crawl_document_status_created
    ON crawl_document (status, created_at DESC);

CREATE INDEX ix_chat_session_created
    ON chat_session (created_at, id);

CREATE INDEX ix_chat_message_created
    ON chat_message (created_at DESC);
