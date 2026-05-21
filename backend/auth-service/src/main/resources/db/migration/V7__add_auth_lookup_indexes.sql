CREATE INDEX IF NOT EXISTS idx_users_lower_email ON users (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_sessions_user_active_last_active
    ON user_sessions (user_id, expired, last_active DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sessions_user_active_device
    ON user_sessions (user_id, expired, device_id);

CREATE INDEX IF NOT EXISTS idx_sessions_user_active_ip
    ON user_sessions (user_id, expired, ip_address);
