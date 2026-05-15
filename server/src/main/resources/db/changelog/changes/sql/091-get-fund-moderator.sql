DROP FUNCTION IF EXISTS get_active_fund_moderators(INT);

CREATE OR REPLACE FUNCTION get_active_fund_moderators(
    p_fund_id INT
)
RETURNS TABLE (
    "userId" INT,
    "firstName" TEXT,
    "lastName" TEXT,
    "fullName" TEXT,
    "emailId" TEXT,
    "phoneNumber" TEXT,
    "role" TEXT,
    "status" TEXT,
    "isEmailVerified" BOOLEAN
)
LANGUAGE sql
AS $$
    SELECT
        u."userId",
        u."firstName",
        u."lastName",

        TRIM(
            COALESCE(u."firstName", '') || ' ' ||
            COALESCE(u."lastName", '')
        ) AS "fullName",

        u."emailId",
        u."phoneNumber",

        fm."role",
        fm."status",

        u."email_verified" AS isEmailVerified

    FROM fund_members fm

    JOIN users u
        ON u."userId" = fm."userId"

    WHERE
        fm."fundId" = p_fund_id
        AND fm."status" = 'ACTIVE'
    AND fm."role" IN (
        'PRIMARY_MODERATOR',
        'MODERATOR'
    )

    ORDER BY
        CASE
            WHEN fm."role" = 'PRIMARY_MODERATOR' THEN 1
            ELSE 2
        END,
        fm."joiningDate";
$$;