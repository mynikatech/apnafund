-- For a given fund & user with names (adjust to your DTO)

DROP FUNCTION get_loan_details_with_names_for_fund_user(INT,INT);
DROP FUNCTION reject_loan_request(INT,INT,TEXT);

CREATE OR REPLACE FUNCTION get_loan_details_with_names_for_fund_user(
  p_fund_id int,
  p_user_id int
)
RETURNS TABLE (
  "fundId" int,
  "loanId" int,
  "loanNumber" text,
  "borrowerId" int,
  "issuedDate" text,
  "period" double precision,
  "loanAmount" double precision,
  "maturityDate" text,
  "rateOfInterest" double precision,
  "status" text,

  "emiInterest" double precision,
  "currPrincipal" double precision,
  "currTotalIntPaid" double precision,
  "totalInterest" double precision,
  "loanDetailsId" int,

  "firstName" text,
  "lastName" text,
  "userId" int
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    l."fundId",
    l."loanId",
    l."loanNumber",
    l."borrowerId",
    l."issuedDate",
    l."period",
    l."loanAmount",
    l."maturityDate",
    l."rateOfInterest",
    l."status",

    d."emiInterest",
    d."currPrincipal",
    d."currTotalIntPaid",
    d."totalInterest",
    d."loanDetailsId",

    u."firstName",
    COALESCE(u."lastName",'') AS "lastName",
    u."userId"
  FROM loans l
  INNER JOIN loan_details d ON l."loanId" = d."loanId"
  INNER JOIN users u        ON l."borrowerId" = u."userId"
  WHERE l."fundId" = p_fund_id
    AND u."userId" = p_user_id
    AND l."status" = 'ACTIVE' AND l."workflowStatus" = 'APPROVED'
  ORDER BY d."loanDetailsId";  -- optional, pick the sort you want
$$;


CREATE OR REPLACE FUNCTION reject_loan_request(
    p_approval_id INT,
    p_rejected_by INT,
    p_reason TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_loan_id INT;
BEGIN

    SELECT "entityId"
    INTO v_loan_id
    FROM approval_requests
    WHERE "approvalId" = p_approval_id
      AND "approvalStatus" = 'PENDING'
      AND "approverUserId" = p_rejected_by;

    IF v_loan_id IS NULL THEN
        RETURN FALSE;
    END IF;

    UPDATE approval_requests
    SET
        "approvalStatus" = 'REJECTED',
        "decisionReason" = p_reason,
        "decidedAt" = NOW()
    WHERE "approvalId" = p_approval_id;

    UPDATE loans
    SET "workflowStatus" = 'REJECTED',
        "status" = 'INACTIVE'
    WHERE "loanId" = v_loan_id;

    RETURN TRUE;

END;
$$;