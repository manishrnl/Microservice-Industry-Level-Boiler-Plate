CREATE TABLE IF NOT EXISTS file_metadata
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(128) NOT NULL,
    size_bytes    BIGINT       NOT NULL DEFAULT 0,
    is_public     BOOLEAN      NOT NULL DEFAULT FALSE,
    content       TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_file_metadata_user_created
    ON file_metadata (user_id, created_at DESC);
