CREATE OR REPLACE FUNCTION get_per_member_expected_maturity_amount(p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$

SELECT COALESCE(
    (
        SELECT
            CASE
                WHEN COUNT(DISTINCT fm."userId") > 0
                THEN fd."totalExpectedMaturityAmount" / COUNT(DISTINCT fm."userId")
                ELSE 0
            END
        FROM fund_details fd
        LEFT JOIN fund_members fm ON fm."fundId" = fd."fundId"
        WHERE fd."fundId" = p_fund_id
        GROUP BY fd."totalExpectedMaturityAmount"
    ),
    0
);
$$;