-- with names (fund/month/year)

DROP FUNCTION IF EXISTS get_all_loan_emis_with_names_for_fund_month_year(INT,TEXT,TEXT);
DROP FUNCTION IF EXISTS get_all_loan_emis_with_names_for_loan(INT);

CREATE OR REPLACE FUNCTION get_all_loan_emis_with_names_for_fund_month_year(
  p_fund_id int,
  p_month   text,
  p_year    text
)
RETURNS TABLE (
  -- loan_emi (may be NULL because of LEFT JOIN)
  "loanEmiId"          int,
  "loanId"             int,
  "emiMonth"           text,
  "emiYear"            text,
  "emiDepositedDate"   text,
  "emiDepositedAmount" double precision,
  "prepaymentAmount"   double precision,
  "lateFee"            double precision,

  -- loans
  "fundId"        int,
  "loanNumber"    text,
  "borrowerId"    int,
  "issuedDate"    text,
  "period"        double precision,
  "loanAmount"    double precision,
  "maturityDate"  text,
  "rateOfInterest" double precision,
  "status"        text,

  -- loan_details
  "emiInterest"       double precision,
  "currPrincipal"     double precision,
  "totalInterest"     double precision,
  "currTotalIntPaid"  double precision,

  -- users
  "firstName"   text,
  "lastName"    text,
  "userId"      int
)
LANGUAGE sql
STABLE
AS $$
  SELECT
    -- loan_emi (LEFT JOIN, so these may be NULL when no EMI row for that month/year)
    e."loanEmiId",
    l."loanId",
    e."emiMonth",
    e."emiYear",
    e."emiDepositedDate",
    e."emiDepositedAmount",
    e."prepaymentAmount",
    e."lateFee",

    -- loans
    l."fundId",
    l."loanNumber",
    l."borrowerId",
    l."issuedDate",
    l."period",
    l."loanAmount",
    l."maturityDate",
    l."rateOfInterest",
    l."status",

    -- loan_details
    d."emiInterest",
    d."currPrincipal",
    d."totalInterest",
    d."currTotalIntPaid",

    -- users
    u."firstName",
    COALESCE(u."lastName",'') AS "lastName",
    u."userId"
  FROM loans l
  INNER JOIN loan_details d ON l."loanId"   = d."loanId"
  INNER JOIN users        u ON l."borrowerId" = u."userId"
  LEFT  JOIN loan_emi     e ON l."loanId"   = e."loanId"
                           AND e."emiMonth" = p_month
                           AND e."emiYear"  = p_year
  WHERE l."fundId" = p_fund_id
    AND l."status" = 'ACTIVE'           -- NOTE: single quotes for string literal
    AND l."workflowStatus" NOT IN ( 'REJECTED', 'PENDING_APPROVAL')
  ORDER BY l."loanId";
$$;

-- with names (by loan)
CREATE OR REPLACE FUNCTION get_all_loan_emis_with_names_for_loan(p_loan_id int)
RETURNS TABLE (
  "loanEmiId" int, "loanId" int, "emiMonth" text, "emiYear" text, "emiDepositedDate" text,
  "emiDepositedAmount" double precision, "prepaymentAmount" double precision, "lateFee" double precision,

  "fundId" int, "loanNumber" text, "borrowerId" int, "issuedDate" text, "period" double precision,
  "loanAmount" double precision, "maturityDate" text, "rateOfInterest" double precision, "status" text,

  "emiInterest" double precision, "currPrincipal" double precision, "totalInterest" double precision,
  "currTotalIntPaid" double precision,

  "firstName" text, "lastName" text, "userId" int
)
LANGUAGE sql STABLE AS $$
  SELECT
    e."loanEmiId", e."loanId", e."emiMonth"::text, e."emiYear"::text, e."emiDepositedDate",
    e."emiDepositedAmount", e."prepaymentAmount", e."lateFee",

    l."fundId", l."loanNumber", l."borrowerId", l."issuedDate",
    l."period"::double precision, l."loanAmount", l."maturityDate",
    l."rateOfInterest", l."status",

    d."emiInterest", d."currPrincipal", d."totalInterest", d."currTotalIntPaid",

    u."firstName", COALESCE(u."lastName",'') AS "lastName", u."userId"
  FROM loans l
  JOIN loan_details d ON d."loanId" = l."loanId"
  JOIN users u        ON u."userId" = l."borrowerId"
  JOIN loan_emi e     ON e."loanId" = l."loanId"
 WHERE e."loanId" = p_loan_id
 AND l."workflowStatus" NOT IN ( 'REJECTED', 'PENDING_APPROVAL')
 ORDER BY e."loanEmiId" DESC;
$$;