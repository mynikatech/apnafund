CREATE OR REPLACE FUNCTION get_users_basic_by_ids(p_user_ids INT[])
RETURNS TABLE (
    userId INT,
    firstName TEXT,
    lastName TEXT,
    emailId TEXT
)
AS $$
BEGIN
    RETURN QUERY
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId"
    FROM users u
    WHERE u."userId" = ANY(p_user_ids);
END;
$$ LANGUAGE plpgsql;