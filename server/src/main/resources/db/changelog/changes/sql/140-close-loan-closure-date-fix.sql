DROP FUNCTION IF EXISTS close_loan(INT, INT, TEXT, TEXT);
DROP FUNCTION IF EXISTS get_pending_loan_emis_for_closure(INT, DATE);


CREATE OR REPLACE FUNCTION get_pending_loan_emis_for_closure(
    p_loan_id INT,
    p_closure_date DATE
)
RETURNS TABLE
(
    "fundId"               INT,
    "loanId"               INT,
    "emiMonth"             TEXT,
    "emiYear"              TEXT,
    "emiDepositedDate"     TEXT,
    "emiDepositedAmount"   DOUBLE PRECISION,
    "prepaymentAmount"     DOUBLE PRECISION,
    "lateFee"              DOUBLE PRECISION
)
LANGUAGE plpgsql
AS
$$
DECLARE
    v_loan               RECORD;
    v_issue_month        DATE;
    v_closure_month      DATE;
    v_curr_month         DATE;
    v_months_elapsed     INT;
    v_effective_rate     DOUBLE PRECISION;
    v_monthly_interest   DOUBLE PRECISION;
BEGIN
    ----------------------------------------------------------------------
    -- Load loan
    ----------------------------------------------------------------------
    SELECT
        l."loanId",
        l."fundId",
        l."issuedDate",
        l."rateOfInterest",
        l."hasVariableInterestRate",
        l."revisedLoanInterestRate",
        l."interestRateRevisionAfterMonths",
        ld."currPrincipal"
    INTO v_loan
    FROM loans l
    JOIN loan_details ld
      ON ld."loanId" = l."loanId"
    WHERE l."loanId" = p_loan_id
    ORDER BY ld."loanDetailsId" DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN;
    END IF;

    ----------------------------------------------------------------------
    -- Iterate month-by-month
    ----------------------------------------------------------------------
    v_issue_month :=
        date_trunc('month',
            to_date(v_loan."issuedDate",'DD/MM/YYYY')
        )::date;

    v_closure_month :=
        date_trunc('month', p_closure_date)::date;

    v_curr_month := v_issue_month;

    WHILE v_curr_month < v_closure_month
    LOOP

        ------------------------------------------------------------------
        -- Skip if EMI already exists
        ------------------------------------------------------------------
        IF NOT EXISTS
        (
            SELECT 1
            FROM loan_emi le
            WHERE le."loanId" = p_loan_id
              AND le."emiMonth" = to_char(v_curr_month,'FMMM')
              AND le."emiYear"  = to_char(v_curr_month,'YYYY')
        )
        THEN

            --------------------------------------------------------------
            -- Months elapsed from issue
            --------------------------------------------------------------
            v_months_elapsed :=
                (
                    EXTRACT(YEAR FROM age(v_curr_month,v_issue_month))::INT * 12
                )
                +
                EXTRACT(MONTH FROM age(v_curr_month,v_issue_month))::INT;

            --------------------------------------------------------------
            -- Effective interest rate
            --------------------------------------------------------------
            v_effective_rate := v_loan."rateOfInterest";

            IF COALESCE(v_loan."hasVariableInterestRate",FALSE)
               AND v_loan."interestRateRevisionAfterMonths" IS NOT NULL
               AND v_months_elapsed >= v_loan."interestRateRevisionAfterMonths"
            THEN
                v_effective_rate :=
                    COALESCE(
                        v_loan."revisedLoanInterestRate",
                        v_loan."rateOfInterest"
                    );
            END IF;

            --------------------------------------------------------------
            -- Monthly interest
            --------------------------------------------------------------
            v_monthly_interest :=
                ROUND(
                    (
                        v_loan."currPrincipal"
                        * v_effective_rate
                        / 12.0
                    ) / 100.0,
                    2
                );

            ------------------------------------------------------------------
            -- Return row
            ------------------------------------------------------------------
            "fundId"             := v_loan."fundId";
            "loanId"             := p_loan_id;
            "emiMonth"           := to_char(v_curr_month,'FMMM');
            "emiYear"            := to_char(v_curr_month,'YYYY');
            "emiDepositedDate"   := to_char(p_closure_date,'DD/MM/YYYY');
            "emiDepositedAmount" := v_monthly_interest;
            "prepaymentAmount"   := 0;
            "lateFee"            := 0;

            RETURN NEXT;
        END IF;

        v_curr_month :=
            (v_curr_month + interval '1 month')::date;

    END LOOP;

END;
$$;

CREATE OR REPLACE FUNCTION close_loan(
    p_loan_id INT,
    p_approved_by INT,
    p_closure_source TEXT,
    p_closure_date TEXT DEFAULT TO_CHAR(CURRENT_DATE,'DD/MM/YYYY')
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_loan RECORD;
    v_details RECORD;

    v_interest_to_pay NUMERIC := 0;
    v_prepayment NUMERIC := 0;
    v_closure_amount NUMERIC := 0;

    v_month TEXT;
    v_year TEXT;
    v_date TEXT;

    v_emi_exists BOOLEAN;

    v_json JSONB;
BEGIN

    ------------------------------------------------------------------
    -- Validate closure source
    ------------------------------------------------------------------

    IF p_closure_source NOT IN ('AUTO_APPROVAL', 'REQUEST_APPROVED') THEN
        RAISE EXCEPTION 'Invalid closure source %', p_closure_source;
    END IF;

    ------------------------------------------------------------------
    -- Fetch loan
    ------------------------------------------------------------------

    SELECT *
    INTO v_loan
    FROM loans
    WHERE "loanId" = p_loan_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF v_loan."status" = 'CLOSED' THEN
        RETURN FALSE;
    END IF;

    ------------------------------------------------------------------
    -- Fetch loan details
    ------------------------------------------------------------------

    SELECT *
    INTO v_details
    FROM loan_details
    WHERE "loanId" = p_loan_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF COALESCE(v_details."currPrincipal",0) <= 0 THEN
        RETURN FALSE;
    END IF;

    ------------------------------------------------------------------
    -- Current Month
    ------------------------------------------------------------------

    IF to_char(to_date(p_closure_date,'DD/MM/YYYY'),'DD/MM/YYYY')
       <> p_closure_date THEN
        RAISE EXCEPTION 'Invalid closure date: %', p_closure_date;
    END IF;

    v_date := p_closure_date;

    v_month := TO_CHAR(TO_DATE(v_date, 'DD/MM/YYYY'), 'FMMM');
    v_year  := TO_CHAR(to_date(v_date,'DD/MM/YYYY'),'YYYY');

    ------------------------------------------------------------------
    -- Check if current month's EMI already exists
    ------------------------------------------------------------------

    SELECT EXISTS (
        SELECT 1
        FROM loan_emi
        WHERE "loanId" = p_loan_id
          AND "emiMonth" = v_month
          AND "emiYear" = v_year
    )
    INTO v_emi_exists;

    ------------------------------------------------------------------
    -- Determine interest payable
    ------------------------------------------------------------------

    IF v_emi_exists THEN
        v_interest_to_pay := 0;
    ELSE
        v_interest_to_pay := COALESCE(v_details."emiInterest", 0);
    END IF;

    ------------------------------------------------------------------
    -- Remaining principal
    ------------------------------------------------------------------

    v_prepayment := COALESCE(v_details."currPrincipal", 0);

    v_closure_amount :=
        v_interest_to_pay + v_prepayment;

    ------------------------------------------------------------------
    -- Build synthetic EMI JSON
    ------------------------------------------------------------------

    v_json :=
        jsonb_build_array(
            jsonb_build_object(
                'loanId', p_loan_id,
                'emiDepositedDate', v_date,
                'emiDepositedAmount', v_interest_to_pay,
                'prepaymentAmount', v_prepayment,
                'lateFee', 0
            )
        );

    ------------------------------------------------------------------
    -- Accounting Engine
    ------------------------------------------------------------------

    IF NOT EXISTS (
        SELECT 1
        FROM save_or_update_all_loan_emis_and_fetch(
            v_loan."fundId",
            v_month,
            v_year,
            v_json,
            p_closure_source
        )
    ) THEN
        RETURN FALSE;
    END IF;

    ------------------------------------------------------------------
    -- Update closure request only if applicable
    ------------------------------------------------------------------

    IF p_closure_source = 'REQUEST_APPROVED' THEN

        UPDATE loan_closure_requests
        SET calculated_amount = v_closure_amount
        WHERE loan_id = p_loan_id
          AND "approvalId" IN (
                SELECT "approvalId"
                FROM approval_requests
                WHERE "entityId" = p_loan_id
                  AND "entityType" = 'LOAN_CLOSURE'
                  AND "approvalStatus" = 'APPROVED'
                ORDER BY "approvalId" DESC
                LIMIT 1
          );

    END IF;

    RETURN TRUE;

END;
$$;