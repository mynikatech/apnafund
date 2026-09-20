DROP FUNCTION IF EXISTS get_active_funds_for_group_moderator(integer, integer);

CREATE OR REPLACE FUNCTION get_active_funds_for_group_moderator(p_group_id int, p_user_id int)
RETURNS SETOF "funds" LANGUAGE sql AS $$
  SELECT * FROM "funds"
   WHERE "groupId" = p_group_id  AND "moderator" = p_user_id AND "fundStatus" = 'ACTIVE';
$$;