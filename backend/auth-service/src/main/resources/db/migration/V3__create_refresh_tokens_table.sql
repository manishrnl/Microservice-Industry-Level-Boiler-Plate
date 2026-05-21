CREATE TABLE refresh_tokens
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash     VARCHAR(255) NOT NULL UNIQUE,
    device_id      VARCHAR(255),
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    issued_at      TIMESTAMP    NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    revoked        BOOLEAN          DEFAULT FALSE,
    revoked_at     TIMESTAMP,
    revoked_reason VARCHAR(100)
);
