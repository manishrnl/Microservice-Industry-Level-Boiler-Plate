CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    username        VARCHAR(100) UNIQUE,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255),
    avatar_url      VARCHAR(500),
    provider        VARCHAR(50)      DEFAULT 'LOCAL',
    provider_id     VARCHAR(255),
    email_verified  BOOLEAN          DEFAULT FALSE,
    account_locked  BOOLEAN          DEFAULT FALSE,
    failed_attempts INT              DEFAULT 0,
    locked_until    TIMESTAMP,
    created_at      TIMESTAMP        DEFAULT NOW(),
    updated_at      TIMESTAMP        DEFAULT NOW()
);
