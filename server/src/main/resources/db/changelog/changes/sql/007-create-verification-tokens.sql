-- Generic verification tokens table
-- Supports EMAIL / PHONE verification, password reset, login OTP, etc.

CREATE TABLE IF NOT EXISTS verification_tokens (
    id SERIAL PRIMARY KEY,

    user_id INT NOT NULL
        REFERENCES users("userId")
        ON DELETE CASCADE,

    channel TEXT NOT NULL,      -- EMAIL | PHONE
    purpose TEXT NOT NULL,      -- EMAIL_VERIFY | PASSWORD_RESET | LOGIN_OTP | PHONE_VERIFY

    token TEXT NOT NULL,        -- HASHED token / OTP
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_user
    ON verification_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_token
    ON verification_tokens(token);

CREATE INDEX IF NOT EXISTS idx_verification_tokens_channel_purpose
    ON verification_tokens(channel, purpose);
