DROP FUNCTION IF EXISTS get_user_group_memberships(INT);
DROP FUNCTION IF EXISTS get_user_fund_memberships(INT);

CREATE OR REPLACE FUNCTION get_user_group_memberships(p_user_id INT)
RETURNS TABLE (
    "groupId" INT,
    "role" TEXT,
    "status" TEXT
)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        gm."groupId",
        gm."role"::text,
        gm."status"::text
    FROM group_members gm
    WHERE gm."userId" = p_user_id;
$$;

CREATE OR REPLACE FUNCTION get_user_fund_memberships(p_user_id INT)
RETURNS TABLE (
    "fundId" INT,
    "role" TEXT,
    "status" TEXT
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        fm."fundId",
        fm."role"::text,
        fm."status"::text
    FROM fund_members fm
    WHERE fm."userId" = p_user_id;
$$;