CREATE OR REPLACE FUNCTION approve_loan_request(
    p_approval_id INT,
    p_approved_by INT,
    p_reason TEXT DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_loan_id INT;
BEGIN

    SELECT "entityId"
    INTO v_loan_id
    FROM approval_requests
    WHERE "approvalId" = p_approval_id
      AND "approvalStatus" = 'PENDING'
      AND "approverUserId" = p_approved_by;

    IF v_loan_id IS NULL THEN
        RETURN FALSE;
    END IF;

    -- mark approval request
    UPDATE approval_requests
    SET
        "approvalStatus" = 'APPROVED',
        "decisionReason" = p_reason,
        "decidedAt" = NOW()
    WHERE "approvalId" = p_approval_id;

    -- activate loan
    UPDATE loans
    SET "workflowStatus" = 'APPROVED'
    WHERE "loanId" = v_loan_id;

    RETURN TRUE;

END;
$$;

CREATE OR REPLACE FUNCTION reject_loan_request(
    p_approval_id INT,
    p_rejected_by INT,
    p_reason TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_loan_id INT;
BEGIN

    SELECT "entityId"
    INTO v_loan_id
    FROM approval_requests
    WHERE "approvalId" = p_approval_id
      AND "approvalStatus" = 'PENDING'
      AND "approverUserId" = p_rejected_by;

    IF v_loan_id IS NULL THEN
        RETURN FALSE;
    END IF;

    UPDATE approval_requests
    SET
        "approvalStatus" = 'REJECTED',
        "decisionReason" = p_reason,
        "decidedAt" = NOW()
    WHERE "approvalId" = p_approval_id;

    UPDATE loans
    SET "workflowStatus" = 'REJECTED'
    WHERE "loanId" = v_loan_id;

    RETURN TRUE;

END;
$$;