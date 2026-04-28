
CREATE OR REPLACE FUNCTION create_loan_closure_approval(
    p_loan_id INT,
    p_requested_by INT,
    p_approver INT,
    p_closure_type TEXT,
    p_requested_amount NUMERIC,
    p_remarks TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_approval_id INT;
BEGIN

    -- Insert into approval_requests
    INSERT INTO approval_requests (
        "entityType",
        "entityId",
        "requestedBy",
        "approverUserId",
        "approvalStatus"
    )
    VALUES (
        'LOAN_CLOSURE',
        p_loan_id,
        p_requested_by,
        p_approver,
        'PENDING'
    )
    RETURNING "approvalId" INTO v_approval_id;

    -- Insert into loan_closure_requests
    INSERT INTO loan_closure_requests (
        "approvalId",
        loan_id,
        closure_type,
        requested_amount,
        remarks
    )
    VALUES (
        v_approval_id,
        p_loan_id,
        p_closure_type,
        p_requested_amount,
        p_remarks
    );

    RETURN TRUE;
END;
$$;

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

    -- Calculate closure amount
    v_calculated_amount :=
        COALESCE(v_details."currPrincipal", 0)
        + COALESCE(v_details."totalInterest", 0)
        - COALESCE(v_details."currTotalIntPaid", 0);

    -- Settle loan_details
    UPDATE loan_details
    SET
        "currPrincipal" = 0,
        "currTotalIntPaid" = v_details."totalInterest"
    WHERE "loanId" = p_loan_id;

    -- Close loan
    UPDATE loans
    SET
        "status" = 'CLOSED',
        "closedDate" = TO_CHAR(NOW(), 'DD/MM/YYYY'),
        "closureType" = 'FORECLOSURE',
        "closureSource" = 'APPROVAL'
    WHERE "loanId" = p_loan_id;

    -- Update closure request
    UPDATE loan_closure_requests
    SET "calculatedAmount" = v_calculated_amount
    WHERE "loanId" = p_loan_id
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