ALTER TABLE tb_chat_session_event
    DROP CONSTRAINT ck_chat_session_event_type;

ALTER TABLE tb_chat_session_event
    ADD CONSTRAINT ck_chat_session_event_type
        CHECK (event_type IN ('SCENARIO_SWITCH', 'NODE_BACK', 'SEARCH_BACK'));
