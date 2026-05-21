CREATE TABLE user_sessions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_id  VARCHAR(255) NOT NULL UNIQUE,
    device_id   VARCHAR(255),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMP        DEFAULT NOW(),
    last_active TIMESTAMP        DEFAULT NOW(),
    expired     BOOLEAN          DEFAULT FALSE
);

CREATE INDEX idx_sessions_user_id ON user_sessions (user_id);
