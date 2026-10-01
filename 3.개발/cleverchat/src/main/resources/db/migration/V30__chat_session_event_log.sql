CREATE TABLE tb_chat_session_event (
    chat_session_event_no BIGSERIAL PRIMARY KEY,
    session_no UUID NOT NULL,
    event_type varchar(40) NOT NULL,
    from_scenario_no BIGINT NULL,
    to_scenario_no BIGINT NULL,
    trigger_message_no BIGINT NULL,
    detail JSONB NOT NULL DEFAULT '{}'::jsonb,
    frst_reg_dt TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_chat_session_event_session
        FOREIGN KEY (session_no) REFERENCES tb_chat_session(chat_session_no) ON DELETE CASCADE,
    CONSTRAINT fk_chat_session_event_from_scenario
        FOREIGN KEY (from_scenario_no) REFERENCES tb_scenario(scenario_no) ON DELETE SET NULL,
    CONSTRAINT fk_chat_session_event_to_scenario
        FOREIGN KEY (to_scenario_no) REFERENCES tb_scenario(scenario_no) ON DELETE SET NULL,
    CONSTRAINT fk_chat_session_event_trigger_message
        FOREIGN KEY (trigger_message_no) REFERENCES tb_chat_message(chat_message_no) ON DELETE SET NULL,
    CONSTRAINT ck_chat_session_event_type
        CHECK (event_type IN ('SCENARIO_SWITCH'))
);

CREATE INDEX ix_chat_session_event_session_time
    ON tb_chat_session_event (session_no, frst_reg_dt, chat_session_event_no);

CREATE INDEX ix_chat_session_event_type_time
    ON tb_chat_session_event (event_type, frst_reg_dt DESC);
