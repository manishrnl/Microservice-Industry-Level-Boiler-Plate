CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE payments
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id           UUID           NOT NULL,
    payment_id        UUID           NOT NULL UNIQUE,
    provider          VARCHAR(50)    NOT NULL,
    status            VARCHAR(50)    NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    currency          VARCHAR(10)    NOT NULL,
    stripe_session_id VARCHAR(255),
    checkout_url      TEXT,
    description       TEXT,
    message           TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_user_created_at ON payments (user_id, created_at DESC);
CREATE INDEX idx_payments_stripe_session_id ON payments (stripe_session_id);
