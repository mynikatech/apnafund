DROP FUNCTION get_loan_details_with_names_for_fund_user(INT,INT);

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
  "workflowStatus" text,

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
    l."workflowStatus",

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
    AND l."workflowStatus" != 'REJECTED'
  ORDER BY d."loanDetailsId";  -- optional, pick the sort you want
$$;


