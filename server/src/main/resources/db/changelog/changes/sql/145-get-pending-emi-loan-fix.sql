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
                        / 12
                    ) / 100,
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