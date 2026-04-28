CREATE OR REPLACE FUNCTION close_loan_by_approval(
    p_loan_id INT,
    p_approved_by INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_loan RECORD;
    v_details RECORD;
    v_calculated_amount NUMERIC;
    v_interest_saved NUMERIC;
    v_fd RECORD;
BEGIN

    -- Fetch loan
    SELECT * INTO v_loan
    FROM loans
    WHERE "loanId" = p_loan_id;

    IF v_loan IS NULL THEN
        RETURN FALSE;
    END IF;

    IF v_loan."status" = 'CLOSED' THEN
        RETURN FALSE;
    END IF;

    -- Fetch loan details
    SELECT * INTO v_details
    FROM loan_details
    WHERE "loanId" = p_loan_id;

    IF v_details IS NULL THEN
        RETURN FALSE;
    END IF;

    -- Calculate remaining interest (THIS IS THE KEY FIX)
    v_interest_saved :=
        COALESCE(v_details."totalInterest", 0)
        - COALESCE(v_details."currTotalIntPaid", 0);

    -- Calculate closure amount (what borrower pays)
    v_calculated_amount :=
        COALESCE(v_details."currPrincipal", 0)
        + COALESCE(v_details."currTotalIntPaid", 0);

    ------------------------------------------------------------------
    -- FUND IMPACT (same concept as EMI function)
    ------------------------------------------------------------------

    SELECT *
    INTO v_fd
    FROM fund_details
    WHERE "fundId" = v_loan."fundId"
    ORDER BY "fundDetailsId" DESC
    LIMIT 1;

    IF v_fd IS NOT NULL THEN

        -- Reduce expected maturity (interest that will NOT be earned)
        UPDATE fund_details
        SET "totalExpectedMaturityAmount" =
            GREATEST(0,
                COALESCE("totalExpectedMaturityAmount", 0)
                - v_interest_saved
            )
        WHERE "fundDetailsId" = v_fd."fundDetailsId";

    END IF;

    ------------------------------------------------------------------
    -- Update loan_details (IMPORTANT FIX)
    ------------------------------------------------------------------

    UPDATE loan_details
    SET
        "currPrincipal" = 0,
        -- only interest actually paid stays
        "totalInterest" = COALESCE(v_details."currTotalIntPaid", 0),
        "totalAmount" = COALESCE(v_details."origPrincipal", 0)
                        + COALESCE(v_details."currTotalIntPaid", 0)
    WHERE "loanId" = p_loan_id;

    ------------------------------------------------------------------
    -- Close loan
    ------------------------------------------------------------------

    UPDATE loans
    SET
        "status" = 'CLOSED',
        "closedDate" = TO_CHAR(NOW(), 'DD/MM/YYYY'),
        "closureType" = 'FORECLOSURE',
        "closureSource" = 'APPROVAL'
    WHERE "loanId" = p_loan_id;

    ------------------------------------------------------------------
    -- Update closure request
    ------------------------------------------------------------------

    UPDATE loan_closure_requests
    SET "calculatedAmount" = v_calculated_amount
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

    RETURN TRUE;

END;
$$;