DROP FUNCTION IF EXISTS create_loan_closure_approval(INT,INT,INT,TEXT, NUMERIC,TEXT);
DROP FUNCTION IF EXISTS create_loan_closure_approval(
    INT, INT, INT, TEXT, DOUBLE PRECISION, TEXT
);

CREATE OR REPLACE FUNCTION create_loan_closure_approval(
    p_loan_id INT,
    p_requested_by INT,
    p_approver INT,
    p_closure_type TEXT,
    p_requested_amount  DOUBLE PRECISION,
    p_remarks TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_approval_id INT;
BEGIN

        -- Prevent duplicate requests
        IF EXISTS (
            SELECT 1
            FROM approval_requests
            WHERE "entityId" = p_loan_id
              AND "entityType" = 'LOAN_CLOSURE'
              AND "approvalStatus" = 'PENDING'
        ) THEN
            RETURN FALSE;
        END IF;

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