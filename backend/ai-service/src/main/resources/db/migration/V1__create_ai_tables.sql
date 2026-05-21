CREATE TABLE chat_sessions
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    title           VARCHAR(255),
    model_used      VARCHAR(100),
    system_prompt   TEXT,
    context_summary TEXT,
    total_tokens    INT              DEFAULT 0,
    created_at      TIMESTAMP        DEFAULT NOW(),
    updated_at      TIMESTAMP        DEFAULT NOW(),
    archived        BOOLEAN          DEFAULT FALSE
);

CREATE TABLE chat_messages
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID        NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT        NOT NULL,
    tokens_used INT,
    model       VARCHAR(100),
    created_at  TIMESTAMP        DEFAULT NOW()
);

CREATE TABLE ai_usage_stats
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    session_id    UUID,
    model         VARCHAR(100),
    input_tokens  INT,
    output_tokens INT,
    total_tokens  INT,
    latency_ms    INT,
    created_at    TIMESTAMP        DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session ON chat_messages (session_id);
CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id);
