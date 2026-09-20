DROP FUNCTION IF EXISTS delete_loan(INT);

CREATE OR REPLACE FUNCTION delete_loan(
    p_loan_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    -- Loan must exist
    IF NOT EXISTS (
        SELECT 1
        FROM loans
        WHERE "loanId" = p_loan_id
    ) THEN
        RETURN FALSE;
    END IF;

    -- Loan must still be OPEN
    IF EXISTS (
        SELECT 1
        FROM loans
        WHERE "loanId" = p_loan_id
          AND "status" = 'CLOSED'
    ) THEN
        RETURN FALSE;
    END IF;

    -- No EMI should exist
    IF EXISTS (
        SELECT 1
        FROM loan_emi
        WHERE "loanId" = p_loan_id
    ) THEN
        RETURN FALSE;
    END IF;

    -- Delete loan details
    DELETE FROM loan_details
    WHERE "loanId" = p_loan_id;

    -- Delete loan
    DELETE FROM loans
    WHERE "loanId" = p_loan_id;

    RETURN TRUE;

END;
$$;