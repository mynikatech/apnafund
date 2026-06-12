DROP FUNCTION IF EXISTS recalculate_fund_financials(INT);

CREATE OR REPLACE FUNCTION recalculate_fund_financials(
    p_fund_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE

    v_active_members INT;
    v_recurring_deposit_amount DOUBLE PRECISION;
    v_fund_period DOUBLE PRECISION;
    v_total_expected_deposit DOUBLE PRECISION;
    v_total_expected_maturity DOUBLE PRECISION;

BEGIN

    -- Count active members
    SELECT COUNT(*)
    INTO v_active_members
    FROM fund_members
    WHERE "fundId" = p_fund_id
       AND "status" = 'ACTIVE'::"memberStatus";

    -- Fetch fund configuration
    SELECT
        "recurringDepositAmount",
        "fundPeriod"
    INTO
        v_recurring_deposit_amount,
        v_fund_period
    FROM funds
    WHERE "fundId" = p_fund_id;

    -- Recalculate totals
    v_total_expected_deposit :=
        COALESCE(v_active_members, 0)
        * COALESCE(v_recurring_deposit_amount, 0)
        * COALESCE(v_fund_period, 0);

    -- Adjust later if maturity logic changes
    v_total_expected_maturity :=
        v_total_expected_deposit;

    -- Update fund details
    UPDATE fund_details
    SET
        "totalExpectedDeposit" = v_total_expected_deposit,
        "totalExpectedMaturityAmount" = v_total_expected_maturity
    WHERE "fundId" = p_fund_id;

    RETURN TRUE;

EXCEPTION
WHEN OTHERS THEN

    RAISE NOTICE
        'Failed to recalculate fund details for fundId %',
        p_fund_id;

    RETURN FALSE;
END;
$$;