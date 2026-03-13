DROP FUNCTION get_pending_approvals(INT);

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

WHERE a."approvalStatus"='PENDING'
AND a."approverUserId"=p_user_id

ORDER BY a."requestedAt" DESC;
$$;