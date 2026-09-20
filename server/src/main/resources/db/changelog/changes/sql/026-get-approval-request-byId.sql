DROP FUNCTION IF EXISTS get_approval_by_id(INT);

DROP FUNCTION IF EXISTS get_loan_by_id(INT);

DROP FUNCTION IF EXISTS activate_group(INT);

DROP FUNCTION IF EXISTS reject_group(INT);

ALTER TABLE loans
ADD COLUMN IF NOT EXISTS "workflowStatus" TEXT DEFAULT 'APPROVED';

CREATE OR REPLACE FUNCTION get_approval_by_id(
    p_approval_id INT
)
RETURNS TABLE(
    approvalId INT,
    entityType TEXT,
    entityId INT,
    requestedBy INT,
    approverUserId INT,
    approvalStatus TEXT
)
LANGUAGE sql
AS $$
SELECT
    "approvalId",
    "entityType",
    "entityId",
    "requestedBy",
    "approverUserId",
    "approvalStatus"
FROM approval_requests
WHERE "approvalId" = p_approval_id;
$$;

CREATE OR REPLACE FUNCTION get_loan_by_id(
    p_loan_id INT
)
RETURNS TABLE(
    "loanId" INT,
    "fundId" INT,
    "loanNumber" TEXT,
    "borrowerId" INT,
    "issuedDate" TEXT,
    "period" NUMERIC,
    "loanAmount" NUMERIC,
    "maturityDate" TEXT,
    "rateOfInterest" NUMERIC,
    "status" TEXT,
    "workflowStatus" TEXT
)
LANGUAGE sql
AS $$
SELECT
    l."loanId",
    l."fundId",
    l."loanNumber",
    l."borrowerId",
    l."issuedDate",
    l."period",
    l."loanAmount",
    l."maturityDate",
    l."rateOfInterest",
    l."status",
    l."workflowStatus"
FROM loans l
WHERE l."loanId" = p_loan_id;
$$;

CREATE OR REPLACE FUNCTION activate_group(
    p_group_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE groups
    SET
        "status" = 'ACTIVE',
        "updatedAt" = NOW()
    WHERE "groupId" = p_group_id;

    RETURN TRUE;

END;
$$;

CREATE OR REPLACE FUNCTION reject_group(
    p_group_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE groups
    SET
        "status" = 'REJECTED',
        "updatedAt" = NOW()
    WHERE "groupId" = p_group_id;

    RETURN TRUE;

END;
$$;

