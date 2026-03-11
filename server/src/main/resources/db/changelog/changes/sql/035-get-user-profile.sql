DROP FUNCTION get_user_profile(integer);

CREATE OR REPLACE FUNCTION get_user_profile(p_userId INTEGER)
RETURNS TABLE (
    "userId"        INTEGER,
    "userName"      TEXT,
    "isPinSet"      BOOLEAN,
    "firstName"     TEXT,
    "lastName"      TEXT,
    "emailId"       TEXT,
    "phoneNumber"   TEXT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
  RETURN QUERY
  SELECT
      u."userId",
      CASE
          WHEN u."lastName" IS NULL OR u."lastName" = ''
          THEN u."firstName"
          ELSE u."firstName" || ' ' || u."lastName"
      END AS "userName",
      u."isPinSet",
      u."firstName",
      u."lastName",
      u."emailId",
      u."phoneNumber"
  FROM users u
  WHERE u."userId" = p_userId;
END;
$$;