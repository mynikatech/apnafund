DROP FUNCTION IF EXISTS save_or_update_all_loan_emis_and_fetch(INT, TEXT, TEXT, JSONB, TEXT);

CREATE OR REPLACE FUNCTION save_or_update_all_loan_emis_and_fetch(
  p_fund_id int,
  p_month   text,
  p_year    text,
  p_emis    jsonb,
  p_closure_source TEXT DEFAULT NULL
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
                      -1
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

       ------------------------------------------------------------------
       -- Loan closed WITHOUT collecting current month's interest.
       -- This happens during Close Loan where:
       --    EMI Amount = 0
       --    Prepayment = Outstanding Principal
       --
       -- Remove the current month's planned interest from the loan totals
       -- and fund expected maturity.
       ------------------------------------------------------------------
       IF COALESCE(v_amt,0) = 0
          AND COALESCE(v_prepay,0) > 0 THEN

           -- Reduce expected maturity for the fund
           v_totalExpMaturity :=
               GREATEST(
                   0,
                   COALESCE(v_totalExpMaturity,0)
                   - COALESCE(v_old_monthly_int,0)
               );

           -- Reduce planned totals for the loan
           UPDATE loan_details ld
           SET
               "totalInterest" =
                   GREATEST(
                       0,
                       COALESCE(ld."totalInterest",0)
                       - COALESCE(v_old_monthly_int,0)
                   ),

               "totalAmount" =
                   GREATEST(
                       0,
                       COALESCE(ld."totalAmount",0)
                       - COALESCE(v_old_monthly_int,0)
                   )
           WHERE ld."loanDetailsId" = v_ld."loanDetailsId";

       END IF;

       ------------------------------------------------------------------
       -- Mark loan closed
       ------------------------------------------------------------------
       UPDATE loans l
       SET "status" = 'CLOSED',
           "closedDate" = TO_CHAR(CURRENT_DATE, 'DD/MM/YYYY'),
           "closureType" = CASE
               WHEN v_remaining_after_cur = 0 THEN 'MATURITY'
               ELSE 'FORECLOSURE'
           END,
           "closureSource" =
               COALESCE(
                   p_closure_source,
                   'EMI'
               )
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