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
  "lateFeeRate" double precision,
  "monthlyDepDateBy" int,
  "groupId" int,
  "fundCode" text
) LANGUAGE sql AS $$
  SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
         f."fundPeriod", f."depositionFrequency", f."moderator",
         f."recurringDepositAmount", f."fundStatus", f."loanInterestRate",
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
  "lateFeeRate" double precision,
  "monthlyDepDateBy" int,
  "groupId" int,
  "fundCode" text
) LANGUAGE sql AS $$
  SELECT f."fundId", f."fundName", f."fundStartDate", f."fundMaturityDate",
         f."fundPeriod", f."depositionFrequency", f."moderator",
         f."recurringDepositAmount", f."fundStatus", f."loanInterestRate",
         f."lateFeeRate", f."monthlyDepDateBy", f."groupId", f."fundCode"
    FROM "funds" f
   WHERE f."fundId" = p_fund_id
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION add_fund(
  p_fundName text, p_fundStartDate text, p_fundMaturityDate text, p_fundPeriod double precision,
  p_depositionFrequency text, p_moderator int, p_recurringDepositAmount double precision,
  p_fundStatus text, p_loanInterestRate double precision, p_lateFeeRate double precision,
  p_monthlyDepDateBy int, p_groupId int, p_fundCode text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO "funds"(
    "fundName","fundStartDate","fundMaturityDate","fundPeriod",
    "depositionFrequency","moderator","recurringDepositAmount",
    "fundStatus","loanInterestRate","lateFeeRate",
    "monthlyDepDateBy","groupId","fundCode"
  ) VALUES (
    p_fundName,p_fundStartDate,p_fundMaturityDate,p_fundPeriod,
    p_depositionFrequency,p_moderator,p_recurringDepositAmount,
    COALESCE(p_fundStatus,'ACTIVE'),p_loanInterestRate,p_lateFeeRate,
    p_monthlyDepDateBy,p_groupId,p_fundCode
  )
  RETURNING "fundId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_fund(
  p_fundId int,
  p_fundName text, p_fundStartDate text, p_fundMaturityDate text, p_fundPeriod double precision,
  p_depositionFrequency text, p_moderator int, p_recurringDepositAmount double precision,
  p_fundStatus text, p_loanInterestRate double precision, p_lateFeeRate double precision,
  p_monthlyDepDateBy int, p_groupId int, p_fundCode text
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE "funds"
     SET "fundName"               = COALESCE(p_fundName, "fundName"),
         "fundStartDate"          = COALESCE(p_fundStartDate, "fundStartDate"),
         "fundMaturityDate"       = COALESCE(p_fundMaturityDate, "fundMaturityDate"),
         "fundPeriod"             = COALESCE(p_fundPeriod, "fundPeriod"),
         "depositionFrequency"    = COALESCE(p_depositionFrequency, "depositionFrequency"),
         "moderator"              = COALESCE(p_moderator, "moderator"),
         "recurringDepositAmount" = COALESCE(p_recurringDepositAmount, "recurringDepositAmount"),
         "fundStatus"             = COALESCE(p_fundStatus, "fundStatus"),
         "loanInterestRate"       = COALESCE(p_loanInterestRate, "loanInterestRate"),
         "lateFeeRate"            = COALESCE(p_lateFeeRate, "lateFeeRate"),
         "monthlyDepDateBy"       = COALESCE(p_monthlyDepDateBy, "monthlyDepDateBy"),
         "groupId"                = COALESCE(p_groupId, "groupId"),
         "fundCode"               = COALESCE(p_fundCode, "fundCode")
   WHERE "fundId" = p_fundId;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION delete_fund(p_fund_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM "funds" WHERE "fundId" = p_fund_id;
  RETURN FOUND;
END $$;

-- Active funds
CREATE OR REPLACE FUNCTION get_active_funds()
RETURNS SETOF "funds" LANGUAGE sql AS $$
  SELECT * FROM "funds" WHERE "fundStatus" = 'ACTIVE';
$$;

CREATE OR REPLACE FUNCTION get_fund_by_code(p_code text)
RETURNS int LANGUAGE sql AS $$
  SELECT COALESCE((
    SELECT "fundId" FROM "funds" WHERE "fundCode" = p_code LIMIT 1
  ), 0);
$$;

CREATE OR REPLACE FUNCTION get_active_funds_for_group(p_group_id int)
RETURNS SETOF "funds" LANGUAGE sql AS $$
  SELECT * FROM "funds"
   WHERE "groupId" = p_group_id AND "fundStatus" = 'ACTIVE';
$$;

CREATE OR REPLACE FUNCTION get_funds_for_group(p_group_id int)
RETURNS SETOF "funds" LANGUAGE sql AS $$
  SELECT * FROM "funds" WHERE "groupId" = p_group_id;
$$;

-- ===================== FUNDS + DETAILS =====================

CREATE OR REPLACE FUNCTION get_all_funds_with_details()
RETURNS TABLE (
  "fundId" int, "fundName" text, "fundStartDate" text, "fundMaturityDate" text,
  "fundPeriod" double precision, "depositionFrequency" text, "moderator" int,
  "recurringDepositAmount" double precision, "fundStatus" text, "loanInterestRate" double precision,
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
  "lateFeeRate" double precision, "monthlyDepDateBy" int, "groupId" int, "fundCode" text,
  "fundDetailsId" int, "totalExpectedDeposit" double precision, "totalCurrentDeposit" double precision,
  "totalCurrentLateFee" double precision, "totalCurrentInterestCollected" double precision,
  "totalExpectedMaturityAmount" double precision, "totalCurrAmount" double precision,
  "moderatorFirstName" text, "moderatorLastName" text, "groupName" text
)
LANGUAGE sql AS $$
  SELECT *
    FROM get_all_funds_with_details() s
   WHERE s."groupId" = p_group_id;
$$;

CREATE OR REPLACE FUNCTION get_fund_with_details(p_fund_id int)
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
   WHERE s."fundId" = p_fund_id;
$$;

-- Rate of interest
CREATE OR REPLACE FUNCTION get_rate_of_interest_for_fund(p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  SELECT COALESCE((
    SELECT "loanInterestRate" FROM "funds" WHERE "fundId" = p_fund_id
  ), 0);
$$;

-- ===================== MEMBERS =====================

CREATE OR REPLACE FUNCTION get_fund_members(p_fund_id int)
RETURNS TABLE ("fundMemberId" int, "userId" int, "fundId" int, "joiningDate" text)
LANGUAGE sql AS $$
  SELECT fm."fundMemberId", fm."userId", fm."fundId", fm."joiningDate"
    FROM fund_members fm
   WHERE fm."fundId" = p_fund_id
   ORDER BY fm."fundMemberId";
$$;

CREATE OR REPLACE FUNCTION add_fund_member(p_user_id int, p_fund_id int, p_joining_date text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO fund_members("userId","fundId","joiningDate")
  VALUES (p_user_id, p_fund_id, p_joining_date)
  ON CONFLICT ("userId","fundId") DO UPDATE
     SET "joiningDate" = EXCLUDED."joiningDate"
  RETURNING "fundMemberId" INTO v_id;

  IF v_id IS NULL THEN
    SELECT "fundMemberId" INTO v_id
      FROM fund_members
     WHERE "userId" = p_user_id AND "fundId" = p_fund_id;
  END IF;

  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION delete_fund_member(p_fund_member_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM fund_members WHERE "fundMemberId" = p_fund_member_id;
  RETURN FOUND;
END $$;

-- ===================== DETAILS =====================

CREATE OR REPLACE FUNCTION get_fund_details(p_fund_id int)
RETURNS TABLE (
  "fundDetailsId" int, "fundId" int,
  "totalExpectedDeposit" double precision, "totalCurrentDeposit" double precision,
  "totalCurrentLateFee" double precision, "totalCurrentInterestCollected" double precision,
  "totalExpectedMaturityAmount" double precision, "totalCurrAmount" double precision
)
LANGUAGE sql AS $$
  SELECT d."fundDetailsId", d."fundId", d."totalExpectedDeposit", d."totalCurrentDeposit",
         d."totalCurrentLateFee", d."totalCurrentInterestCollected",
         d."totalExpectedMaturityAmount", d."totalCurrAmount"
    FROM fund_details d
   WHERE d."fundId" = p_fund_id
   ORDER BY d."fundDetailsId" DESC
   LIMIT 1;
$$;

-- As procedures for parity with UsersSql; switch to functions if you prefer
CREATE OR REPLACE PROCEDURE insert_fund_details(
  p_fund_id int, p_totalExpectedDeposit double precision, p_totalCurrentDeposit double precision,
  p_totalCurrentLateFee double precision, p_totalCurrentInterestCollected double precision,
  p_totalExpectedMaturityAmount double precision, p_totalCurrAmount double precision
) LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO fund_details(
    "fundId","totalExpectedDeposit","totalCurrentDeposit",
    "totalCurrentLateFee","totalCurrentInterestCollected",
    "totalExpectedMaturityAmount","totalCurrAmount"
  ) VALUES (
    p_fund_id, p_totalExpectedDeposit, p_totalCurrentDeposit,
    p_totalCurrentLateFee, p_totalCurrentInterestCollected,
    p_totalExpectedMaturityAmount, p_totalCurrAmount
  );
END $$;

CREATE OR REPLACE PROCEDURE update_fund_details(
  p_fundDetailsId int, p_fund_id int, p_totalExpectedDeposit double precision, p_totalCurrentDeposit double precision,
  p_totalCurrentLateFee double precision, p_totalCurrentInterestCollected double precision,
  p_totalExpectedMaturityAmount double precision, p_totalCurrAmount double precision
) LANGUAGE plpgsql AS $$
BEGIN
  UPDATE fund_details
     SET "fundId" = COALESCE(p_fund_id, "fundId"),
         "totalExpectedDeposit" = COALESCE(p_totalExpectedDeposit, "totalExpectedDeposit"),
         "totalCurrentDeposit" = COALESCE(p_totalCurrentDeposit, "totalCurrentDeposit"),
         "totalCurrentLateFee" = COALESCE(p_totalCurrentLateFee, "totalCurrentLateFee"),
         "totalCurrentInterestCollected" = COALESCE(p_totalCurrentInterestCollected, "totalCurrentInterestCollected"),
         "totalExpectedMaturityAmount" = COALESCE(p_totalExpectedMaturityAmount, "totalExpectedMaturityAmount"),
         "totalCurrAmount" = COALESCE(p_totalCurrAmount, "totalCurrAmount")
   WHERE "fundDetailsId" = p_fundDetailsId;
END $$;

CREATE OR REPLACE FUNCTION upsert_fund_details(
  p_fund_id int, p_totalExpectedDeposit double precision, p_totalCurrentDeposit double precision,
  p_totalCurrentLateFee double precision, p_totalCurrentInterestCollected double precision,
  p_totalExpectedMaturityAmount double precision, p_totalCurrAmount double precision
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO fund_details(
    "fundId","totalExpectedDeposit","totalCurrentDeposit",
    "totalCurrentLateFee","totalCurrentInterestCollected",
    "totalExpectedMaturityAmount","totalCurrAmount"
  ) VALUES (
    p_fund_id, p_totalExpectedDeposit, p_totalCurrentDeposit,
    p_totalCurrentLateFee, p_totalCurrentInterestCollected,
    p_totalExpectedMaturityAmount, p_totalCurrAmount
  )
  RETURNING "fundDetailsId" INTO v_id;

  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION get_available_fund_amount(p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  SELECT
    COALESCE(fd."totalCurrentDeposit",0)
  + COALESCE(fd."totalCurrentLateFee",0)
  + COALESCE(fd."totalCurrentInterestCollected",0)
  - COALESCE(SUM(ld."currPrincipal"),0)
  FROM fund_details fd
  LEFT JOIN loans l ON l."fundId" = fd."fundId" AND l."status" = 'ACTIVE'
  LEFT JOIN loan_details ld ON ld."loanId" = l."loanId"
  WHERE fd."fundId" = p_fund_id
  GROUP BY fd."totalCurrentDeposit", fd."totalCurrentLateFee", fd."totalCurrentInterestCollected";
$$;

-- Users in the group who are NOT yet members of this fund
CREATE OR REPLACE FUNCTION get_available_fund_members(p_group_id int, p_fund_id int)
RETURNS TABLE (
  "userId" int, "firstName" text, "lastName" text, "emailId" text,
  "phoneNumber" text, "status" text, "passwordHash" text,
  "createdDate" text, "isPinSet" boolean, "hashPIN" text,
  "firebaseUserId" text, "userCode" text
) LANGUAGE sql AS $$
  SELECT u."userId", u."firstName", u."lastName", u."emailId",
         u."phoneNumber", u."status", u."passwordHash",
         u."createdDate", u."isPinSet", u."hashPIN",
         u."firebaseUserId", u."userCode"
    FROM users u
    JOIN group_members gm ON gm."userId" = u."userId"
   WHERE gm."groupId" = p_group_id
     AND NOT EXISTS (
       SELECT 1 FROM fund_members fm
        WHERE fm."fundId" = p_fund_id AND fm."userId" = u."userId"
     )
   ORDER BY u."firstName", u."lastName";
$$;

CREATE OR REPLACE FUNCTION add_fund_members_batch(p_members jsonb)
RETURNS SETOF int LANGUAGE plpgsql AS $$
DECLARE rec jsonb; v_id int;
BEGIN
  IF p_members IS NULL OR jsonb_typeof(p_members) <> 'array' THEN
    RAISE EXCEPTION 'add_fund_members_batch expects a JSON array';
  END IF;

  FOR rec IN SELECT * FROM jsonb_array_elements(p_members)
  LOOP
    v_id := add_fund_member(
      (rec->>'userId')::int,
      (rec->>'fundId')::int,
      NULLIF(rec->>'joiningDate','')
    );
    RETURN NEXT v_id;
  END LOOP;
  RETURN;
END $$;

CREATE OR REPLACE FUNCTION get_fund_members_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "fundMemberId" int,
  "userId"       int,
  "fundId"       int,
  "joiningDate"  text,
  "firstName"    text,
  "lastName"     text
) LANGUAGE sql STABLE AS $$
  SELECT
    fm."fundMemberId",
    fm."userId",
    fm."fundId",
    fm."joiningDate",
    u."firstName",
    COALESCE(u."lastName", '') AS "lastName"
  FROM fund_members fm
  JOIN users u  ON u."userId" = fm."userId"
  WHERE fm."fundId" = p_fund_id
  ORDER BY u."firstName", u."lastName";
$$;

CREATE OR REPLACE FUNCTION check_fund_member_exists(p_user_id int, p_fund_id int)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1
      FROM fund_members fm
     WHERE fm."userId" = p_user_id
       AND fm."fundId" = p_fund_id
     LIMIT 1
  );
$$;

-- (Optional but recommended) keep data consistent + speed lookups
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM   pg_constraint
    WHERE  conname = 'uq_fund_members_user_fund'
  ) THEN
    ALTER TABLE fund_members
      ADD CONSTRAINT uq_fund_members_user_fund
      UNIQUE ("userId","fundId");
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_fund_members_fund_user
  ON fund_members("fundId","userId");

 CREATE OR REPLACE FUNCTION add_fund_with_details(p_fund jsonb, p_details jsonb)
 RETURNS int
 LANGUAGE plpgsql
 AS $$
 DECLARE
   v_fund_id int;
 BEGIN
   -- Insert into ApnaFundDev.funds
   INSERT INTO "ApnaFundDev"."funds" (
     "fundName",
     "fundStartDate",
     "fundMaturityDate",
     "fundPeriod",
     "depositionFrequency",
     "moderator",
     "recurringDepositAmount",
     "fundStatus",
     "loanInterestRate",
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
     (p_fund->>'lateFeeRate')::double precision,
     (p_fund->>'monthlyDepDateBy')::int,
     (p_fund->>'groupId')::int,
     p_fund->>'fundCode'
   )
   RETURNING "fundId" INTO v_fund_id;

   INSERT INTO "ApnaFundDev"."fund_details" (
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

  CREATE UNIQUE INDEX IF NOT EXISTS uq_funddetails_fund
    ON fund_details ("fundId");