CREATE OR REPLACE FUNCTION get_all_funds_with_details_for_group(p_group_id int)
RETURNS TABLE (
  "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
  "fundPeriod" double precision, "depositionFrequency" text, "moderator" int,
  "recurringDepositAmount" double precision, "fundStatus" text, "loanInterestRate" double precision,
  "lateFeeRate" double precision, "monthlyDepDateBy" int, "groupId" int, "fundCode" text,
  "fundDetailsId" int, "totalExpectedDeposit" double precision, "totalCurrentDeposit" double precision,
  "totalCurrentLateFee" double precision, "totalCurrentInterestCollected" double precision,
  "totalExpectedMaturityAmount" double precision, "totalCurrAmount" double precision,
  "moderatorFirstName" text, "moderatorLastName" text, "groupName" text
)
LANGUAGE sql AS $$
  SELECT *
    FROM get_all_funds_with_details() s
   WHERE s."groupId" = p_group_id
   ORDER BY
       CASE
           WHEN s."fundStatus" = 'ACTIVE' THEN 0
           WHEN s."fundStatus" = 'INACTIVE' THEN 1
           WHEN s."fundStatus" = 'CLOSED' THEN 2
           ELSE 3
       END,
       s."fundStartDate" DESC;
 $$;
