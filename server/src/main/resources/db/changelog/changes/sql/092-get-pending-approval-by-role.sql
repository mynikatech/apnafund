DROP FUNCTION IF EXISTS get_pending_approvals(INT);
DROP FUNCTION IF EXISTS get_active_admin_users();
DROP FUNCTION IF EXISTS approve_approval_request(INT, INT, TEXT);
DROP FUNCTION IF EXISTS reject_approval_request(INT, INT, TEXT);


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

        /* -------- GROUP APPROVALS -------- */
        (
            a."entityType"='GROUP'

            AND EXISTS (
                SELECT 1
                FROM group_members gm
                WHERE gm."groupId" = g."groupId"
                  AND gm."userId" = p_user_id
                  AND gm."status"='ACTIVE'
                  AND (
                        gm."role"='PRIMARY_MODERATOR'
                        OR gm."role"='MODERATOR'
                  )
            )
        )

        OR

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

CREATE OR REPLACE FUNCTION get_active_admin_users()
RETURNS TABLE (
    "userId" INT,
    "firstName" TEXT,
    "lastName" TEXT,
    "emailId" TEXT,
    "phoneNumber" TEXT,
    "isEmailVerified" BOOLEAN
)
LANGUAGE sql
AS $$
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId",
        u."phoneNumber",
        u."email_verified" AS isEmailVerified

    FROM users u

    JOIN user_roles ur
        ON ur."userId" = u."userId"

    JOIN roles r
        ON r."roleId" = ur."roleId"

    WHERE
        ur."status" = 'ACTIVE'
        AND r."roleCode" = 'ADMIN';
$$;

CREATE OR REPLACE FUNCTION approve_approval_request(
    p_approval_id INT,
    p_approved_by INT,
    p_reason TEXT DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE approval_requests
    SET
        "approvalStatus" = 'APPROVED',
        "approverUserId" = p_approved_by,
        "decisionReason" = p_reason,
        "decidedAt" = NOW()

    WHERE
        "approvalId" = p_approval_id
        AND "approvalStatus" = 'PENDING';

    RETURN FOUND;

END;
$$;

CREATE OR REPLACE FUNCTION reject_approval_request(
    p_approval_id INT,
    p_rejected_by INT,
    p_reason TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE approval_requests
    SET
        "approvalStatus" = 'REJECTED',
        "approverUserId" = p_rejected_by,
        "decisionReason" = p_reason,
        "decidedAt" = NOW()

    WHERE
        "approvalId" = p_approval_id
        AND "approvalStatus" = 'PENDING';

    RETURN FOUND;

END;
$$;