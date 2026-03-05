CREATE OR REPLACE FUNCTION create_verification_token(
    p_user_id int,
    p_channel TEXT,
    p_purpose TEXT,
    p_token TEXT,
    p_expires_at TIMESTAMP
)
RETURNS VOID AS $$
BEGIN
    -- Invalidate existing tokens (expired or not)
    UPDATE verification_tokens
       SET used = TRUE
     WHERE user_id = p_user_id
       AND channel = p_channel
       AND purpose = p_purpose
       AND used = FALSE;

    INSERT INTO verification_tokens (
        user_id, channel, purpose, token, expires_at
    )
    VALUES (
        p_user_id, p_channel, p_purpose, p_token, p_expires_at
    );
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION verify_verification_token(
    p_token TEXT,
    p_channel TEXT,
    p_purpose TEXT
)
RETURNS INT AS $$
DECLARE
    v_user_id INT;
BEGIN
    SELECT user_id
      INTO v_user_id
      FROM verification_tokens
     WHERE token = p_token
       AND channel = p_channel
       AND purpose = p_purpose
       AND used = FALSE
       AND expires_at > NOW()
     LIMIT 1;

    IF v_user_id IS NULL THEN
        RETURN NULL;
    END IF;

    -- 🔥 Invalidate ONLY this OTP
    UPDATE verification_tokens
       SET used = TRUE
     WHERE token = p_token
       AND channel = p_channel
       AND purpose = p_purpose
       AND used = FALSE;

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION mark_user_email_verified(p_user_id INT)
RETURNS VOID AS $$
BEGIN
    UPDATE users
       SET email_verified = TRUE,
           email_verified_at = NOW()
     WHERE "userId" = p_user_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION is_email_verified(p_user_id int)
RETURNS BOOLEAN AS $$
    SELECT email_verified
    FROM users
    WHERE "userId" = p_user_id;
$$ LANGUAGE sql;

CREATE INDEX IF NOT EXISTS idx_verification_active_token
ON verification_tokens (token, channel, purpose)
WHERE used = FALSE;

CREATE INDEX IF NOT EXISTS idx_verification_active_user
ON verification_tokens (user_id, channel, purpose)
WHERE used = FALSE;

DROP INDEX IF EXISTS idx_verification_tokens_token;
DROP INDEX IF EXISTS idx_verification_tokens_channel_purpose;