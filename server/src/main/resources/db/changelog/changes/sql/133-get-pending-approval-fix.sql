
DROP FUNCTION IF EXISTS get_pending_approvals(INT);

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
    "requesterEmail" TEXT,

    "loanNumber" TEXT,
    "requestedAmount" TEXT,
    "loamOutstandingAmount" TEXT,
    "loanAmount" TEXT,
    "loanPeriod" TEXT,
    "LoanIntRate" TEXT,

    "groupName" TEXT,
    "groupDescription" TEXT
)
LANGUAGE sql
AS $$
SELECT
    a."approvalId",
    a."entityType",
    a."entityId",

    /* ---------- TITLE ---------- */
    CASE
        WHEN a."entityType" = 'LOAN'
            THEN 'Loan Approval Required'
        WHEN a."entityType" = 'GROUP'
            THEN 'Group Approval Required'
        ELSE 'Approval Required'
    END AS "title",

    /* ---------- SUBTITLE ---------- */
    CASE
        WHEN a."entityType" = 'LOAN'
            THEN 'Loan requested in fund: ' || f."fundName"
        WHEN a."entityType" = 'GROUP'
            THEN 'New group awaiting activation'
        ELSE ''
    END AS "subtitle",

    /* ---------- DESCRIPTION ---------- */
    CASE
        WHEN a."entityType" = 'LOAN'
            THEN 'Borrower: ' || bu."firstName" || ' ' || bu."lastName"
        WHEN a."entityType" = 'GROUP'
            THEN 'Group: ' || g."groupName" || '  Description: ' || g."description"
        ELSE NULL
    END AS "description",

    /* ---------- REQUESTER ---------- */
    ru."firstName" || ' ' || ru."lastName" AS "requesterName",

    a."requestedAt" AS "createdAt",

    a."requestedBy" AS "requesterUserId",

    ru."emailId" AS "requesterEmail",

    /* ---------- LOAN DETAILS ---------- */
    CASE
        WHEN a."entityType" = 'LOAN'
            THEN l."loanNumber"
        ELSE NULL
    END AS "loanNumber",

    CASE
        WHEN a."entityType" = 'LOAN'
            THEN l."loanAmount"::TEXT
        ELSE NULL
    END AS "requestedAmount",

    CASE
        WHEN a."entityType" = 'LOAN'
            THEN ld."currPrincipal"::TEXT
        ELSE NULL
    END AS "loamOutstandingAmount",

    CASE
        WHEN a."entityType" = 'LOAN'
            THEN l."loanAmount"::TEXT
        ELSE NULL
    END AS "loanAmount",

    CASE
        WHEN a."entityType" = 'LOAN'
            THEN l."period"::TEXT
        ELSE NULL
    END AS "loanPeriod",

    CASE
        WHEN a."entityType" = 'LOAN'
            THEN l."rateOfInterest"::TEXT
        ELSE NULL
    END AS "LoanIntRate",

    /* ---------- GROUP DETAILS ---------- */
    CASE
        WHEN a."entityType" = 'GROUP'
            THEN g."groupName"
        ELSE NULL
    END AS "groupName",

    CASE
        WHEN a."entityType" = 'GROUP'
            THEN g."description"
        ELSE NULL
    END AS "groupDescription"

FROM approval_requests a

JOIN users ru
    ON ru."userId" = a."requestedBy"

/* ---------- LOAN ---------- */
LEFT JOIN loans l
    ON a."entityType" = 'LOAN'
   AND l."loanId" = a."entityId"

LEFT JOIN loan_details ld
    ON ld."loanId" = l."loanId"

LEFT JOIN funds f
    ON f."fundId" = l."fundId"

LEFT JOIN users bu
    ON bu."userId" = l."borrowerId"

/* ---------- GROUP ---------- */
LEFT JOIN groups g
    ON a."entityType" = 'GROUP'
   AND g."groupId" = a."entityId"

WHERE
    a."approvalStatus" = 'PENDING'
    AND
    (
        (
            a."entityType" = 'LOAN'
            AND EXISTS (
                SELECT 1
                FROM fund_members fm
                WHERE fm."fundId" = f."fundId"
                  AND fm."userId" = p_user_id
                  AND fm."status" = 'ACTIVE'
                  AND fm."role" IN ('PRIMARY_MODERATOR', 'MODERATOR')
            )
        )

        OR

        EXISTS (
            SELECT 1
            FROM user_roles ur
            JOIN roles r
                ON r."roleId" = ur."roleId"
            WHERE ur."userId" = p_user_id
              AND ur."status" = 'ACTIVE'
              AND r."roleCode" = 'ADMIN'
        )
    )

ORDER BY a."requestedAt" DESC;
$$;