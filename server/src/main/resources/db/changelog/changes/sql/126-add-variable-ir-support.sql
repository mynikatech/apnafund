DROP FUNCTION IF EXISTS get_loan_details_with_names_for_fund(INT);
DROP FUNCTION IF EXISTS get_loan_details_with_names_for_fund_user(INT,INT);
DROP FUNCTION get_loan_complete( int);
DROP FUNCTION IF EXISTS save_or_update_all_loan_emis_and_fetch(INT, TEXT, TEXT, JSONB);
DROP FUNCTION IF EXISTS add_loan(TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, TEXT, INT);
DROP FUNCTION IF EXISTS add_loan(TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, BOOLEAN, DOUBLE PRECISION, INT, TEXT, INT);
DROP FUNCTION IF EXISTS update_loan(INT, TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, TEXT, INT);
DROP FUNCTION IF EXISTS update_loan(INT, TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, BOOLEAN, DOUBLE PRECISION, INT, TEXT, INT);
DROP FUNCTION IF EXISTS insert_loan_with_details(TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, TEXT, INT, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION);
DROP FUNCTION IF EXISTS update_loan_with_details(INT, TEXT, INT, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, TEXT, INT, INT, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION);
DROP FUNCTION IF EXISTS get_loan_by_id(INT);
DROP FUNCTION IF EXISTS get_all_loan_emis_with_names_for_fund_month_year(INT, TEXT, TEXT);
DROP FUNCTION IF EXISTS get_all_loan_emis_with_names_for_loan(INT);
DROP FUNCTION IF EXISTS calculate_expected_interest(
    DOUBLE PRECISION,
    DOUBLE PRECISION,
    BOOLEAN,
    DOUBLE PRECISION,
    INTEGER,
    INTEGER,
    INTEGER
);

CREATE OR REPLACE FUNCTION calculate_expected_interest(
    p_principal DOUBLE PRECISION,
    p_rateOfInterest DOUBLE PRECISION,
    p_hasVariableInterestRate BOOLEAN,
    p_revisedLoanInterestRate DOUBLE PRECISION,
    p_interestRateRevisionAfterMonths INTEGER,
    p_loanPeriod INTEGER,
    p_monthsElapsed INTEGER
)
RETURNS DOUBLE PRECISION
LANGUAGE plpgsql
AS $$
DECLARE
    v_totalInterest DOUBLE PRECISION := 0;
    v_month_no INTEGER;
    v_rate DOUBLE PRECISION;
BEGIN

    FOR v_month_no IN p_monthsElapsed .. (p_loanPeriod - 1)
    LOOP

        v_rate :=
            CASE
                WHEN COALESCE(p_hasVariableInterestRate, FALSE)
                     AND p_interestRateRevisionAfterMonths IS NOT NULL
                     AND v_month_no >= p_interestRateRevisionAfterMonths
                THEN COALESCE(
                        p_revisedLoanInterestRate,
                        p_rateOfInterest
                     )
                ELSE p_rateOfInterest
            END;

        v_totalInterest :=
            v_totalInterest +
            ((p_principal * v_rate / 12.0) / 100.0);

    END LOOP;

    RETURN ROUND(v_totalInterest::numeric, 2);
END;
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
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
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
  l."hasVariableInterestRate",
  l."revisedLoanInterestRate",
  l."interestRateRevisionAfterMonths",
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
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
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
    l."hasVariableInterestRate",
    l."revisedLoanInterestRate",
    l."interestRateRevisionAfterMonths",
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
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
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
  l."hasVariableInterestRate",
  l."revisedLoanInterestRate",
  l."interestRateRevisionAfterMonths",
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

CREATE OR REPLACE FUNCTION add_loan(
  p_loanNumber text, p_borrowerId int, p_issuedDate text, p_period double precision,
  p_loanAmount double precision, p_maturityDate text, p_rateOfInterest double precision,
  p_hasVariableInterestRate boolean, p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int,p_status text, p_fundId int
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO loans("loanNumber","borrowerId","issuedDate","period","loanAmount",
                    "maturityDate","rateOfInterest",
                     "hasVariableInterestRate","revisedLoanInterestRate",
                     "interestRateRevisionAfterMonths","status","fundId")
  VALUES (p_loanNumber,p_borrowerId,p_issuedDate,p_period,p_loanAmount,
          p_maturityDate,p_rateOfInterest,COALESCE(p_hasVariableInterestRate,FALSE),
          p_revisedLoanInterestRate, p_interestRateRevisionAfterMonths,
          COALESCE(p_status,'ACTIVE'),p_fundId)
  RETURNING "loanId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_loan(
  p_loanId int,
  p_loanNumber text, p_borrowerId int, p_issuedDate text, p_period double precision,
  p_loanAmount double precision, p_maturityDate text, p_rateOfInterest double precision,
  p_hasVariableInterestRate boolean, p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int, p_status text, p_fundId int
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE loans
     SET "loanNumber" = COALESCE(p_loanNumber, "loanNumber"),
         "borrowerId" = COALESCE(p_borrowerId, "borrowerId"),
         "issuedDate" = COALESCE(p_issuedDate, "issuedDate"),
         "period"     = COALESCE(p_period, "period"),
         "loanAmount" = COALESCE(p_loanAmount, "loanAmount"),
         "maturityDate" = COALESCE(p_maturityDate, "maturityDate"),
         "rateOfInterest" = COALESCE(p_rateOfInterest, "rateOfInterest"),
         "hasVariableInterestRate" =
             COALESCE(
                 p_hasVariableInterestRate,
                 "hasVariableInterestRate"
             ),

         "revisedLoanInterestRate" =
             COALESCE(
                 p_revisedLoanInterestRate,
                 "revisedLoanInterestRate"
             ),

         "interestRateRevisionAfterMonths" =
             COALESCE(
                 p_interestRateRevisionAfterMonths,
                 "interestRateRevisionAfterMonths"
             ),
         "status"     = COALESCE(p_status, "status"),
         "fundId"     = COALESCE(p_fundId, "fundId")
   WHERE "loanId" = p_loanId;
  RETURN FOUND;
END $$;


CREATE OR REPLACE FUNCTION insert_loan_with_details(
  p_loanNumber       text,              -- not used (kept for signature compatibility)
  p_borrowerId       int,
  p_issuedDate       text,              -- your schema stores dates as text
  p_period           double precision,
  p_loanAmount       double precision,
  p_maturityDate     text,
  p_rateOfInterest   double precision,
  p_hasVariableInterestRate boolean,
  p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int,
  p_status           text,
  p_fundId           int,
  p_origPrincipal    double precision,
  p_totalInterest    double precision,  -- << interest-only expected maturity component
  p_totalAmount      double precision,  -- (not used for expected maturity)
  p_emiInterest      double precision,
  p_currTotalIntPaid double precision,
  p_currPrincipal    double precision
) RETURNS int
LANGUAGE plpgsql AS $$
DECLARE
  v_loan_id   int;
  v_base      text;
  v_next_seq  int;
  v_next_sfx  text;
  v_max_seq   int;
  v_pat       text := '\$(\d{2})$';
BEGIN
  -- Build base key and lock it to avoid race on suffix allocation
  v_base := format('LN$%s$%s$%s', p_borrowerId, p_issuedDate, p_fundId);
  PERFORM pg_advisory_xact_lock(hashtext(v_base));

  -- Find the max existing 2-digit suffix for this base (legacy bare base counts as 01)
  SELECT COALESCE(
           MAX(
             CASE
               WHEN l."loanNumber" = v_base THEN 1
               ELSE NULLIF(substring(l."loanNumber" FROM v_pat), '')::int
             END
           ),
           0
         )
    INTO v_max_seq
  FROM loans l
  WHERE l."borrowerId" = p_borrowerId
    AND l."fundId"     = p_fundId
    AND l."issuedDate" = p_issuedDate
    AND (l."loanNumber" = v_base OR l."loanNumber" LIKE v_base || '$%');

  v_next_seq := v_max_seq + 1;
  IF v_next_seq > 99 THEN
    RAISE EXCEPTION 'Sequence overflow for base % (more than 99 loans on same key)', v_base
      USING ERRCODE = 'unique_violation';
  END IF;

  v_next_sfx := LPAD(v_next_seq::text, 2, '0');
  v_base := v_base || '$' || v_next_sfx;  -- final generated loanNumber

  -- Insert loan row using your existing helper
  v_loan_id := add_loan(
                  v_base,
                  p_borrowerId,
                  p_issuedDate,
                  p_period,
                  p_loanAmount,
                  p_maturityDate,
                  p_rateOfInterest,
                  p_hasVariableInterestRate,
                  p_revisedLoanInterestRate,
                  p_interestRateRevisionAfterMonths,
                  COALESCE(p_status,'ACTIVE'),
                  p_fundId
               );

  -- Insert details
  PERFORM insert_loan_details(
            v_loan_id,
            p_origPrincipal,
            p_totalInterest,
            p_totalAmount,
            p_emiInterest,
            p_currTotalIntPaid,
            p_currPrincipal
          );

  -- Expected Maturity = INTEREST ONLY (+ late fees elsewhere)
  UPDATE fund_details
     SET "totalExpectedMaturityAmount" =
           COALESCE("totalExpectedMaturityAmount",0) + COALESCE(p_totalInterest,0)
   WHERE "fundId" = p_fundId;

  IF NOT FOUND THEN
    INSERT INTO fund_details(
      "fundId",
      "totalExpectedDeposit",
      "totalCurrentDeposit",
      "totalCurrentLateFee",
      "totalCurrentInterestCollected",
      "totalExpectedMaturityAmount",
      "totalCurrAmount"
    ) VALUES (
      p_fundId, 0, 0, 0, 0, COALESCE(p_totalInterest,0), 0
    );
  END IF;

  RETURN v_loan_id;
END
$$;

CREATE OR REPLACE FUNCTION update_loan_with_details(
  p_loanId           int,
  p_loanNumber       text,
  p_borrowerId       int,
  p_issuedDate       text,
  p_period           double precision,
  p_loanAmount       double precision,
  p_maturityDate     text,
  p_rateOfInterest   double precision,
  p_hasVariableInterestRate boolean,
  p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int,
  p_status           text,
  p_fundId           int,
  p_loanDetailsId    int,
  p_origPrincipal    double precision,
  p_totalInterest    double precision,  -- << interest-only drives expected maturity
  p_totalAmount      double precision,
  p_emiInterest      double precision,
  p_currTotalIntPaid double precision,
  p_currPrincipal    double precision
) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
  ok                   boolean;
  v_old_total_interest double precision;
  v_delta_interest     double precision;
BEGIN
  -- Read current planned totalInterest on the details row to compute delta
  SELECT ld."totalInterest"
    INTO v_old_total_interest
  FROM loan_details ld
  WHERE ld."loanDetailsId" = p_loanDetailsId
  LIMIT 1;

  -- Update loan & details using your existing helpers
  ok := update_loan(
          p_loanId,
          p_loanNumber,
          p_borrowerId,
          p_issuedDate,
          p_period,
          p_loanAmount,
          p_maturityDate,
          p_rateOfInterest,
          p_hasVariableInterestRate,
          p_revisedLoanInterestRate,
          p_interestRateRevisionAfterMonths,
          p_status,
          p_fundId
       );

  PERFORM update_loan_details(
            p_loanDetailsId,
            p_loanId,
            p_origPrincipal,
            p_totalInterest,
            p_totalAmount,
            p_emiInterest,
            p_currTotalIntPaid,
            p_currPrincipal
          );

  -- Expected maturity delta = new planned interest - old planned interest
  v_delta_interest := COALESCE(p_totalInterest,0) - COALESCE(v_old_total_interest,0);

  IF v_delta_interest <> 0 THEN
    UPDATE fund_details
       SET "totalExpectedMaturityAmount" =
             COALESCE("totalExpectedMaturityAmount",0) + v_delta_interest
     WHERE "fundId" = p_fundId;

    IF NOT FOUND THEN
      INSERT INTO fund_details(
        "fundId",
        "totalExpectedDeposit",
        "totalCurrentDeposit",
        "totalCurrentLateFee",
        "totalCurrentInterestCollected",
        "totalExpectedMaturityAmount",
        "totalCurrAmount"
      ) VALUES (
        p_fundId, 0, 0, 0, 0, COALESCE(v_delta_interest,0), 0
      );
    END IF;
  END IF;

  RETURN ok;
END
$$;

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
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
  "status"        text,
  "closedDate"    text,
  "closureType"   text,
  "closureSource" text,

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
    l."hasVariableInterestRate",
    l."revisedLoanInterestRate",
    l."interestRateRevisionAfterMonths",
    l."status",
    l."closedDate",
    l."closureType",
    l."closureSource",

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

CREATE OR REPLACE FUNCTION get_all_loan_emis_with_names_for_loan(p_loan_id int)
RETURNS TABLE (
  "loanEmiId" int, "loanId" int, "emiMonth" text, "emiYear" text, "emiDepositedDate" text,
  "emiDepositedAmount" double precision, "prepaymentAmount" double precision, "lateFee" double precision,

  "fundId" int, "loanNumber" text, "borrowerId" int, "issuedDate" text, "period" double precision,
  "loanAmount" double precision, "maturityDate" text, "rateOfInterest" double precision,
  "hasVariableInterestRate" boolean, "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,"status" text,
  "closedDate" text, "closureType" text, "closureSource" text,

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
    l."rateOfInterest", l."hasVariableInterestRate",l."revisedLoanInterestRate",
    l."interestRateRevisionAfterMonths",l."status", l."closedDate", l."closureType", l."closureSource",

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




CREATE OR REPLACE FUNCTION save_or_update_all_loan_emis_and_fetch(
  p_fund_id int,
  p_month   text,
  p_year    text,
  p_emis    jsonb
)
RETURNS TABLE (
  "loanEmiId"           int,
  "loanId"              int,
  "emiMonth"            text,
  "emiYear"             text,
  "emiDepositedDate"    text,
  "emiDepositedAmount"  double precision,
  "prepaymentAmount"    double precision,
  "lateFee"             double precision,
  "fundId"              int,
  "loanNumber"          text,
  "borrowerId"          int,
  "issuedDate"          text,
  "period"              double precision,
  "loanAmount"          double precision,
  "maturityDate"        text,
  "rateOfInterest"      double precision,
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
  "status"              text,
  "closedDate"          text,
  "closureType"         text,
  "closureSource"       text,
  "emiInterest"         double precision,
  "currPrincipal"       double precision,
  "totalInterest"       double precision,
  "currTotalIntPaid"    double precision,
  "firstName"           text,
  "lastName"            text,
  "userId"              int
)
LANGUAGE plpgsql
AS $$
DECLARE
  -- fund rollups snapshot
  v_fd_id               int;
  v_totalInt            double precision;
  v_totalLate           double precision;
  v_totalCurr           double precision;
  v_totalExpMaturity    double precision;

  -- loop vars from payload
  v_loan_id             int;
  v_month               text := p_month::text;
  v_year                text := p_year::text;
  v_date                date;
  v_date_text           text;
  v_amt                 double precision;
  v_prepay              double precision;
  v_late                double precision;

  -- existing EMI row (if any)
  v_existing            record;
  v_interest_delta      double precision;
  v_late_delta          double precision;
  v_had_existing        boolean;

  -- loan + details snapshot
  v_ld                  record;
  v_currBal             double precision;
  v_old_monthly_int     double precision;
  v_updtEmiInt          double precision;
  v_isClosed            boolean;

  -- prepayment → interest-saved (exclude all months that already have EMI + exclude current month)
  v_issue_date          date;
  v_emi_month_dt        date;     -- first day of (v_year, v_month)
  v_sched_end_dt        date;     -- first day of the month after planned end
  v_period_months       int;
  v_months_elapsed      int;
  v_remaining_after_cur int;      -- remaining months strictly after current month
  v_paid_future         int;      -- count of future months that already have EMI rows
  v_unpaid_remaining    int;      -- remaining months without EMI rows (after current)
  v_interest_saved      double precision;
  v_new_expected        double precision;

  -- helpers for comparing months (YYYY*12 + MM)
  v_key_cur             int;
  v_key_end             int;
   -- variable to help use the effective rate for variable interest rate
  v_effectiveRate double precision;
  v_old_principal          double precision;
  v_old_rate_months        integer;
  v_revised_rate_months    integer;
  v_old_rate_savings       double precision;
  v_revised_rate_savings   double precision;
BEGIN
  -- Load current fund_details (latest row)
  SELECT "fundDetailsId","totalCurrentInterestCollected","totalCurrentLateFee",
         "totalCurrAmount","totalExpectedMaturityAmount"
    INTO v_fd_id, v_totalInt, v_totalLate, v_totalCurr, v_totalExpMaturity
  FROM fund_details fd
  WHERE fd."fundId" = p_fund_id
  ORDER BY "fundDetailsId" DESC
  LIMIT 1;

  IF v_fd_id IS NULL THEN
    v_fd_id := 0;
    v_totalInt := 0;
    v_totalLate := 0;
    v_totalCurr := 0;
    v_totalExpMaturity := 0;
  END IF;

  -- Process each EMI row from JSON
  FOR v_loan_id, v_date, v_amt, v_prepay, v_late IN
    SELECT (e->>'loanId')::int,
           to_date(e->>'emiDepositedDate','DD/MM/YYYY')::date,
           COALESCE((e->>'emiDepositedAmount')::double precision, 0),
           COALESCE((e->>'prepaymentAmount')::double precision, 0),
           COALESCE((e->>'lateFee')::double precision, 0)
    FROM jsonb_array_elements(p_emis) AS e
  LOOP
    v_isClosed := false;
    v_date_text := CASE WHEN v_date IS NOT NULL THEN to_char(v_date,'DD/MM/YYYY') ELSE NULL END;

    -- Existing EMI for same loan/month/year?
    SELECT * INTO v_existing
    FROM loan_emi le
    WHERE le."loanId" = v_loan_id
      AND le."emiMonth" = v_month
      AND le."emiYear"  = v_year
    LIMIT 1;
    v_had_existing := FOUND;

    IF v_had_existing THEN
      v_interest_delta := v_amt  - COALESCE(v_existing."emiDepositedAmount", 0);
      v_late_delta     := v_late - COALESCE(v_existing."lateFee", 0);

      UPDATE loan_emi le
         SET "emiDepositedDate"   = COALESCE(v_date_text, le."emiDepositedDate"),
             "emiDepositedAmount" = v_amt,
             "prepaymentAmount"   = v_prepay,
             "lateFee"            = v_late
       WHERE le."loanEmiId" = v_existing."loanEmiId";

      -- Actuals (interest + late fee deltas)
      v_totalInt  := v_totalInt  + v_interest_delta;
      v_totalLate := v_totalLate + v_late_delta;
      v_totalCurr := v_totalCurr + v_interest_delta + v_late_delta;

      -- Expected maturity: ONLY late-fee delta
      v_totalExpMaturity := v_totalExpMaturity + v_late_delta;

    ELSE
      INSERT INTO loan_emi(
        "loanId","emiMonth","emiYear","emiDepositedDate",
        "emiDepositedAmount","prepaymentAmount","lateFee"
      )
      VALUES (v_loan_id, v_month, v_year, v_date_text, v_amt, v_prepay, v_late);

      -- Actuals (first-time entry this month)
      v_totalInt  := v_totalInt  + v_amt;
      v_totalLate := v_totalLate + v_late;
      v_totalCurr := v_totalCurr + v_amt + v_late;

      -- Expected maturity: ONLY late-fee (no EMI amount)
      v_totalExpMaturity := v_totalExpMaturity + v_late;
    END IF;

    -- Loan details snapshot
    SELECT ld."loanDetailsId", ld."loanId", ld."origPrincipal", ld."totalInterest", ld."totalAmount",
           ld."emiInterest", ld."currTotalIntPaid", ld."currPrincipal",
           l."rateOfInterest", l."hasVariableInterestRate",
           l."revisedLoanInterestRate", l."interestRateRevisionAfterMonths",
           l."status", l."loanNumber", l."borrowerId",
           l."issuedDate", l."period", l."loanAmount", l."maturityDate", l."fundId"
      INTO v_ld
    FROM loan_details ld
    JOIN loans l ON l."loanId" = ld."loanId"
    WHERE ld."loanId" = v_loan_id
    ORDER BY ld."loanDetailsId" DESC
    LIMIT 1;

    IF NOT FOUND THEN
      CONTINUE;
    END IF;

    v_old_monthly_int := COALESCE(v_ld."emiInterest", 0);

    -- Determine EMI month context once
    v_issue_date := to_date(v_ld."issuedDate", 'DD/MM/YYYY');

    v_emi_month_dt :=
        to_date(
            v_year || '-' || lpad(v_month, 2, '0') || '-01',
            'YYYY-MM-DD'
        );

    v_months_elapsed :=
        (date_part('year', age(v_emi_month_dt, v_issue_date))::int * 12)
      + (date_part('month', age(v_emi_month_dt, v_issue_date))::int);

    -- Principal after prepayment
    IF COALESCE(v_prepay, 0) > 0 THEN
      IF v_ld."currPrincipal" = v_ld."origPrincipal" THEN
        v_currBal := v_ld."origPrincipal" - v_prepay;
      ELSE
        v_currBal := v_ld."currPrincipal" - v_prepay;
      END IF;

      IF v_currBal <= 0 THEN
        v_currBal := 0;
        v_isClosed := true;
      END IF;
    ELSE
      v_currBal := v_ld."currPrincipal";
    END IF;

    -- New monthly interest after prepayment
     v_effectiveRate := v_ld."rateOfInterest";

     IF COALESCE(v_ld."hasVariableInterestRate", FALSE)
        AND v_ld."interestRateRevisionAfterMonths" IS NOT NULL
        AND v_months_elapsed >= v_ld."interestRateRevisionAfterMonths"
     THEN
         v_effectiveRate :=
             COALESCE(
                 v_ld."revisedLoanInterestRate",
                 v_ld."rateOfInterest"
             );
     END IF;

     v_updtEmiInt :=
         (v_currBal * v_effectiveRate / 12.0) / 100.0;

    IF NOT v_had_existing THEN
      v_interest_delta := v_amt;
    END IF;

    -- Update loan_details (qualify RHS to avoid OUT-param ambiguity)
    IF COALESCE(v_prepay, 0) > 0 OR COALESCE(v_interest_delta, 0) <> 0 THEN
      UPDATE loan_details
         SET "emiInterest"      = CASE WHEN COALESCE(v_prepay, 0) > 0
                                       THEN v_updtEmiInt
                                       ELSE loan_details."emiInterest" END,
             "currTotalIntPaid" = COALESCE(loan_details."currTotalIntPaid", 0)
                                   + COALESCE(v_interest_delta, 0),
             "currPrincipal"    = CASE WHEN COALESCE(v_prepay, 0) > 0
                                       THEN v_currBal
                                       ELSE loan_details."currPrincipal" END
       WHERE loan_details."loanDetailsId" = v_ld."loanDetailsId";
    END IF;

    ------------------------------------------------------------------
    -- PREPAYMENT → REDUCE EXPECTED MATURITY BY INTEREST SAVED
    -- Exclude:
    --   • the CURRENT EMI month
    --   • ANY months that already have an EMI row (including advance-paid)
    ------------------------------------------------------------------
    IF COALESCE(v_prepay, 0) > 0 THEN
      -- Schedule math
      v_period_months := CEIL(v_ld."period")::int;
      v_sched_end_dt  := (v_issue_date + (v_period_months || ' months')::interval)::date;
      v_old_principal :=
          CASE
              WHEN v_ld."currPrincipal" = v_ld."origPrincipal"
              THEN v_ld."origPrincipal"
              ELSE v_ld."currPrincipal"
          END;

      -- Remaining months strictly AFTER current month in the planned schedule
      v_remaining_after_cur := GREATEST(0, v_period_months - v_months_elapsed - 1);

      -- Compare keys YYYY*12 + MM
      v_key_cur := (EXTRACT(YEAR FROM v_emi_month_dt)::int) * 12
                 + (EXTRACT(MONTH FROM v_emi_month_dt)::int);

      -- Count how many FUTURE months already have EMI rows for this loan
      SELECT COUNT(*) INTO v_paid_future
      FROM loan_emi e
      WHERE e."loanId" = v_loan_id
        AND ( (e."emiYear")::int * 12 + (e."emiMonth")::int ) > v_key_cur;

      v_unpaid_remaining := GREATEST(0, v_remaining_after_cur - v_paid_future);

      -- Interest saved applies only to those unpaid-remaining months
      v_interest_saved := 0;

      IF COALESCE(v_ld."hasVariableInterestRate", FALSE)
         AND v_ld."interestRateRevisionAfterMonths" IS NOT NULL
      THEN

          v_old_rate_months :=
              LEAST(
                  v_unpaid_remaining,
                  GREATEST(
                      0,
                      v_ld."interestRateRevisionAfterMonths"
                      - (v_months_elapsed)
                  )
              );

          v_revised_rate_months :=
              GREATEST(
                  0,
                  v_unpaid_remaining - v_old_rate_months
              );

          v_old_rate_savings :=
              (
                  (v_old_principal * v_ld."rateOfInterest" / 12.0 / 100.0)
                  -
                  (v_currBal * v_ld."rateOfInterest" / 12.0 / 100.0)
              );

          v_interest_saved :=
              v_old_rate_savings * v_old_rate_months;

          IF v_revised_rate_months > 0 THEN

              v_revised_rate_savings :=
                  (
                      (v_old_principal * COALESCE(
                          v_ld."revisedLoanInterestRate",
                          v_ld."rateOfInterest"
                      ) / 12.0 / 100.0)
                      -
                      (v_currBal * COALESCE(
                          v_ld."revisedLoanInterestRate",
                          v_ld."rateOfInterest"
                      ) / 12.0 / 100.0)
                  );

              v_interest_saved :=
                  v_interest_saved
                  +
                  (v_revised_rate_savings * v_revised_rate_months);

          END IF;

      ELSE

          v_interest_saved :=
              GREATEST(
                  0,
                  v_old_monthly_int - v_updtEmiInt
              ) * v_unpaid_remaining;

      END IF;
      IF v_interest_saved > 0 THEN
        -- 1) Reduce fund expected maturity by the saved interest
        v_totalExpMaturity := GREATEST(0, COALESCE(v_totalExpMaturity,0) - v_interest_saved);

        -- 2) Also reduce planned totals on loan_details so plan reflects new schedule:
        UPDATE loan_details
           SET "totalInterest" = GREATEST(0, COALESCE(loan_details."totalInterest",0) - v_interest_saved),
               "totalAmount"   = GREATEST(0, COALESCE(loan_details."totalAmount",0)   - v_interest_saved)
         WHERE loan_details."loanDetailsId" = v_ld."loanDetailsId";
      END IF;
    END IF;

    -- Close loan if principal hit zero
    IF v_isClosed THEN
        UPDATE loans l
        SET "status" = 'CLOSED',
            "closedDate" = NOW(),
            "closureType" = CASE
                WHEN v_remaining_after_cur = 0 THEN 'MATURITY'
                ELSE 'FORECLOSURE'
            END,
            "closureSource" = CASE
                WHEN COALESCE(v_prepay, 0) > 0 THEN 'EMI'
                ELSE 'APPROVAL'
            END
        WHERE l."loanId" = v_ld."loanId";
    END IF;

    -- Persist fund_details rollups
    UPDATE fund_details fd
       SET "totalCurrentInterestCollected" = v_totalInt,
           "totalCurrentLateFee"           = v_totalLate,
           "totalCurrAmount"               = v_totalCurr,
           "totalExpectedMaturityAmount"   = GREATEST(0, v_totalExpMaturity)
     WHERE fd."fundId" = p_fund_id;

    IF NOT FOUND THEN
      INSERT INTO fund_details(
        "fundId","totalExpectedDeposit","totalCurrentDeposit",
        "totalCurrentLateFee","totalCurrentInterestCollected",
        "totalExpectedMaturityAmount","totalCurrAmount"
      ) VALUES (
        p_fund_id, 0, 0, v_totalLate, v_totalInt, GREATEST(0, v_totalExpMaturity), v_totalCurr
      );
    END IF;
  END LOOP;

  -- Return refreshed rows (with names) for the requested fund/month/year
  RETURN QUERY
    SELECT * FROM get_all_loan_emis_with_names_for_fund_month_year(p_fund_id, p_month, p_year);
END
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
    "hasVariableInterestRate" boolean,
    "revisedLoanInterestRate" double precision,
    "interestRateRevisionAfterMonths" int,
    "status" TEXT,
    "workflowStatus" TEXT,
    "closedDate"    TEXT,
    "closureType"   TEXT,
    "closureSource" TEXT
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
    l."hasVariableInterestRate",
    l."revisedLoanInterestRate",
    l."interestRateRevisionAfterMonths",
    l."status",
    l."workflowStatus",
    l."closedDate",
    l."closureType",
    l."closureSource"
FROM loans l
WHERE l."loanId" = p_loan_id;
$$;