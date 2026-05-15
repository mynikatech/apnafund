DROP FUNCTION IF EXISTS get_groups_for_moderator_with_moderator_info(INT);

CREATE OR REPLACE FUNCTION get_groups_for_moderator_with_moderator_info(
    p_user_id int
)
RETURNS TABLE (
    "groupId" int,
    "groupName" text,
    "moderator" int,
    "moderatorName" text,
    "moderatorEmail" text,
    "createdDate" text,
    "description" text,
    "groupCode" text,
    "status" text
)
LANGUAGE sql AS $$

SELECT
    g."groupId",
    g."groupName",
    g."moderator",

    TRIM(
        COALESCE(u."firstName", '') || ' ' ||
        COALESCE(u."lastName", '')
    ) AS "moderatorName",

    u."emailId" AS "moderatorEmail",

    g."createdDate",
    g."description",
    g."groupCode",
    g."status"

FROM group_members gm

JOIN "groups" g
    ON g."groupId" = gm."groupId"

LEFT JOIN users u
    ON u."userId" = g."moderator"

WHERE
    gm."userId" = p_user_id
    AND gm."status" = 'ACTIVE'
    AND gm."role" IN (
        'PRIMARY_MODERATOR',
        'MODERATOR'
    )

ORDER BY gm."joiningDate" DESC;

$$;