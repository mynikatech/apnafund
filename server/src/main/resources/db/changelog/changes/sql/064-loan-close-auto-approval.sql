CREATE OR REPLACE FUNCTION create_auto_approved_loan_closure(
    p_loan_id INT,
    p_approved_by INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_approval_id INT;
BEGIN

    -- 1️⃣ Insert approval (already approved)
    INSERT INTO approval_requests (
        "entityType",
        "entityId",
        "requestedBy",
        "approverUserId",
        "approvalStatus",
        "approvedBy"
    )
    VALUES (
        'LOAN_CLOSURE',
        p_loan_id,
        p_approved_by,
        p_approved_by,
        'APPROVED',
        p_approved_by
    )
    RETURNING "approvalId" INTO v_approval_id;

    -- 2️⃣ Insert closure request (for consistency)
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
        'FORECLOSURE',
        NULL,
        'Auto-approved (moderator)'
    );

    RETURN TRUE;

END;
$$;