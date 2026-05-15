DROP FUNCTION IF EXISTS add_group_member(INT, INT, TEXT,TEXT, INT);
DROP FUNCTION IF EXISTS activate_group_memberships(INT, INT);

CREATE OR REPLACE FUNCTION add_group_member(
    p_user_id INT,
    p_group_id INT,
    p_joining_date TEXT,
    p_role TEXT,
    p_actor_user_id INT,
    p_status TEXT DEFAULT 'ACTIVE'
)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id INT;
BEGIN

    INSERT INTO group_members(
        "userId",
        "groupId",
        "joiningDate",
        "role",
        "status"
    )
    VALUES (
        p_user_id,
        p_group_id,
        p_joining_date,
        p_role::memberRole,
        p_status::memberStatus
    )

    ON CONFLICT ("userId","groupId")

    DO UPDATE SET

        -- Reactivate/update status
        "status" = p_status::memberStatus,

        -- Preserve primary moderator
        "role" = CASE
            WHEN group_members."role" = 'PRIMARY_MODERATOR'
                THEN group_members."role"
            ELSE EXCLUDED."role"
        END,

        "joiningDate" = EXCLUDED."joiningDate",

        "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,

        "updatedBy" = p_actor_user_id

    RETURNING "groupMemberId"
    INTO v_id;

    RETURN v_id;

END;
$$;

CREATE OR REPLACE FUNCTION activate_group_memberships(
    p_group_id INT,
    p_actor_user_id INT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_updated_count INTEGER;
BEGIN

    UPDATE group_members
    SET
        "status" = 'ACTIVE'::memberStatus,

        "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,

        "updatedBy" = p_actor_user_id

    WHERE
        "groupId" = p_group_id

        AND "role" IN (
            'PRIMARY_MODERATOR',
            'MODERATOR'
        )

        AND "status" <> 'ACTIVE';

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    RETURN v_updated_count;

END;
$$;


CREATE OR REPLACE FUNCTION get_pending_approvals(
    p_user_id INT
)
RETURNS TABLE(
    "approvalId" INT,
    "entityType" TEXT,
    "entityId" INT,
    "title" TEXT,
    "subtitle" TEXT,
    "description" TEXT,
    "requesterName" TEXT,
    "createdAt" TIMESTAMPTZ,
    "requesterUserId" INT,
    "requesterEmail" TEXT
)
LANGUAGE sql
AS $$
SELECT
    a."approvalId",
    a."entityType",
    a."entityId",

    /* ---------- TITLE ---------- */
    CASE
        WHEN a."entityType"='LOAN'
            THEN 'Loan Approval Required'
        WHEN a."entityType"='GROUP'
            THEN 'Group Approval Required'
        ELSE 'Approval Required'
    END AS "title",

    /* ---------- SUBTITLE ---------- */
    CASE
        WHEN a."entityType"='LOAN'
            THEN 'Loan requested in fund: ' || f."fundName"
        WHEN a."entityType"='GROUP'
            THEN 'New group awaiting activation'
        ELSE ''
    END AS "subtitle",

    /* ---------- DESCRIPTION ---------- */
    CASE
        WHEN a."entityType"='LOAN'
            THEN 'Borrower: ' || bu."firstName" || ' ' || bu."lastName"
        WHEN a."entityType"='GROUP'
            THEN 'Group: ' || g."groupName" || ' '|| 'Description: ' ||   g."description"
        ELSE NULL
    END AS "description",

    ru."firstName" || ' ' || ru."lastName" AS "requesterName",

    a."requestedAt" AS "createdAt",

    a."requestedBy" AS "requesterUserId",

    ru."emailId" AS "requesterEmail"

FROM approval_requests a

JOIN users ru
    ON ru."userId" = a."requestedBy"

/* -------- LOAN JOIN -------- */
LEFT JOIN loans l
    ON a."entityType"='LOAN'
   AND l."loanId"=a."entityId"

LEFT JOIN funds f
    ON f."fundId"=l."fundId"

LEFT JOIN users bu
    ON bu."userId"=l."borrowerId"

/* -------- GROUP JOIN -------- */
LEFT JOIN groups g
    ON a."entityType"='GROUP'
   AND g."groupId"=a."entityId"

WHERE
    a."approvalStatus"='PENDING'

    AND (

        /* -------- LOAN APPROVALS -------- */
        (
            a."entityType"='LOAN'

            AND EXISTS (
                SELECT 1
                FROM fund_members fm
                WHERE fm."fundId" = f."fundId"
                  AND fm."userId" = p_user_id
                  AND fm."status"='ACTIVE'
                  AND (
                        fm."role"='PRIMARY_MODERATOR'
                        OR fm."role"='MODERATOR'
                  )
            )
        )

        OR

        /* -------- ADMIN OVERRIDE -------- */
        EXISTS (
            SELECT 1
            FROM user_roles ur
            JOIN roles r
                ON r."roleId" = ur."roleId"
            WHERE ur."userId" = p_user_id
              AND ur."status"='ACTIVE'
              AND r."roleCode"='ADMIN'
        )
    )

ORDER BY a."requestedAt" DESC;
$$;