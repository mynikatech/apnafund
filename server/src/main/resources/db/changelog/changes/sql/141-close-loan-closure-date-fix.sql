DROP FUNCTION IF EXISTS close_loan(INT, INT, TEXT, TEXT);

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
    v_pending_emi RECORD;

    v_prepayment NUMERIC := 0;
    v_closure_amount NUMERIC := 0;

    v_month TEXT;
    v_year TEXT;
    v_date TEXT;
    v_closure_date DATE;


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
    WHERE "loanId" = p_loan_id
    ORDER BY "loanDetailsId" DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF COALESCE(v_details."currPrincipal",0) <= 0 THEN
        RETURN FALSE;
    END IF;

    ------------------------------------------------------------------
    -- Current Month
    ------------------------------------------------------------------

        IF p_closure_date ~ '^\d{4}-\d{2}-\d{2}$' THEN
            v_closure_date := p_closure_date::date;

        ELSIF p_closure_date ~ '^\d{2}/\d{2}/\d{4}$' THEN
            v_closure_date := to_date(p_closure_date,'DD/MM/YYYY');

        ELSE
            RAISE EXCEPTION 'Invalid closure date format: %', p_closure_date;
        END IF;

        v_date := TO_CHAR(v_closure_date, 'DD/MM/YYYY');   -- only for JSON

        v_month := EXTRACT(MONTH FROM v_closure_date)::INT::TEXT;
        v_year  := EXTRACT(YEAR FROM v_closure_date)::INT::TEXT;

        ------------------------------------------------------------------
        -- Post all pending monthly interest EMIs
        ------------------------------------------------------------------

        FOR v_pending_emi IN
            SELECT *
            FROM get_pending_loan_emis_for_closure(
                p_loan_id,
                v_closure_date
            )
        LOOP

            v_json :=
                jsonb_build_array(
                    jsonb_build_object(
                        'loanId', v_pending_emi."loanId",
                        'emiDepositedDate', v_pending_emi."emiDepositedDate",
                        'emiDepositedAmount', v_pending_emi."emiDepositedAmount",
                        'prepaymentAmount', v_pending_emi."prepaymentAmount",
                        'lateFee', v_pending_emi."lateFee"
                    )
                );

            PERFORM *
            FROM save_or_update_all_loan_emis_and_fetch(
                v_pending_emi."fundId",
                v_pending_emi."emiMonth",
                v_pending_emi."emiYear",
                v_json,
                p_closure_source
            );

        END LOOP;

        SELECT *
        INTO v_details
        FROM loan_details
        WHERE "loanId" = p_loan_id
        ORDER BY "loanDetailsId" DESC
        LIMIT 1;

        IF NOT FOUND THEN
            RETURN FALSE;
        END IF;
    ------------------------------------------------------------------
    -- Remaining principal
    ------------------------------------------------------------------

    v_prepayment := COALESCE(v_details."currPrincipal",0);

    IF v_prepayment <= 0 THEN
        RETURN FALSE;
    END IF;

    v_closure_amount := v_prepayment;

    ------------------------------------------------------------------
    -- Build synthetic EMI JSON
    ------------------------------------------------------------------

    v_json :=
        jsonb_build_array(
            jsonb_build_object(
                'loanId', p_loan_id,
                'emiDepositedDate', v_date,
                'emiDepositedAmount', 0,
                'prepaymentAmount', v_prepayment,
                'lateFee', 0
            )
        );

    ------------------------------------------------------------------
    -- Accounting Engine
    ------------------------------------------------------------------

    PERFORM 1
    FROM save_or_update_all_loan_emis_and_fetch(
        v_loan."fundId",
        v_month,
        v_year,
        v_json,
        p_closure_source
    );
    ------------------------------------------------------------------
    -- Verify Loan closed
    ------------------------------------------------------------------

    SELECT *
    INTO v_loan
    FROM loans
    WHERE "loanId" = p_loan_id;

    IF v_loan."status" <> 'CLOSED' THEN
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