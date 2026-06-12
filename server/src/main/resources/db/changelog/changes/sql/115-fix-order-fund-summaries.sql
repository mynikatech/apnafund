DROP FUNCTION IF EXISTS get_monthly_financial_summary(INT, INT, INT);
DROP FUNCTION IF EXISTS get_fund_member_financial_summary(INT);

CREATE OR REPLACE FUNCTION get_monthly_financial_summary(
    p_fund_id INT,
    p_month INT,
    p_year INT
)
RETURNS TABLE (

    "userId" INT,

    "firstName" TEXT,

    "lastName" TEXT,

    "depositAmount" DOUBLE PRECISION,

    "latestDepositDate" TEXT,

    "loanIssuedAmount" DOUBLE PRECISION,

    "loanPrepaymentAmount" DOUBLE PRECISION,

    "interestPaidAmount" DOUBLE PRECISION,

    "latestEmiPaymentDate" TEXT,

    "feesPaidAmount" DOUBLE PRECISION

)
LANGUAGE sql
STABLE
AS
$$

SELECT

    u."userId",

    u."firstName",

    u."lastName",

    d.total_deposit
        AS "depositAmount",

    d.latest_deposit_date
        AS "latestDepositDate",

    l.total_loan_issued
        AS "loanIssuedAmount",

    le.total_prepayment
        AS "loanPrepaymentAmount",

    le.total_interest_paid
        AS "interestPaidAmount",

    le.latest_emi_payment_date
        AS "latestEmiPaymentDate",

    (
        COALESCE(d.deposit_late_fee, 0)
        +
        COALESCE(le.emi_late_fee, 0)
    ) AS "feesPaidAmount"

FROM fund_members fm

INNER JOIN users u
    ON u."userId" = fm."userId"

LEFT JOIN (

    SELECT

        d."depositorId" AS "userId",

        d."fundId",

        d."depositAmount"
            AS total_deposit,

        d."depositedDate"
            AS latest_deposit_date,

        COALESCE(d."lateFee", 0)
            AS deposit_late_fee

    FROM deposits d

    WHERE
        d."depositMonth" = p_month::TEXT
        AND d."depositYear" = p_year::TEXT

) d
    ON d."userId" = u."userId"
    AND d."fundId" = fm."fundId"

LEFT JOIN (

    SELECT

        l."borrowerId",

        SUM(l."loanAmount")
            AS total_loan_issued

    FROM loans l

    WHERE
        EXTRACT(
            MONTH FROM
            TO_DATE(l."issuedDate", 'DD/MM/YYYY')
        ) = p_month

        AND EXTRACT(
            YEAR FROM
            TO_DATE(l."issuedDate", 'DD/MM/YYYY')
        ) = p_year

    GROUP BY l."borrowerId"

) l
    ON l."borrowerId" = u."userId"

LEFT JOIN (

    SELECT

        lo."borrowerId",

        SUM(
            COALESCE(le."emiDepositedAmount", 0)
        ) AS total_interest_paid,

        SUM(
            COALESCE(le."prepaymentAmount", 0)
        ) AS total_prepayment,

        MAX(le."emiDepositedDate")
            AS latest_emi_payment_date,

        SUM(
            COALESCE(le."lateFee", 0)
        ) AS emi_late_fee

    FROM loan_emi le

    INNER JOIN loans lo
        ON lo."loanId" = le."loanId"

    WHERE
        le."emiMonth" = p_month::TEXT
        AND le."emiYear" = p_year::TEXT

    GROUP BY lo."borrowerId"

) le
    ON le."borrowerId" = u."userId"

WHERE
    fm."fundId" = p_fund_id
    AND fm."status" = 'ACTIVE'

ORDER BY
    CASE fm."role"::text
        WHEN 'PRIMARY_MODERATOR' THEN 1
        WHEN 'MODERATOR' THEN 2
        ELSE 3
    END,
    LOWER(u."firstName"),
    LOWER(u."lastName");

$$;



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
    CASE fm."role"::text
        WHEN 'PRIMARY_MODERATOR' THEN 1
        WHEN 'MODERATOR' THEN 2
        ELSE 3
    END,
    LOWER(u."firstName"),
    LOWER(u."lastName");
$$;