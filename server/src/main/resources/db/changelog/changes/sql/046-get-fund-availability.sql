CREATE OR REPLACE FUNCTION get_fund_availability(p_fund_id int)
RETURNS TABLE (
    total_amount DOUBLE PRECISION,
    approved_amount DOUBLE PRECISION,
    pending_amount DOUBLE PRECISION,
    available_amount DOUBLE PRECISION
)
LANGUAGE sql AS $$

SELECT
    -- Total funds
    COALESCE(fd."totalCurrentDeposit",0)
  + COALESCE(fd."totalCurrentLateFee",0)
  + COALESCE(fd."totalCurrentInterestCollected",0) AS total_amount,

    -- Approved loans
    COALESCE(SUM(
        CASE
            WHEN l."status" = 'ACTIVE'
             AND l."workflowStatus" = 'APPROVED'
            THEN ld."currPrincipal"
            ELSE 0
        END
    ),0) AS approved_amount,

    -- Pending loans (reserved)
    COALESCE(SUM(
        CASE
            WHEN l."status" = 'PENDING'
             AND l."workflowStatus" = 'PENDING_APPROVAL'
            THEN ld."currPrincipal"
            ELSE 0
        END
    ),0) AS pending_amount,

    -- Final available
    (
        COALESCE(fd."totalCurrentDeposit",0)
      + COALESCE(fd."totalCurrentLateFee",0)
      + COALESCE(fd."totalCurrentInterestCollected",0)
      - COALESCE(SUM(
            CASE
                WHEN l."status" = 'ACTIVE'
                 AND l."workflowStatus" = 'APPROVED'
                THEN ld."currPrincipal"
                ELSE 0
            END
        ),0)
      - COALESCE(SUM(
            CASE
                WHEN l."status" = 'PENDING'
                 AND l."workflowStatus" = 'PENDING_APPROVAL'
                THEN ld."currPrincipal"
                ELSE 0
            END
        ),0)
    ) AS available_amount

FROM fund_details fd
LEFT JOIN loans l ON l."fundId" = fd."fundId"
LEFT JOIN loan_details ld ON ld."loanId" = l."loanId"

WHERE fd."fundId" = p_fund_id

GROUP BY fd."totalCurrentDeposit",
         fd."totalCurrentLateFee",
         fd."totalCurrentInterestCollected";

$$;