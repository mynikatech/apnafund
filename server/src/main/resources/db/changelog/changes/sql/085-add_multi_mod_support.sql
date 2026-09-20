-- Members with names (DTO)

DROP FUNCTION IF EXISTS get_group_members_with_names(INT);
DROP FUNCTION IF EXISTS get_members_with_names_for_fund(INT);
DROP FUNCTION IF EXISTS get_group_members_with_names(integer,boolean);

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
   ORDER BY gm."groupMemberId";
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
   ORDER BY gm."groupMemberId";
$$;