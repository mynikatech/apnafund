-- One group for a user (latest joining)
CREATE OR REPLACE FUNCTION get_groups_for_moderator(p_user_id int)
RETURNS TABLE (
  "groupId" int, "groupName" text, "moderator" int, "createdDate" text,
  "description" text, "groupCode" text, "status" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."moderator", g."createdDate",
         g."description", g."groupCode", g."status"
    FROM group_members gm
    JOIN "groups" g ON g."groupId" = gm."groupId"
   WHERE gm."userId" = p_user_id and g."moderator" = p_user_id
   ORDER BY gm."joiningDate" DESC
$$;


CREATE OR REPLACE FUNCTION get_groups_for_user(p_user_id int)
RETURNS TABLE (
  "groupId" int, "groupName" text, "moderator" int, "createdDate" text,
  "description" text, "groupCode" text, "status" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."moderator", g."createdDate",
         g."description", g."groupCode", g."status"
    FROM group_members gm
    JOIN "groups" g ON g."groupId" = gm."groupId"
   WHERE gm."userId" = p_user_id
   ORDER BY gm."joiningDate" DESC
$$;