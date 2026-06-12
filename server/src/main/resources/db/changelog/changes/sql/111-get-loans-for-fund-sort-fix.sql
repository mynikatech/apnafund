
DROP FUNCTION IF EXISTS get_loan_details_with_names_for_fund(INT);
DROP FUNCTION IF EXISTS get_loan_details_with_names_for_fund_user(INT,INT);

CREATE OR REPLACE FUNCTION get_loan_details_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "loanId" int,
  "fundId" int,
  "loanNumber" text,
  "borrowerId" int,
  "issuedDate" text,
  "period" double precision,
  "loanAmount" double precision,
  "maturityDate" text,
  "rateOfInterest" double precision,
  "status" text,
  "closedDate"    text,
  "closureType"   text,
  "closureSource" text,
  "loanDetailsId" int,
  "totalInterest" double precision,
  "emiInterest" double precision,
  "currTotalIntPaid" double precision,
  "currPrincipal" double precision,
  "firstName" text,
  "lastName" text,
  "userId" int,
  "workflowStatus" text
)
LANGUAGE sql AS $$

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
  l."closedDate",
  l."closureType",
  l."closureSource",
  ld."loanDetailsId",
  ld."totalInterest",
  ld."emiInterest",
  ld."currTotalIntPaid",
  ld."currPrincipal",
  u."firstName",
  COALESCE(u."lastName",''),
  u."userId",
  l."workflowStatus"

FROM loans l
LEFT JOIN loan_details ld
  ON ld."loanId" = l."loanId"

JOIN users u
  ON u."userId" = l."borrowerId"

WHERE l."fundId" = p_fund_id

ORDER BY TO_DATE(l."issuedDate", 'DD/MM/YYYY') DESC;

$$;

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
  "closedDate"    text,
  "closureType"   text,
  "closureSource" text,
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
    l."closedDate",
    l."closureType",
    l."closureSource",
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
    ORDER BY TO_DATE(l."issuedDate", 'DD/MM/YYYY') DESC;
$$;