CREATE OR REPLACE FUNCTION get_available_fund_amount(p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  SELECT
    COALESCE(fd."totalCurrentDeposit",0)
  + COALESCE(fd."totalCurrentLateFee",0)
  + COALESCE(fd."totalCurrentInterestCollected",0)
  - COALESCE(SUM(ld."currPrincipal"),0)
  FROM fund_details fd
  LEFT JOIN loans l ON l."fundId" = fd."fundId" AND l."status" = 'ACTIVE'
  AND l."workflowStatus" in ('PENDING_APPROVAL', 'APPROVED')
  LEFT JOIN loan_details ld ON ld."loanId" = l."loanId"
  WHERE fd."fundId" = p_fund_id
  GROUP BY fd."totalCurrentDeposit", fd."totalCurrentLateFee", fd."totalCurrentInterestCollected";
$$;