
DROP FUNCTION IF EXISTS get_fund_members_with_names_for_fund(INT);

DROP FUNCTION IF EXISTS get_group_members_with_names(INT, BOOLEAN);
DROP FUNCTION IF EXISTS get_members_with_names_for_fund(INT);
DROP FUNCTION IF EXISTS get_user_with_group(INT);

CREATE OR REPLACE FUNCTION get_fund_members_with_names_for_fund(p_fund_id int, p_status TEXT DEFAULT 'ACTIVE')
RETURNS TABLE (
  "fundMemberId" int,
  "userId"       int,
  "fundId"       int,
  "joiningDate"  text,
  "firstName"    text,
  "lastName"     text,
  "emailId"      text,
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


-- Members with names (DTO)


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

CREATE OR REPLACE FUNCTION get_members_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "groupMemberId" int,
  "groupId" int,
  "userId" int,
  "userName" text,
  "joiningDate" text,
  "role" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT gm."groupMemberId",
         gm."groupId",
         gm."userId",
         CASE
           WHEN u."lastName" IS NULL OR u."lastName" = ''
           THEN u."firstName"
           ELSE u."firstName" || ' ' || u."lastName"
         END AS "userName",
         gm."joiningDate",
         gm."role"::text,
         gm."status"::text
    FROM "funds" f
    JOIN group_members gm ON gm."groupId" = f."groupId"
    JOIN users u ON u."userId" = gm."userId"
   WHERE f."fundId" = p_fund_id
     AND gm."status" = 'ACTIVE'::"memberStatus"
  ORDER BY
    CASE gm."role"::text
      WHEN 'PRIMARY_MODERATOR' THEN 1
      WHEN 'MODERATOR' THEN 2
      ELSE 3
    END,
    u."firstName",
    u."lastName";
$$;

CREATE OR REPLACE FUNCTION get_user_with_group(p_group_id int)
RETURNS TABLE (
  "userId" int, "firstName" text, "lastName" text, "emailId" text,
  "phoneNumber" text, "status" text, "userCode" text, "isPinSet" boolean,
  "groupId" int, "groupName" text, "moderator" int,
  "description" text, "groupStatus" text
) LANGUAGE sql AS $$
  SELECT u."userId", u."firstName", u."lastName", u."emailId",
         u."phoneNumber", u."status", u."userCode", u."isPinSet",
         g."groupId", g."groupName", g."moderator", g."description", g."status"
    FROM users u
    LEFT JOIN group_members gm ON u."userId" = gm."userId"
    LEFT JOIN "groups" g       ON gm."groupId" = g."groupId"
   WHERE gm."groupId" = p_group_id
   ORDER BY
        CASE gm."role"::text
            WHEN 'PRIMARY_MODERATOR' THEN 1
            WHEN 'MODERATOR' THEN 2
            ELSE 3
        END,
        u."firstName",
        u."lastName";
$$;

