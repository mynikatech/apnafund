DROP FUNCTION IF EXISTS get_fund_member_financial_summary(INT);


CREATE OR REPLACE FUNCTION get_fund_member_financial_summary(
    p_fund_id INT
)
RETURNS TABLE (
    "userId" INT,
    "firstName" TEXT,
    "lastName" TEXT,
    "totalDeposit" DOUBLE PRECISION,
    "totalLoanAmount" DOUBLE PRECISION,
    "totalOutstandingAmount" DOUBLE PRECISION,
    "totalCurrIntPaid" DOUBLE PRECISION,
    "expectedMatAmount" DOUBLE PRECISION
)
LANGUAGE sql
STABLE
AS $$
    WITH dep AS (
        SELECT
            d."depositorId" AS "userId",
            COALESCE(SUM(d."depositAmount"), 0)::double precision AS total_dep
        FROM deposits d
        WHERE d."fundId" = p_fund_id
        GROUP BY d."depositorId"
    ),

    loan AS (
        SELECT
            l."borrowerId" AS "userId",
            COALESCE(SUM(l."loanAmount"), 0)::double precision AS total_loan
        FROM loans l
        WHERE l."fundId" = p_fund_id
        GROUP BY l."borrowerId"
    ),

    loan_summary AS (
        SELECT
            l."borrowerId" AS "userId",

            COALESCE(
                SUM(
                    ld."currPrincipal"
                    + (ld."totalInterest" - ld."currTotalIntPaid")
                ),
                0
            )::double precision AS outstanding_amt,

            COALESCE(
                SUM(ld."currTotalIntPaid"),
                0
            )::double precision AS int_paid

        FROM loan_details ld
        JOIN loans l
            ON l."loanId" = ld."loanId"

        WHERE l."fundId" = p_fund_id

        GROUP BY l."borrowerId"
    )

    SELECT
        u."userId",
        u."firstName",
        u."lastName",

        COALESCE(dep.total_dep, 0) AS "totalDeposit",

        COALESCE(loan.total_loan, 0) AS "totalLoanAmount",

        COALESCE(loan_summary.outstanding_amt, 0)
            AS "totalOutstandingAmount",

        COALESCE(loan_summary.int_paid, 0)
            AS "totalCurrIntPaid",

        get_per_member_expected_maturity_amount(p_fund_id)::double precision
            AS "expectedMatAmount"

    FROM "fund_members" fm

    INNER JOIN users u
        ON u."userId" = fm."userId"

    LEFT JOIN dep
        ON dep."userId" = fm."userId"

    LEFT JOIN loan
        ON loan."userId" = fm."userId"

    LEFT JOIN loan_summary
        ON loan_summary."userId" = fm."userId"

    WHERE fm."fundId" = p_fund_id
      AND fm."status" = 'ACTIVE'::"memberStatus"

    ORDER BY
        u."firstName",
        u."lastName";
$$;