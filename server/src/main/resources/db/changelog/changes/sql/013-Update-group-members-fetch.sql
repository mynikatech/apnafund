-- Members with names (DTO)

DROP FUNCTION IF EXISTS get_group_members_with_names(INT);

CREATE OR REPLACE FUNCTION get_group_members_with_names(p_group_id int)
RETURNS TABLE (
  "groupMemberId" int, "groupId" int, "userId" int,"firstName" text, "lastName" text,"joiningDate" text, "emailId" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId",
         gm."groupId",
         gm."userId",
         u."firstName",
         u."lastName",
         gm."joiningDate",
         u."emailId"
    FROM group_members gm
    JOIN users u ON u."userId" = gm."userId"
   WHERE gm."groupId" = p_group_id
   ORDER BY gm."groupMemberId";
$$;