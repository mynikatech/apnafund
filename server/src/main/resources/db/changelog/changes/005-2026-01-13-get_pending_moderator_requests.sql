DROP FUNCTION IF EXISTS get_pending_moderator_requests();

CREATE FUNCTION get_pending_moderator_requests()
RETURNS TABLE (
  "userId"             int,
  "groupId"            int,
  "groupName"          text,
  "moderatorName"      text,
  "emailId"            text,
  "groupDescription"   text
)
LANGUAGE sql
AS $func$
    SELECT
        u."userId",
        g."groupId",
        g."groupName",
        u."firstName" || ' ' || u."lastName" AS "moderatorName",
        u."emailId",
        g."description" AS "groupDescription"
    FROM "users" u
    INNER JOIN "user_roles" ur ON u."userId" = ur."userId"
    INNER JOIN "roles" r       ON ur."roleId" = r."roleId"
    INNER JOIN "groups" g      ON g."moderator" = u."userId"
    WHERE r."roleCode" = 'MODERATOR'
      AND ur."status" = 'PENDING'
      AND g."status"  = 'PENDING';
$func$;
