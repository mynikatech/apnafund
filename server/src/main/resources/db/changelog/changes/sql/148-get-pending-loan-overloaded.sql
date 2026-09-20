DROP FUNCTION IF EXISTS get_pending_loan_emis_for_closure(INT, INT, INT);

CREATE OR REPLACE FUNCTION get_pending_loan_emis_for_closure(
    p_loan_id INT,
    p_month INT,
    p_year INT
)
RETURNS TABLE (
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
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM get_pending_loan_emis_for_closure(
        p_loan_id,
        (make_date(p_year, p_month, 1)
         + interval '1 month'
         - interval '1 day')::date
    );
END;
$$;