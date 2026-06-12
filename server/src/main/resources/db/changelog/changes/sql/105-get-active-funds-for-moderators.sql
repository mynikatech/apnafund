DROP FUNCTION IF EXISTS get_active_funds_for_group_moderator(INT, INT);

CREATE OR REPLACE FUNCTION get_active_funds_for_group_moderator(
    p_group_id INT,
    p_user_id INT
)
RETURNS SETOF "funds"
LANGUAGE sql
AS $$
    SELECT DISTINCT f.*
    FROM "funds" f
    INNER JOIN "fund_members" fm
        ON fm."fundId" = f."fundId"
    WHERE f."groupId" = p_group_id
      AND f."fundStatus" = 'ACTIVE'
      AND fm."userId" = p_user_id
      AND fm."status" = 'ACTIVE'::"memberStatus"
      AND fm."role" IN (
            'MODERATOR'::"memberRole",
            'PRIMARY_MODERATOR'::"memberRole"
      );
$$;