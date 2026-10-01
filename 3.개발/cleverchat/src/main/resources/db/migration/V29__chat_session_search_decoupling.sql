ALTER TABLE tb_chat_session ALTER COLUMN scenario_no DROP NOT NULL;
ALTER TABLE tb_chat_session ALTER COLUMN version_no DROP NOT NULL;

ALTER TABLE tb_chat_session
    ADD COLUMN session_type varchar(20) NOT NULL DEFAULT 'SCENARIO';

ALTER TABLE tb_chat_session
    ADD CONSTRAINT ck_chat_session_type
        CHECK (session_type IN ('SCENARIO', 'SEARCH'));
