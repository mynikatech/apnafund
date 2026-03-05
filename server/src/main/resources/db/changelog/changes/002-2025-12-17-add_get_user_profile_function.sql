--liquibase formatted sql

-- changeset apnafund:20251217-get-user-profile-fn splitStatements:false endDelimiter:$$
CREATE OR REPLACE FUNCTION get_user_profile(p_userId INTEGER)
RETURNS TABLE (
    "userId"        INTEGER,
    "userName"      TEXT,
    "roleId"        INTEGER,
    "roleCode"      TEXT,
    "groupId"       INTEGER,
    "groupName"     TEXT,
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
      r."roleId",
      r."roleCode",
      (
        SELECT gm."groupId"
        FROM group_members gm
        WHERE gm."userId" = u."userId"
        LIMIT 1
      ) AS "groupId",
      (
        SELECT g."groupName"
        FROM group_members gm
        JOIN "groups" g ON g."groupId" = gm."groupId"
        WHERE gm."userId" = u."userId"
        LIMIT 1
      ) AS "groupName",
      u."isPinSet",
      u."firstName",
      u."lastName",
      u."emailId",
      u."phoneNumber"
  FROM users u
  JOIN user_roles ur ON u."userId" = ur."userId"
  JOIN roles r       ON r."roleId" = ur."roleId"
  WHERE u."userId" = p_userId;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_user_profile(INTEGER)
TO apnafund_appdev;
