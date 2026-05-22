CREATE TABLE IF NOT EXISTS user_preferences
(
    user_id  UUID PRIMARY KEY,
    timezone VARCHAR(100) NOT NULL
);
