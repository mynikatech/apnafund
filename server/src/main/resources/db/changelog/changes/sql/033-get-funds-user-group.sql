-- All funds for a user
CREATE OR REPLACE FUNCTION get_funds_for_user_for_group(p_user_id int, p_group_id int)
RETURNS TABLE (
  "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
  "fundPeriod" int, "depositionFrequency" text, "moderator" int,
  "recurringDepositAmount" double precision, "fundStatus" text,
  "loanInterestRate" double precision, "lateFeeRate" double precision,
  "monthlyDepDateBy" int, "groupId" int, "fundCode" text
) LANGUAGE sql AS $$
  SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
         f."fundPeriod", f."depositionFrequency", f."moderator",
         f."recurringDepositAmount", f."fundStatus",
         f."loanInterestRate", f."lateFeeRate",
         f."monthlyDepDateBy", f."groupId", f."fundCode"
    FROM fund_members fm
    JOIN funds f ON f."fundId" = fm."fundId"
   WHERE fm."userId" = p_user_id
   AND f."groupId" = p_group_id
   ORDER BY f."fundId";
$$;