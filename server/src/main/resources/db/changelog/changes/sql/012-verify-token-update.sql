DROP FUNCTION IF EXISTS verify_verification_token(TEXT,TEXT,TEXT);

CREATE OR REPLACE FUNCTION verify_verification_token(
    p_token TEXT,
    p_channel TEXT,
    p_purpose TEXT,
    p_userid INT
)
RETURNS INT AS $$
DECLARE
    v_user_id INT;
BEGIN
    SELECT user_id
      INTO v_user_id
      FROM verification_tokens
     WHERE user_id = p_userid
       AND token = p_token
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