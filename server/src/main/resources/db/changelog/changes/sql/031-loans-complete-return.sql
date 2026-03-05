DROP FUNCTION get_loan_complete( int);
DROP FUNCTION get_loan_details_with_names_for_fund( int);

CREATE OR REPLACE FUNCTION get_loan_complete(p_loan_id int)
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
  "workflowStatus" text,
  "loanDetailsId" int,
  "origPrincipal" double precision,
  "totalInterest" double precision,
  "totalAmount" double precision,
  "emiInterest" double precision,
  "currTotalIntPaid" double precision,
  "currPrincipal" double precision
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
  l."workflowStatus",
  ld."loanDetailsId",
  ld."origPrincipal",
  ld."totalInterest",
  ld."totalAmount",
  ld."emiInterest",
  ld."currTotalIntPaid",
  ld."currPrincipal"
FROM loans l
LEFT JOIN loan_details ld
  ON ld."loanId" = l."loanId"
WHERE l."loanId" = p_loan_id;
$$;

CREATE OR REPLACE FUNCTION get_loans_complete_for_fund(p_fund_id int)
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
  "workflowStatus" text,
  "loanDetailsId" int,
  "origPrincipal" double precision,
  "totalInterest" double precision,
  "totalAmount" double precision,
  "emiInterest" double precision,
  "currTotalIntPaid" double precision,
  "currPrincipal" double precision
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
  l."workflowStatus",
  ld."loanDetailsId",
  ld."origPrincipal",
  ld."totalInterest",
  ld."totalAmount",
  ld."emiInterest",
  ld."currTotalIntPaid",
  ld."currPrincipal"
FROM loans l
LEFT JOIN loan_details ld
  ON ld."loanId" = l."loanId"
WHERE l."fundId" = p_fund_id
ORDER BY l."issuedDate" DESC;
$$;

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

ORDER BY l."loanId";

$$;