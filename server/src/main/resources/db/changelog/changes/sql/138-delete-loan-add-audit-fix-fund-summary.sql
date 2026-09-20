DROP FUNCTION IF EXISTS delete_loan(INT);
DROP FUNCTION IF EXISTS update_fund_details(INT);
DROP FUNCTION IF EXISTS insert_audit_record(TEXT, INT, TEXT, INT, TEXT, JSONB);
DROP FUNCTION IF EXISTS create_auto_approved_loan_closure(INT, INT);

CREATE TABLE IF NOT EXISTS audit_records (
    "auditId"           SERIAL PRIMARY KEY,
    "entityType"        TEXT NOT NULL,
    "entityId"          INTEGER NOT NULL,
    "operation"         TEXT NOT NULL,
    "performedByUserId" INTEGER,
    "performedOn"       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "reason"            TEXT,
    "data"              JSONB NOT NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_audit_entity
ON audit_records ("entityType", "entityId");

CREATE INDEX IF NOT EXISTS idx_audit_operation
ON audit_records ("operation");

CREATE INDEX IF NOT EXISTS idx_audit_performed_on
ON audit_records ("performedOn");

CREATE INDEX IF NOT EXISTS idx_audit_performed_by
ON audit_records ("performedByUserId");


CREATE OR REPLACE FUNCTION update_fund_details(
    p_fund_id INT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_expected_loan_interest     DOUBLE PRECISION;
    v_current_interest           DOUBLE PRECISION;
    v_current_loan_late_fee      DOUBLE PRECISION;
    v_current_deposit_late_fee   DOUBLE PRECISION;
    v_current_total_late_fee     DOUBLE PRECISION;
BEGIN

    -----------------------------------------------------------------------
    -- Expected loan interest
    -----------------------------------------------------------------------
    SELECT COALESCE(SUM(ld."totalInterest"), 0)
      INTO v_expected_loan_interest
      FROM loan_details ld
      JOIN loans l
        ON l."loanId" = ld."loanId"
     WHERE l."fundId" = p_fund_id;

    -----------------------------------------------------------------------
    -- Interest collected till date
    -----------------------------------------------------------------------
    SELECT COALESCE(SUM(ld."currTotalIntPaid"), 0)
      INTO v_current_interest
      FROM loan_details ld
      JOIN loans l
        ON l."loanId" = ld."loanId"
     WHERE l."fundId" = p_fund_id;

    -----------------------------------------------------------------------
    -- Loan EMI late fees collected
    -----------------------------------------------------------------------
    SELECT COALESCE(SUM(le."lateFee"), 0)
      INTO v_current_loan_late_fee
      FROM loan_emi le
      JOIN loans l
        ON l."loanId" = le."loanId"
     WHERE l."fundId" = p_fund_id;

    -----------------------------------------------------------------------
    -- Deposit late fees collected
    -----------------------------------------------------------------------
    SELECT COALESCE(SUM(d."lateFee"), 0)
      INTO v_current_deposit_late_fee
      FROM deposits d
     WHERE d."fundId" = p_fund_id;

    -----------------------------------------------------------------------
    -- Total late fees
    -----------------------------------------------------------------------
    v_current_total_late_fee :=
        v_current_deposit_late_fee +
        v_current_loan_late_fee;

    -----------------------------------------------------------------------
    -- Update summary
    -----------------------------------------------------------------------
    UPDATE fund_details
       SET
           "totalCurrentInterestCollected" =
               v_current_interest,

           "totalCurrentLateFee" =
               v_current_total_late_fee,

           "totalExpectedMaturityAmount" =
                 COALESCE("totalExpectedDeposit", 0)
               + v_expected_loan_interest
               + v_current_total_late_fee,

           "totalCurrAmount" =
                 COALESCE("totalCurrentDeposit", 0)
               + v_current_interest
               + v_current_total_late_fee
     WHERE "fundId" = p_fund_id;

END;
$$;

CREATE OR REPLACE FUNCTION delete_loan(
    p_loan_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_fund_id INT;
BEGIN

    -- Loan must exist
    SELECT "fundId"
      INTO v_fund_id
      FROM loans
     WHERE "loanId" = p_loan_id;

    IF NOT FOUND THEN
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

    PERFORM insert_audit_record(
        'LOAN',
        p_loan_id,
        'DELETE',
        NULL, -- or p_performed_by_user_id if you pass it
        'Loan deleted',
        (
            SELECT row_to_json(t)::jsonb
            FROM (
                SELECT l.*, ld.*
                FROM loans l
                LEFT JOIN loan_details ld
                  ON ld."loanId" = l."loanId"
                WHERE l."loanId" = p_loan_id
            ) t
        )
    );

    -- Delete loan details
    DELETE FROM loan_details
     WHERE "loanId" = p_loan_id;

    -- Delete loan
    DELETE FROM loans
     WHERE "loanId" = p_loan_id;

    -- Recalculate fund/member summaries
    PERFORM update_fund_details(v_fund_id);

    RETURN TRUE;

END;
$$;


CREATE OR REPLACE FUNCTION insert_audit_record(
    p_entity_type          TEXT,
    p_entity_id            INTEGER,
    p_operation            TEXT,
    p_performed_by_user_id INTEGER,
    p_reason               TEXT,
    p_data                 JSONB
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO audit_records (
        "entityType",
        "entityId",
        "operation",
        "performedByUserId",
        "reason",
        "data"
    )
    VALUES (
        p_entity_type,
        p_entity_id,
        p_operation,
        p_performed_by_user_id,
        p_reason,
        p_data
    );
END;
$$;


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

    -- Create approval (already approved)
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
        p_approved_by,
        p_approved_by,
        'APPROVED'
    )
    RETURNING "approvalId"
    INTO v_approval_id;

    -- Create closure request
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

    -- Audit
    PERFORM insert_audit_record(
        'LOAN_CLOSURE',
        p_loan_id,
        'AUTO_APPROVED',
        p_approved_by,
        'Loan closure auto-approved by moderator',
        jsonb_build_object(
            'approvalId', v_approval_id,
            'closureType', 'FORECLOSURE'
        )
    );

    RETURN TRUE;

END;
$$;