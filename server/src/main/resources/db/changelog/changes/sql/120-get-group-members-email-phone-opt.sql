DROP FUNCTION IF EXISTS get_group_members_with_names(INT, BOOLEAN);
DROP FUNCTION IF EXISTS get_fund_members_with_names_for_fund(INT, TEXT);
DROP FUNCTION IF EXISTS get_pending_moderator_requests();
DROP FUNCTION IF EXISTS get_users_basic_by_ids(INT[]);

CREATE OR REPLACE FUNCTION get_group_members_with_names(
    p_group_id int,
    p_only_active boolean DEFAULT false
)
RETURNS TABLE (
  "groupMemberId" int,
  "groupId" int,
  "userId" int,
  "firstName" text,
  "lastName" text,
  "joiningDate" text,
  "emailId" text,
  "phoneNumber" text,
  "role" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT gm."groupMemberId",
         gm."groupId",
         gm."userId",
         u."firstName",
         u."lastName",
         gm."joiningDate",
         u."emailId",
         u."phoneNumber",
         gm."role"::text,
         gm."status"::text
    FROM group_members gm
    JOIN users u ON u."userId" = gm."userId"
   WHERE gm."groupId" = p_group_id
     AND (
           NOT p_only_active
           OR gm."status" = 'ACTIVE'::"memberStatus"
         )
   ORDER BY
     CASE gm."role"::text
       WHEN 'PRIMARY_MODERATOR' THEN 1
       WHEN 'MODERATOR' THEN 2
       ELSE 3
     END,
     u."firstName",
     u."lastName";
$$;


CREATE OR REPLACE FUNCTION get_fund_members_with_names_for_fund(
    p_fund_id int,
    p_status TEXT DEFAULT 'ACTIVE'
)
RETURNS TABLE (
  "fundMemberId" int,
  "userId"       int,
  "fundId"       int,
  "joiningDate"  text,
  "firstName"    text,
  "lastName"     text,
  "emailId"      text,
  "phoneNumber"  text,
  "role"         text,
  "status"       text
)
LANGUAGE sql STABLE AS $$
  SELECT
    fm."fundMemberId",
    fm."userId",
    fm."fundId",
    fm."joiningDate",
    u."firstName",
    COALESCE(u."lastName", '') AS "lastName",
    u."emailId",
    u."phoneNumber",
    fm."role"::text,
    fm."status"::text
  FROM fund_members fm
  JOIN users u ON u."userId" = fm."userId"
  WHERE fm."fundId" = p_fund_id
    AND (
            p_status = 'ALL'
            OR fm."status" = p_status::"memberStatus"
          )
  ORDER BY
      CASE fm."role"::text
        WHEN 'PRIMARY_MODERATOR' THEN 1
        WHEN 'MODERATOR' THEN 2
        ELSE 3
      END,
      u."firstName",
      u."lastName";
$$;

CREATE FUNCTION get_pending_moderator_requests()
RETURNS TABLE (
  "userId"             int,
  "groupId"            int,
  "groupName"          text,
  "moderatorName"      text,
  "emailId"            text,
  "phoneNumber"        text,
  "groupDescription"   text
)
LANGUAGE sql
AS $func$
    SELECT
        u."userId",
        g."groupId",
        g."groupName",
        u."firstName" || ' ' || COALESCE(u."lastName", '') AS "moderatorName",
        u."emailId",
        u."phoneNumber",
        g."description" AS "groupDescription"
    FROM "users" u
    INNER JOIN "user_roles" ur ON u."userId" = ur."userId"
    INNER JOIN "roles" r       ON ur."roleId" = r."roleId"
    INNER JOIN "groups" g      ON g."moderator" = u."userId"
    WHERE r."roleCode" = 'MODERATOR'
      AND ur."status" = 'PENDING'
      AND g."status"  = 'PENDING';
$func$;

CREATE OR REPLACE FUNCTION get_users_basic_by_ids(p_user_ids INT[])
RETURNS TABLE (
    userId INT,
    firstName TEXT,
    lastName TEXT,
    emailId TEXT,
    phoneNumber TEXT
)
AS $$
BEGIN
    RETURN QUERY
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId",
        u."phoneNumber"
    FROM users u
    WHERE u."userId" = ANY(p_user_ids);
END;
$$ LANGUAGE plpgsql;