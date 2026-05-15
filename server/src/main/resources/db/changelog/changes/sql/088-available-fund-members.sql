DROP FUNCTION IF EXISTS get_available_fund_members(INT, INT);

CREATE OR REPLACE FUNCTION get_available_fund_members(
    p_group_id int,
    p_fund_id int
)
RETURNS TABLE (
    "userId" int,
    "firstName" text,
    "lastName" text,
    "emailId" text,
    "phoneNumber" text
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId",
        u."phoneNumber"

    FROM users u

    -- User must belong to group
    JOIN group_members gm
      ON gm."userId" = u."userId"

    WHERE gm."groupId" = p_group_id

      -- Active user only
      AND u."status" = 'ACTIVE'

      -- Active group membership only
      AND gm."status" = 'ACTIVE'::"memberStatus"

      -- Exclude only ACTIVE fund members
      AND NOT EXISTS (
          SELECT 1
          FROM fund_members fm
          WHERE fm."fundId" = p_fund_id
            AND fm."userId" = u."userId"
            AND fm."status" = 'ACTIVE'::"memberStatus"
      )

    ORDER BY u."firstName", u."lastName";
$$;