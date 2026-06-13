DROP FUNCTION IF EXISTS mark_user_email_unverified(INT);

CREATE OR REPLACE FUNCTION mark_user_email_unverified(p_user_id INT)
RETURNS VOID AS $$
BEGIN
    UPDATE users
       SET email_verified = FALSE,
           email_verified_at = NULL
     WHERE "userId" = p_user_id;
END;
$$ LANGUAGE plpgsql;