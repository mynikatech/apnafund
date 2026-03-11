CREATE OR REPLACE FUNCTION get_basic_group_for_user(p_user_id int)
RETURNS TABLE (
  "groupId" int, "groupName" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName"
    FROM group_members gm
    JOIN "groups" g ON g."groupId" = gm."groupId"
   WHERE gm."userId" = p_user_id
   AND g."status" = 'ACTIVE'
$$;