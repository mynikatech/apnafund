CREATE OR REPLACE FUNCTION has_pending_loan_closure_request(
    p_loan_id INT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM approval_requests
        WHERE "entityId" = p_loan_id
          AND "entityType" = 'LOAN_CLOSURE'
          AND "approvalStatus" = 'PENDING'
    );
$$;