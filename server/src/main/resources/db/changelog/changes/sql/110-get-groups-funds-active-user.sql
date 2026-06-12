DROP FUNCTION IF EXISTS has_active_groups(INT);
DROP FUNCTION IF EXISTS has_active_funds(INT);

CREATE OR REPLACE FUNCTION has_active_groups(
    p_user_id INT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS
$$
SELECT EXISTS (
    SELECT 1
    FROM group_members gm
    INNER JOIN groups g
        ON g."groupId" = gm."groupId"
    WHERE gm."userId" = p_user_id
      AND gm."status" = 'ACTIVE'::"memberStatus"
      AND g."status" = 'ACTIVE'
);
$$;



CREATE OR REPLACE FUNCTION has_active_funds(
    p_user_id INT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS
$$
SELECT EXISTS (
    SELECT 1
    FROM fund_members fm
    INNER JOIN funds f
        ON f."fundId" = fm."fundId"
    WHERE fm."userId" = p_user_id
      AND fm."status" = 'ACTIVE'::"memberStatus"
      AND f."fundStatus" = 'ACTIVE'
);
$$;