DROP FUNCTION IF EXISTS get_funds();

DROP FUNCTION IF EXISTS get_fund(INT);
DROP FUNCTION IF EXISTS add_fund_with_details(JSONB, JSONB);

DROP FUNCTION IF EXISTS get_all_funds_with_details();

DROP FUNCTION IF EXISTS get_all_funds_with_details_for_group(INT);

DROP FUNCTION IF EXISTS get_fund_with_details(INT);

DROP FUNCTION IF EXISTS get_funds_for_user(INT);

DROP FUNCTION IF EXISTS get_funds_for_user_for_group(INT, INT);
DROP FUNCTION IF EXISTS add_fund(TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, INT, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, INT, INT, TEXT);

DROP FUNCTION IF EXISTS add_fund(TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, INT, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, BOOLEAN, DOUBLE PRECISION, INTEGER, DOUBLE PRECISION, INT, INT, TEXT);
DROP FUNCTION IF EXISTS update_fund(INT, TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, INT, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, DOUBLE PRECISION, INT, INT, TEXT);
DROP FUNCTION IF EXISTS update_fund(INT, TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, INT, DOUBLE PRECISION, TEXT, DOUBLE PRECISION, BOOLEAN, DOUBLE PRECISION, INTEGER, DOUBLE PRECISION, INT, INT, TEXT);

CREATE OR REPLACE FUNCTION get_funds()
RETURNS TABLE (
  "fundId" int,
  "fundName" text,
  "fundStartDate" text,
  "fundMaturityDate" text,
  "fundPeriod" double precision,
  "depositionFrequency" text,
  "moderator" int,
  "recurringDepositAmount" double precision,
  "fundStatus" text,
  "loanInterestRate" double precision,
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
  "lateFeeRate" double precision,
  "monthlyDepDateBy" int,
  "groupId" int,
  "fundCode" text
) LANGUAGE sql AS $$
  SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
         f."fundPeriod", f."depositionFrequency", f."moderator",
         f."recurringDepositAmount", f."fundStatus", f."loanInterestRate",
         f."hasVariableInterestRate", f."revisedLoanInterestRate",
         f."interestRateRevisionAfterMonths",
         f."lateFeeRate", f."monthlyDepDateBy", f."groupId", f."fundCode"
    FROM "funds" f
   ORDER BY f."fundId";
$$;

CREATE OR REPLACE FUNCTION get_fund(p_fund_id int)
RETURNS TABLE (
  "fundId" int,
  "fundName" text,
  "fundStartDate" text,
  "fundMaturityDate" text,
  "fundPeriod" double precision,
  "depositionFrequency" text,
  "moderator" int,
  "recurringDepositAmount" double precision,
  "fundStatus" text,
  "loanInterestRate" double precision,
  "hasVariableInterestRate" boolean,
  "revisedLoanInterestRate" double precision,
  "interestRateRevisionAfterMonths" int,
  "lateFeeRate" double precision,
  "monthlyDepDateBy" int,
  "groupId" int,
  "fundCode" text
) LANGUAGE sql AS $$
  SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
         f."fundPeriod", f."depositionFrequency", f."moderator",
         f."recurringDepositAmount", f."fundStatus", f."loanInterestRate",
         f."hasVariableInterestRate", f."revisedLoanInterestRate",
         f."interestRateRevisionAfterMonths",
         f."lateFeeRate", f."monthlyDepDateBy", f."groupId", f."fundCode"
    FROM "funds" f
   WHERE f."fundId" = p_fund_id
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION add_fund(
  p_fundName text, p_fundStartDate text, p_fundMaturityDate text, p_fundPeriod double precision,
  p_depositionFrequency text, p_moderator int, p_recurringDepositAmount double precision,
  p_fundStatus text, p_loanInterestRate double precision,
  p_hasVariableInterestRate boolean, p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int,
  p_lateFeeRate double precision,
  p_monthlyDepDateBy int, p_groupId int, p_fundCode text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO "funds"(
    "fundName","fundStartDate","fundMaturityDate","fundPeriod",
    "depositionFrequency","moderator","recurringDepositAmount",
    "fundStatus","loanInterestRate","hasVariableInterestRate",
    "revisedLoanInterestRate","interestRateRevisionAfterMonths",
    "lateFeeRate","monthlyDepDateBy","groupId","fundCode"
  ) VALUES (
    p_fundName,p_fundStartDate,p_fundMaturityDate,p_fundPeriod,
    p_depositionFrequency,p_moderator,p_recurringDepositAmount,
    COALESCE(p_fundStatus,'ACTIVE'),p_loanInterestRate,
    COALESCE(p_hasVariableInterestRate,FALSE),
    p_revisedLoanInterestRate,p_interestRateRevisionAfterMonths,
    p_lateFeeRate,p_monthlyDepDateBy,p_groupId,p_fundCode
  )
  RETURNING "fundId" INTO v_id;

  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_fund(
  p_fundId int,
  p_fundName text, p_fundStartDate text, p_fundMaturityDate text, p_fundPeriod double precision,
  p_depositionFrequency text, p_moderator int, p_recurringDepositAmount double precision,
  p_fundStatus text, p_loanInterestRate double precision,
  p_hasVariableInterestRate boolean, p_revisedLoanInterestRate double precision,
  p_interestRateRevisionAfterMonths int,
  p_lateFeeRate double precision,
  p_monthlyDepDateBy int, p_groupId int, p_fundCode text
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE "funds"
     SET "fundName"                        = COALESCE(p_fundName, "fundName"),
         "fundStartDate"                   = COALESCE(p_fundStartDate, "fundStartDate"),
         "fundMaturityDate"                = COALESCE(p_fundMaturityDate, "fundMaturityDate"),
         "fundPeriod"                      = COALESCE(p_fundPeriod, "fundPeriod"),
         "depositionFrequency"             = COALESCE(p_depositionFrequency, "depositionFrequency"),
         "moderator"                       = COALESCE(p_moderator, "moderator"),
         "recurringDepositAmount"          = COALESCE(p_recurringDepositAmount, "recurringDepositAmount"),
         "fundStatus"                      = COALESCE(p_fundStatus, "fundStatus"),
         "loanInterestRate"                = COALESCE(p_loanInterestRate, "loanInterestRate"),
         "hasVariableInterestRate"         = COALESCE(p_hasVariableInterestRate, "hasVariableInterestRate"),
         "revisedLoanInterestRate"         = COALESCE(p_revisedLoanInterestRate, "revisedLoanInterestRate"),
         "interestRateRevisionAfterMonths" = COALESCE(p_interestRateRevisionAfterMonths, "interestRateRevisionAfterMonths"),
         "lateFeeRate"                     = COALESCE(p_lateFeeRate, "lateFeeRate"),
         "monthlyDepDateBy"                = COALESCE(p_monthlyDepDateBy, "monthlyDepDateBy"),
         "groupId"                         = COALESCE(p_groupId, "groupId"),
         "fundCode"                        = COALESCE(p_fundCode, "fundCode")
   WHERE "fundId" = p_fundId;

  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION get_all_funds_with_details()
  RETURNS TABLE (
    "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
    "fundPeriod" double precision, "depositionFrequency" text, "moderator" int,
    "recurringDepositAmount" double precision, "fundStatus" text, "loanInterestRate" double precision,
    "hasVariableInterestRate" boolean, "revisedLoanInterestRate" double precision, "interestRateRevisionAfterMonths" int,
    "lateFeeRate" double precision, "monthlyDepDateBy" int, "groupId" int, "fundCode" text,
    "fundDetailsId" int, "totalExpectedDeposit" double precision, "totalCurrentDeposit" double precision,
    "totalCurrentLateFee" double precision, "totalCurrentInterestCollected" double precision,
    "totalExpectedMaturityAmount" double precision, "totalCurrAmount" double precision,
    "moderatorFirstName" text, "moderatorLastName" text,
    "groupName" text
  )
  LANGUAGE sql AS $$
    SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
           f."fundPeriod", f."depositionFrequency", f."moderator",
           f."recurringDepositAmount", f."fundStatus", f."loanInterestRate",
           f."hasVariableInterestRate", f."revisedLoanInterestRate", f."interestRateRevisionAfterMonths",
           f."lateFeeRate", f."monthlyDepDateBy", f."groupId", f."fundCode",
           d."fundDetailsId", d."totalExpectedDeposit", d."totalCurrentDeposit",
           d."totalCurrentLateFee", d."totalCurrentInterestCollected",
           d."totalExpectedMaturityAmount", d."totalCurrAmount",
           u."firstName" as moderatorFirstName, u."lastName" as moderatorLastName,
           g."groupName"
      FROM "funds" f
      INNER JOIN fund_details d ON d."fundId" = f."fundId"
      LEFT JOIN groups g ON f."groupId" = g."groupId"
      LEFT JOIN users u ON u."userId" = f."moderator"
     ORDER BY f."fundId";
  $$;

CREATE OR REPLACE FUNCTION get_all_funds_with_details_for_group(p_group_id int)
RETURNS TABLE (
  "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
  "fundPeriod" double precision, "depositionFrequency" text, "moderator" int,
  "recurringDepositAmount" double precision, "fundStatus" text, "loanInterestRate" double precision,
  "hasVariableInterestRate" boolean, "revisedLoanInterestRate" double precision, "interestRateRevisionAfterMonths" int,
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
 CREATE OR REPLACE FUNCTION get_fund_with_details(p_fund_id int)
 RETURNS TABLE (
   "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
   "fundPeriod" double precision, "depositionFrequency" text, "moderator" int,
   "recurringDepositAmount" double precision, "fundStatus" text, "loanInterestRate" double precision,
   "hasVariableInterestRate" boolean, "revisedLoanInterestRate" double precision, "interestRateRevisionAfterMonths" int,
   "lateFeeRate" double precision, "monthlyDepDateBy" int, "groupId" int, "fundCode" text,
   "fundDetailsId" int, "totalExpectedDeposit" double precision, "totalCurrentDeposit" double precision,
   "totalCurrentLateFee" double precision, "totalCurrentInterestCollected" double precision,
   "totalExpectedMaturityAmount" double precision, "totalCurrAmount" double precision,
   "moderatorFirstName" text, "moderatorLastName" text, "groupName" text
 )
 LANGUAGE sql AS $$
   SELECT *
     FROM get_all_funds_with_details() s
    WHERE s."fundId" = p_fund_id;
 $$;

 CREATE OR REPLACE FUNCTION add_fund_with_details(p_fund jsonb, p_details jsonb)
  RETURNS int
  LANGUAGE plpgsql
  AS $$
  DECLARE
    v_fund_id int;
  BEGIN
    -- Insert into funds
    INSERT INTO "funds" (
      "fundName",
      "fundStartDate",
      "fundMaturityDate",
      "fundPeriod",
      "depositionFrequency",
      "moderator",
      "recurringDepositAmount",
      "fundStatus",
      "loanInterestRate",
      "hasVariableInterestRate",
      "revisedLoanInterestRate",
      "interestRateRevisionAfterMonths",
      "lateFeeRate",
      "monthlyDepDateBy",
      "groupId",
      "fundCode"
    ) VALUES (
      p_fund->>'fundName',
      p_fund->>'fundStartDate',
      p_fund->>'fundMaturityDate',
      (p_fund->>'fundPeriod')::double precision,
      p_fund->>'depositionFrequency',
      (p_fund->>'moderator')::int,
      (p_fund->>'recurringDepositAmount')::double precision,
      COALESCE(NULLIF(p_fund->>'fundStatus',''), 'ACTIVE'),
      (p_fund->>'loanInterestRate')::double precision,
      COALESCE(
          NULLIF(p_fund->>'hasVariableInterestRate','')::boolean,
          FALSE
      ),
      NULLIF(
          p_fund->>'revisedLoanInterestRate',
          ''
      )::double precision,
      NULLIF(
          p_fund->>'interestRateRevisionAfterMonths',
          ''
      )::int,
      (p_fund->>'lateFeeRate')::double precision,
      (p_fund->>'monthlyDepDateBy')::int,
      (p_fund->>'groupId')::int,
      p_fund->>'fundCode'
    )
    RETURNING "fundId" INTO v_fund_id;

    INSERT INTO fund_details (
      "fundId",
      "totalExpectedDeposit",
      "totalCurrentDeposit",
      "totalCurrentLateFee",
      "totalCurrentInterestCollected",
      "totalExpectedMaturityAmount",
      "totalCurrAmount"
    ) VALUES (
      v_fund_id,
      COALESCE(NULLIF(p_details->>'totalExpectedDeposit','')::double precision, 0),
      COALESCE(NULLIF(p_details->>'totalCurrentDeposit','')::double precision, 0),
      COALESCE(NULLIF(p_details->>'totalCurrentLateFee','')::double precision, 0),
      COALESCE(NULLIF(p_details->>'totalCurrentInterestCollected','')::double precision, 0),
      COALESCE(NULLIF(p_details->>'totalExpectedMaturityAmount','')::double precision, 0),
      COALESCE(NULLIF(p_details->>'totalCurrAmount','')::double precision, 0)
    );

    RETURN v_fund_id;
  END
  $$;


  CREATE OR REPLACE FUNCTION get_funds_for_user(p_user_id int)
  RETURNS TABLE (
    "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
    "fundPeriod" int, "depositionFrequency" text, "moderator" int,
    "recurringDepositAmount" double precision, "fundStatus" text,
    "loanInterestRate" double precision, "hasVariableInterestRate" boolean,
     "revisedLoanInterestRate" double precision, "interestRateRevisionAfterMonths" int,
    "lateFeeRate" double precision, "monthlyDepDateBy" int, "groupId" int, "fundCode" text
  ) LANGUAGE sql AS $$
    SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
           f."fundPeriod", f."depositionFrequency", f."moderator",
           f."recurringDepositAmount", f."fundStatus",
           f."loanInterestRate", f."hasVariableInterestRate",
           f."revisedLoanInterestRate", f."interestRateRevisionAfterMonths",f."lateFeeRate",
           f."monthlyDepDateBy", f."groupId", f."fundCode"
      FROM fund_members fm
      JOIN funds f ON f."fundId" = fm."fundId"
     WHERE fm."userId" = p_user_id
     ORDER BY f."fundId";
  $$;


  CREATE OR REPLACE FUNCTION get_funds_for_user_for_group(p_user_id int, p_group_id int)
  RETURNS TABLE (
    "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
    "fundPeriod" int, "depositionFrequency" text, "moderator" int,
    "recurringDepositAmount" double precision, "fundStatus" text,
    "loanInterestRate" double precision,"hasVariableInterestRate" boolean,
    "revisedLoanInterestRate" double precision, "interestRateRevisionAfterMonths" int,"lateFeeRate" double precision,
    "monthlyDepDateBy" int, "groupId" int, "fundCode" text
  ) LANGUAGE sql AS $$
    SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
           f."fundPeriod", f."depositionFrequency", f."moderator",
           f."recurringDepositAmount", f."fundStatus",
           f."loanInterestRate", f."hasVariableInterestRate",
           f."revisedLoanInterestRate", f."interestRateRevisionAfterMonths",f."lateFeeRate",
           f."monthlyDepDateBy", f."groupId", f."fundCode"
      FROM fund_members fm
      JOIN funds f ON f."fundId" = fm."fundId"
     WHERE fm."userId" = p_user_id
     AND f."groupId" = p_group_id
     ORDER BY f."fundId";
  $$;

