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
        u."firstName",
        u."lastName";
$$;