DO $$
BEGIN
  IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_namespace n
            ON n.oid = c.connamespace
        WHERE c.conname = 'uq_deposits_fund_user_month_year'
          AND n.nspname = current_schema()
  ) THEN
    ALTER TABLE "deposits"
      ADD CONSTRAINT uq_deposits_fund_user_month_year
      UNIQUE ("fundId","depositorId","depositMonth","depositYear");
  END IF;
END$$;


-- =====================================================================
-- Basic reads
-- =====================================================================

CREATE OR REPLACE FUNCTION get_deposit(p_id int)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."depositId" = p_id
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION get_all_deposits()
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   ORDER BY d."depositYear" DESC, d."depositMonth" DESC, d."depositId" DESC;
$$;

CREATE OR REPLACE FUNCTION get_deposits_for_fund(p_fund_id int)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."fundId" = p_fund_id
   ORDER BY d."depositYear" DESC, d."depositMonth" DESC, d."depositId" DESC;
$$;

CREATE OR REPLACE FUNCTION get_deposits_for_fund_month_year(p_fund_id int, p_month text, p_year text)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."fundId" = p_fund_id
     AND d."depositMonth" = p_month
     AND d."depositYear"  = p_year
   ORDER BY d."depositId" DESC;
$$;



-- =====================================================================
-- Joins with member names (DTO with names)
-- =====================================================================

-- For a fund (all months/years)
CREATE OR REPLACE FUNCTION get_deposits_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision,
  "firstName" text, "lastName" text, "userId" int
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee",
         u."firstName", COALESCE(u."lastName",'') AS "lastName", u."userId"
    FROM "deposits" d
    JOIN "users" u ON u."userId" = d."depositorId"
   WHERE d."fundId" = p_fund_id
   ORDER BY d."depositYear" DESC, d."depositMonth" DESC, d."depositId" DESC;
$$;

-- For a specific month/year (accept TEXT, cast to INT)
CREATE OR REPLACE FUNCTION get_deposits_with_names_for_fund_month_year(
  p_fund_id int, p_month text, p_year text
)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision,
  "firstName" text, "lastName" text, "userId" int
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee",
         u."firstName", COALESCE(u."lastName",'') AS "lastName", u."userId"
    FROM "deposits" d
    JOIN "users" u ON u."userId" = d."depositorId"
   WHERE d."fundId" = p_fund_id
     AND d."depositMonth" = p_month::text
     AND d."depositYear"  = p_year::text
   ORDER BY d."depositId" DESC;
$$;

-- Full with-names list for a fund/month/year (same as above but separate entry-point)
CREATE OR REPLACE FUNCTION get_all_deposits_for_fund_for_month_year(
  p_fund_id int, p_month text, p_year text
)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision,
  "firstName" text, "lastName" text, userId int
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee",
         u."firstName", COALESCE(u."lastName",'') AS "lastName", u."userId"
    FROM "users" u
    INNER JOIN "fund_members" fm ON u."userId" = fm."userId"
    INNER JOIN "funds" f ON fm."fundId" = f."fundId"
    LEFT JOIN "deposits" d
             ON d."depositorId" = u."userId"
            AND d."fundId"      = f."fundId"
            AND d."depositMonth"= p_month::text
            AND d."depositYear" = p_year::text
   WHERE f."fundId" = p_fund_id
   ORDER BY u."firstName" DESC;
$$;



-- =====================================================================
-- Targeted lookups
-- =====================================================================

CREATE OR REPLACE FUNCTION get_deposits_for_fund_depositor_month_year(
  p_depositor_id int, p_fund_id int, p_month text, p_year text
)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."fundId" = p_fund_id
     AND d."depositorId" = p_depositor_id
     AND d."depositMonth" = p_month::text
     AND d."depositYear"  = p_year::text
   ORDER BY d."depositId" DESC
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION get_deposit_for_member(
  p_fund_id int, p_depositor_id int, p_month text, p_year text
)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."fundId" = p_fund_id
     AND d."depositorId" = p_depositor_id
     AND d."depositMonth" = p_month::text
     AND d."depositYear"  = p_year::text
   LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION get_deposit_for_member_for_fund(
  p_fund_id int, p_depositor_id int
)
RETURNS TABLE (
  "depositId" int, "depositorId" int, "fundId" int, "depositedDate" text,
  "depositAmount" double precision, "depositMonth" text, "depositYear" text, "lateFee" double precision
) LANGUAGE sql AS $$
  SELECT d."depositId", d."depositorId", d."fundId", d."depositedDate",
         d."depositAmount", d."depositMonth", d."depositYear", d."lateFee"
    FROM "deposits" d
   WHERE d."fundId" = p_fund_id
     AND d."depositorId" = p_depositor_id
   ORDER BY d."depositYear" DESC, d."depositMonth" DESC, d."depositId" DESC;
$$;



-- =====================================================================
-- Mutations
-- =====================================================================

CREATE OR REPLACE FUNCTION add_deposit(
  p_depositor_id int,
  p_fund_id int,
  p_deposited_date date,
  p_deposit_amount double precision,
  p_deposit_month text,
  p_deposit_year text,
  p_late_fee double precision
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO "deposits"(
    "depositorId","fundId","depositedDate",
    "depositAmount","depositMonth","depositYear","lateFee"
  ) VALUES (
    p_depositor_id, p_fund_id, p_deposited_date,
    p_deposit_amount, p_deposit_month, p_deposit_year, COALESCE(p_late_fee,0)
  )
  ON CONFLICT ("fundId","depositorId","depositMonth","depositYear")
  DO UPDATE SET
    "depositedDate" = EXCLUDED."depositedDate",
    "depositAmount" = EXCLUDED."depositAmount",
    "lateFee"       = EXCLUDED."lateFee"
  RETURNING "depositId" INTO v_id;

  RETURN v_id;
END $$;


CREATE OR REPLACE FUNCTION update_deposit(
  p_deposit_id int,
  p_depositor_id int,
  p_fund_id int,
  p_deposited_date text,
  p_deposit_amount double precision,
  p_deposit_month text,
  p_deposit_year text,
  p_late_fee double precision
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE "deposits"
     SET "depositorId"   = COALESCE(p_depositor_id, "depositorId"),
         "fundId"        = COALESCE(p_fund_id, "fundId"),
         "depositedDate" = COALESCE(p_deposited_date, "depositedDate"),
         "depositAmount" = COALESCE(p_deposit_amount, "depositAmount"),
         "depositMonth"  = COALESCE(p_deposit_month, "depositMonth"),
         "depositYear"   = COALESCE(p_deposit_year, "depositYear"),
         "lateFee"       = COALESCE(p_late_fee, "lateFee")
   WHERE "depositId" = p_deposit_id;
  RETURN FOUND;
END $$;


CREATE OR REPLACE FUNCTION delete_deposit(p_deposit_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM "deposits" WHERE "depositId" = p_deposit_id;
  RETURN FOUND;
END $$;


-- Called in batch by JDBI @SqlBatch; returns the current/updated id.
CREATE OR REPLACE FUNCTION upsert_deposit(
  p_deposit_id int,
  p_depositor_id int,
  p_fund_id int,
  p_deposited_date text,
  p_deposit_amount double precision,
  p_deposit_month text,
  p_deposit_year text,
  p_late_fee double precision
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  IF p_deposit_id IS NOT NULL THEN
    UPDATE "deposits"
       SET "depositorId"   = COALESCE(p_depositor_id, "depositorId"),
           "fundId"        = COALESCE(p_fund_id, "fundId"),
           "depositedDate" = COALESCE(p_deposited_date, "depositedDate"),
           "depositAmount" = COALESCE(p_deposit_amount, "depositAmount"),
           "depositMonth"  = COALESCE(p_deposit_month, "depositMonth"),
           "depositYear"   = COALESCE(p_deposit_year, "depositYear"),
           "lateFee"       = COALESCE(p_late_fee, "lateFee")
     WHERE "depositId" = p_deposit_id;

    IF FOUND THEN
      v_id := p_deposit_id;
      RETURN v_id;
    END IF;
    -- fall through to INSERT if not found
  END IF;

  INSERT INTO "deposits"(
    "depositorId","fundId","depositedDate",
    "depositAmount","depositMonth","depositYear","lateFee"
  ) VALUES (
    p_depositor_id, p_fund_id, p_deposited_date,
    p_deposit_amount, p_deposit_month, p_deposit_year, COALESCE(p_late_fee,0)
  )
  ON CONFLICT ("fundId","depositorId","depositMonth","depositYear")
  DO UPDATE SET
    "depositedDate" = EXCLUDED."depositedDate",
    "depositAmount" = EXCLUDED."depositAmount",
    "lateFee"       = EXCLUDED."lateFee"
  RETURNING "depositId" INTO v_id;

  RETURN v_id;
END $$;



CREATE OR REPLACE FUNCTION save_or_update_all_deposits_and_fetch(
  p_fund_id int,
  p_month   text,
  p_year    text,
  p_rows    jsonb
)
RETURNS TABLE (
  "depositId"   int,
  "depositorId" int,
  "fundId"      int,
  "depositedDate" text,
  "depositAmount" double precision,
  "depositMonth"  text,
  "depositYear"   text,
  "lateFee"       double precision,
  "firstName"     text,
  "lastName"      text,
  "userId"        int
) LANGUAGE plpgsql AS $$
DECLARE
  v_fd_id             int;
  v_currDep           double precision;
  v_currLate          double precision;
  v_currTotal         double precision;
  v_expectedMaturity  double precision;

  r           jsonb;
  v_depositor int;
  v_date      text;
  v_amt       double precision;
  v_late      double precision;

  ex RECORD; -- existing row in deposits
BEGIN
  -- Load the latest fund_details snapshot for THIS fund
  SELECT fd."fundDetailsId",
         fd."totalCurrentDeposit",
         fd."totalCurrentLateFee",
         fd."totalCurrAmount",
         fd."totalExpectedMaturityAmount"
    INTO v_fd_id, v_currDep, v_currLate, v_currTotal, v_expectedMaturity
  FROM fund_details fd
  WHERE fd."fundId" = p_fund_id
  ORDER BY fd."fundDetailsId" DESC
  LIMIT 1;

  IF v_fd_id IS NULL THEN
    v_fd_id := 0;
    v_currDep := 0;
    v_currLate := 0;
    v_currTotal := 0;
    v_expectedMaturity := 0;
  END IF;

  -- Iterate incoming rows
  FOR r IN SELECT * FROM jsonb_array_elements(p_rows) LOOP
    v_depositor := (r->>'depositorId')::int;
    v_date      := r->>'depositedDate';
    v_amt       := COALESCE((r->>'depositAmount')::double precision, 0);
    v_late      := COALESCE((r->>'lateFee')::double precision, 0);

    -- Look up existing deposit for this fund/depositor/month/year
    SELECT d.*
      INTO ex
    FROM deposits d
    WHERE d."fundId" = p_fund_id
      AND d."depositorId" = v_depositor
      AND d."depositMonth" = p_month
      AND d."depositYear"  = p_year
    LIMIT 1;

    IF FOUND THEN
      -- UPDATE existing row
      UPDATE deposits d2
         SET "depositedDate" = v_date, -- if you later make this DATE: to_date(v_date,'DD/MM/YYYY')
             "depositAmount" = v_amt,
             "lateFee"       = v_late
       WHERE d2."depositId" = ex."depositId";

      -- Apply deltas to fund_details rollups
      v_currDep          := v_currDep + (v_amt - ex."depositAmount");
      v_currLate         := v_currLate + (v_late - COALESCE(ex."lateFee",0));
      v_currTotal        := v_currTotal + (v_amt - ex."depositAmount") + (v_late - COALESCE(ex."lateFee",0));
      v_expectedMaturity := v_expectedMaturity + (v_late - COALESCE(ex."lateFee",0));

    ELSE
      -- INSERT new row
      INSERT INTO deposits(
        "depositorId","fundId","depositedDate",
        "depositAmount","depositMonth","depositYear","lateFee"
      )
      VALUES (
        v_depositor, p_fund_id, v_date,
        v_amt, p_month, p_year, v_late
      );

      -- Increment rollups
      v_currDep          := v_currDep + v_amt;
      v_currLate         := v_currLate + v_late;
      v_currTotal        := v_currTotal + v_amt + v_late;
      v_expectedMaturity := v_expectedMaturity + v_late;
    END IF;
  END LOOP;

  -- Persist updated fund_details for THIS fund
  UPDATE fund_details fd
     SET "totalCurrentDeposit"        = v_currDep,
         "totalCurrentLateFee"        = v_currLate,
         "totalCurrAmount"            = v_currTotal,
         "totalExpectedMaturityAmount"= v_expectedMaturity
   WHERE fd."fundId" = p_fund_id;

  -- Return refreshed rows: members even without deposit (LEFT JOIN)
  RETURN QUERY
  SELECT
    d."depositId",
    d."depositorId",
    f."fundId",
    d."depositedDate",
    d."depositAmount",
    d."depositMonth",
    d."depositYear",
    d."lateFee",
    u."firstName",
    COALESCE(u."lastName",'') AS "lastName",
    u."userId"
  FROM users u
  INNER JOIN fund_members fm ON fm."userId" = u."userId"
  INNER JOIN funds f         ON f."fundId"  = fm."fundId"
  LEFT JOIN deposits d
         ON d."depositorId"  = u."userId"
        AND d."fundId"       = f."fundId"
        AND d."depositMonth" = p_month
        AND d."depositYear"  = p_year
  WHERE f."fundId" = p_fund_id
  ORDER BY u."firstName" ASC;
END $$;