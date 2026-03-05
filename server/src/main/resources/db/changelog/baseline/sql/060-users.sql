CREATE OR REPLACE FUNCTION get_user(p_user_id int)
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
   WHERE u."userId" = p_user_id;
$$;

CREATE OR REPLACE FUNCTION get_users()
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
    FROM users u;
$$;

CREATE OR REPLACE FUNCTION get_user_by_email(p_email text)
RETURNS SETOF users LANGUAGE sql AS $$
  SELECT * FROM users WHERE "emailId" = p_email LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION get_user_by_phone(p_phone text)
RETURNS SETOF users LANGUAGE sql AS $$
  SELECT * FROM users WHERE "phoneNumber" = p_phone LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION does_user_exist(p_email text)
RETURNS boolean LANGUAGE sql AS $$
  SELECT EXISTS (SELECT 1 FROM users WHERE "emailId" = p_email);
$$;

CREATE OR REPLACE FUNCTION count_matching_users(p_email text, p_phone text, p_exclude_user_id int)
RETURNS int LANGUAGE sql AS $$
  SELECT COUNT(*)::int
    FROM users
   WHERE (("emailId" = p_email) OR ("phoneNumber" = p_phone))
     AND "userId" <> p_exclude_user_id;
$$;

CREATE OR REPLACE FUNCTION check_user_pin(p_user_id int, p_pin text)
RETURNS boolean LANGUAGE sql AS $$
  SELECT EXISTS(
    SELECT 1 FROM users
     WHERE "userId" = p_user_id AND "hashPIN" = p_pin
  )
$$;

CREATE OR REPLACE FUNCTION get_user_with_group(p_group_id int)
RETURNS TABLE (
  "userId" int, "firstName" text, "lastName" text, "emailId" text,
  "phoneNumber" text, "status" text, "userCode" text, "isPinSet" boolean,
  "groupId" int, "groupName" text, "moderator" int,
  "description" text, "groupStatus" text
) LANGUAGE sql AS $$
  SELECT u."userId", u."firstName", u."lastName", u."emailId",
         u."phoneNumber", u."status", u."userCode", u."isPinSet",
         g."groupId", g."groupName", g."moderator", g."description", g."status"
    FROM users u
    LEFT JOIN group_members gm ON u."userId" = gm."userId"
    LEFT JOIN "groups" g       ON gm."groupId" = g."groupId"
   WHERE gm."groupId" = p_group_id;
$$;

CREATE OR REPLACE FUNCTION get_all_users_with_group()
RETURNS TABLE (
  "userId" int, "firstName" text, "lastName" text, "emailId" text,
  "phoneNumber" text, "status" text, "userCode" text, "isPinSet" boolean,
  "groupId" int, "groupName" text, "moderator" int,
  "description" text, "groupStatus" text
) LANGUAGE sql AS $$
  SELECT u."userId", u."firstName", u."lastName", u."emailId",
         u."phoneNumber", u."status", u."userCode", u."isPinSet",
         g."groupId", g."groupName", g."moderator", g."description", g."status"
    FROM users u
    LEFT JOIN group_members gm ON u."userId" = gm."userId"
    LEFT JOIN "groups" g       ON gm."groupId" = g."groupId";
$$;

-- Fetch a group_members row for a (user, group)
CREATE OR REPLACE FUNCTION get_group_member(p_user_id int, p_group_id int)
RETURNS TABLE(
  "groupMemberId" int,
  "userId" int,
  "groupId" int,
  "joiningDate" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId", gm."userId", gm."groupId", gm."joiningDate"
    FROM group_members gm
   WHERE gm."userId" = p_user_id
     AND gm."groupId" = p_group_id
   LIMIT 1;
$$;

-- ---------- USERS (writes) ----------

CREATE OR REPLACE FUNCTION upsert_user_by_email(
  p_firstName text, p_lastName text, p_emailId text, p_phoneNumber text,
  p_status text, p_createdDate text,
  p_isPinSet boolean, p_hashPIN text, p_passwordHash text, p_firebaseUserId text, p_userCode text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO users("firstName","lastName","emailId","phoneNumber","status","createdDate",
                    "isPinSet","hashPIN","passwordHash","firebaseUserId","userCode")
  VALUES(p_firstName,p_lastName,p_emailId,p_phoneNumber,p_status,p_createdDate,
         p_isPinSet,p_hashPIN,p_passwordHash,p_firebaseUserId,p_userCode)
  ON CONFLICT ("emailId") DO UPDATE
    SET "firstName"      = COALESCE(EXCLUDED."firstName",      users."firstName"),
        "lastName"       = COALESCE(EXCLUDED."lastName",       users."lastName"),
        "phoneNumber"    = COALESCE(EXCLUDED."phoneNumber",    users."phoneNumber"),
        "status"         = COALESCE(EXCLUDED."status",         users."status"),
        "createdDate"    = COALESCE(EXCLUDED."createdDate",    users."createdDate"),
        "isPinSet"       = COALESCE(EXCLUDED."isPinSet",       users."isPinSet"),
        "hashPIN"        = COALESCE(EXCLUDED."hashPIN",        users."hashPIN"),
        "passwordHash"   = COALESCE(EXCLUDED."passwordHash",   users."passwordHash"),
        "firebaseUserId" = COALESCE(EXCLUDED."firebaseUserId", users."firebaseUserId"),
        "userCode"       = COALESCE(EXCLUDED."userCode",       users."userCode");
  SELECT u."userId" INTO v_id FROM users u WHERE u."emailId" = p_emailId LIMIT 1;
  RETURN v_id;
END $$;

CREATE OR REPLACE PROCEDURE update_user_password(p_user_id int, p_password_hash text)
LANGUAGE plpgsql AS $$
BEGIN
  UPDATE users SET "passwordHash" = p_password_hash WHERE "userId" = p_user_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'No user %', p_user_id USING ERRCODE = 'NO_DATA_FOUND';
  END IF;
  INSERT INTO user_password_history("userId","passwordHash")
  VALUES (p_user_id, p_password_hash);
END $$;

CREATE OR REPLACE PROCEDURE update_user_pin(p_user_id int, p_pin_hash text)
LANGUAGE plpgsql AS $$
BEGIN
  UPDATE users
     SET "hashPIN" = p_pin_hash, "isPinSet" = TRUE
   WHERE "userId" = p_user_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'No user %', p_user_id USING ERRCODE = 'NO_DATA_FOUND';
  END IF;
  INSERT INTO user_pin_history("userId","pinHash")
  VALUES (p_user_id, p_pin_hash);
END $$;

CREATE OR REPLACE PROCEDURE upsert_group_member(p_user_id int, p_group_id int, p_joining_date date)
LANGUAGE plpgsql AS $$
BEGIN
  INSERT INTO group_members("userId","groupId","joiningDate")
  VALUES(p_user_id, p_group_id, p_joining_date)
  ON CONFLICT ("userId","groupId") DO UPDATE
     SET "joiningDate" = EXCLUDED."joiningDate";
END $$;

CREATE OR REPLACE PROCEDURE delete_user(p_user_id int)
LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM users WHERE "userId" = p_user_id;
END $$;

CREATE OR REPLACE PROCEDURE delete_all_users()
LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM users;
END $$;

-- ---------- FEEDBACK JOIN ----------

CREATE OR REPLACE FUNCTION get_feedback_with_user_group()
RETURNS TABLE (
  "feedbackId" integer,
  "message"    text,
  "timestamp"  text,
  "userName"   text,
  "groupName"  text
) LANGUAGE sql STABLE AS $$
  SELECT
    f."feedbackId",
    f."message",
    f."timestamp",
    CASE
      WHEN u."lastName" IS NULL OR u."lastName" = ''
      THEN u."firstName"
      ELSE u."firstName" || ' ' || u."lastName"
    END AS "userName",
    g."groupName"
  FROM feedbacks f
  JOIN users u               ON f."userId" = u."userId"
  LEFT JOIN group_members gm ON u."userId" = gm."userId"
  LEFT JOIN "groups" g       ON gm."groupId" = g."groupId"
  ORDER BY f."timestamp" DESC;
$$;

-- ---------- Aggregates / extras ----------

-- One group for a user (latest joining)
CREATE OR REPLACE FUNCTION get_group_for_user(p_user_id int)
RETURNS TABLE (
  "groupId" int, "groupName" text, "moderator" int, "createdDate" text,
  "description" text, "groupCode" text, "status" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."moderator", g."createdDate",
         g."description", g."groupCode", g."status"
    FROM group_members gm
    JOIN "groups" g ON g."groupId" = gm."groupId"
   WHERE gm."userId" = p_user_id
   ORDER BY gm."joiningDate" DESC
   LIMIT 1;
$$;

-- All funds for a user
CREATE OR REPLACE FUNCTION get_funds_for_user(p_user_id int)
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
   ORDER BY f."fundId";
$$;

-- Total deposit by user in a fund
CREATE OR REPLACE FUNCTION get_total_deposit(p_user_id int, p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  SELECT COALESCE(SUM(d."depositAmount"), 0)::double precision
    FROM deposits d
   WHERE d."depositorId" = p_user_id
     AND d."fundId"      = p_fund_id;
$$;

-- Per-member expected maturity amount for a fund
CREATE OR REPLACE FUNCTION get_per_member_expected_maturity_amount(p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  WITH f AS (
    SELECT fd."totalExpectedMaturityAmount", COUNT(DISTINCT fm."userId") AS members
      FROM fund_details fd
      JOIN funds fu ON fu."fundId" = fd."fundId"
      JOIN fund_members fm ON fm."fundId" = fu."fundId"
     WHERE fd."fundId" = p_fund_id
     GROUP BY fd."totalExpectedMaturityAmount"
  )
  SELECT CASE WHEN f.members > 0 THEN (f."totalExpectedMaturityAmount" / f.members)::double precision
              ELSE 0::double precision
         END
  FROM f;
$$;

-- Total loan amount for user in a fund
CREATE OR REPLACE FUNCTION get_total_loan_amount(p_user_id int, p_fund_id int)
RETURNS double precision LANGUAGE sql AS $$
  SELECT COALESCE(SUM(l."loanAmount"), 0)::double precision
    FROM loans l
   WHERE l."borrowerId" = p_user_id
     AND l."fundId"     = p_fund_id;
$$;

CREATE OR REPLACE FUNCTION get_user_details(p_user_id int)
RETURNS TABLE (
  "userId" int,
  "groupsCount" int,
  "fundsCount" int,
  "totalDepositAllFunds" double precision,
  "totalLoansAllFunds" double precision
) LANGUAGE sql AS $$
  WITH g AS (
    SELECT COUNT(DISTINCT gm."groupId")::int AS cnt
      FROM group_members gm WHERE gm."userId" = p_user_id
  ),
  f AS (
    SELECT COUNT(DISTINCT fm."fundId")::int AS cnt
      FROM fund_members fm WHERE fm."userId" = p_user_id
  ),
  d AS (
    SELECT COALESCE(SUM(d."depositAmount"),0)::double precision AS amt
      FROM deposits d WHERE d."depositorId" = p_user_id
  ),
  l AS (
    SELECT COALESCE(SUM(l."loanAmount"),0)::double precision AS amt
      FROM loans l WHERE l."borrowerId" = p_user_id
  )
  SELECT p_user_id AS "userId",
         (SELECT cnt FROM g) AS "groupsCount",
         (SELECT cnt FROM f) AS "fundsCount",
         (SELECT amt FROM d) AS "totalDepositAllFunds",
         (SELECT amt FROM l) AS "totalLoansAllFunds";
$$;

CREATE OR REPLACE FUNCTION validate_user(
    p_email         text,
    p_password_hash text
)
RETURNS TABLE (
    "userId"         int,
    "firstName"      text,
    "lastName"       text,
    "emailId"        text,
    "phoneNumber"    text,
    "status"         text,
    "passwordHash"   text,
    "createdDate"    text,
    "isPinSet"       boolean,
    "hashPIN"        text,
    "firebaseUserId" text,
    "userCode"       text
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId",
        u."phoneNumber",
        u."status",
        u."passwordHash",
        u."createdDate"::text AS "createdDate",
        u."isPinSet",
        u."hashPIN",
        u."firebaseUserId",
        u."userCode"
    FROM users u
    WHERE lower(u."emailId") = lower(p_email)
      AND u."passwordHash"   = p_password_hash
    LIMIT 1;
$$;

-- 1) Roles of a user
CREATE OR REPLACE FUNCTION get_roles_of_user(p_user_id int)
RETURNS TABLE (
  "roleId" int,
  "roleCode" text,
  "roleDescription" text,
  "status" text
) LANGUAGE sql AS $$
  SELECT r."roleId", r."roleCode", r."roleDescription", r."status"
    FROM user_roles ur
    JOIN roles r ON r."roleId" = ur."roleId"
   WHERE ur."userId" = p_user_id
   ORDER BY r."roleId";
$$;

-- 2) user_roles rows of a user
CREATE OR REPLACE FUNCTION get_user_roles_of_user(p_user_id int)
RETURNS TABLE (
  "userRoleId" int,
  "userId"     int,
  "roleId"     int,
  "status"     text
) LANGUAGE sql AS $$
  SELECT ur."userRoleId", ur."userId", ur."roleId", ur."status"
    FROM user_roles ur
   WHERE ur."userId" = p_user_id
   ORDER BY ur."userRoleId";
$$;

-- 3) All role codes (strings) for a user
CREATE OR REPLACE FUNCTION get_all_user_role_codes(p_user_id int)
RETURNS SETOF text LANGUAGE sql AS $$
  SELECT r."roleCode"
    FROM user_roles ur JOIN roles r ON r."roleId" = ur."roleId"
   WHERE ur."userId" = p_user_id
   ORDER BY r."roleCode";
$$;

-- 4) All role IDs (ints) for a user
CREATE OR REPLACE FUNCTION get_all_user_role_ids(p_user_id int)
RETURNS SETOF int LANGUAGE sql AS $$
  SELECT ur."roleId"
    FROM user_roles ur
   WHERE ur."userId" = p_user_id
   ORDER BY ur."roleId";
$$;

-- 5) All users for a given role (returns user_roles rows)
CREATE OR REPLACE FUNCTION get_all_users_for_role(p_role_id int)
RETURNS TABLE (
  "userRoleId" int,
  "userId"     int,
  "roleId"     int,
  "status"     text
) LANGUAGE sql AS $$
  SELECT ur."userRoleId", ur."userId", ur."roleId", ur."status"
    FROM user_roles ur
   WHERE ur."roleId" = p_role_id
   ORDER BY ur."userRoleId";
$$;

-- 6) Add a single user-role (upsert), return id
CREATE OR REPLACE FUNCTION add_user_role(p_user_id int, p_role_id int, p_status text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_roles("userId","roleId","status")
  VALUES (p_user_id, p_role_id, COALESCE(p_status,'PENDING'))
  ON CONFLICT ("userId","roleId")
  DO UPDATE SET "status" = EXCLUDED."status"
  RETURNING "userRoleId" INTO v_id;

  IF v_id IS NULL THEN
    SELECT "userRoleId" INTO v_id
      FROM user_roles
     WHERE "userId" = p_user_id AND "roleId" = p_role_id
     LIMIT 1;
  END IF;

  RETURN v_id;
END $$;

-- 7) Batch add user-roles from JSONB array of objects {userId, roleId, status}
CREATE OR REPLACE FUNCTION add_user_roles_batch(p_items jsonb)
RETURNS int
LANGUAGE plpgsql AS $$
DECLARE
  v_cnt    int := 0;
  obj      jsonb;
  v_user   int;
  v_role   int;
  v_status text;
BEGIN
  IF jsonb_typeof(p_items) <> 'array' THEN
    RAISE EXCEPTION 'add_user_roles_batch expects a JSON array';
  END IF;

  FOR obj IN SELECT value FROM jsonb_array_elements(p_items)
  LOOP
    v_user   := (obj->>'userId')::int;
    v_role   := (obj->>'roleId')::int;
    v_status := COALESCE(NULLIF(obj->>'status',''), 'PENDING');

    PERFORM add_user_role(v_user, v_role, v_status);

    v_cnt := v_cnt + 1;
  END LOOP;

  RETURN v_cnt;
END $$;

-- 8) Update a user_role row (by id)
CREATE OR REPLACE FUNCTION update_user_role(p_user_role_id int, p_status text)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_roles
     SET "status" = COALESCE(p_status, "status")
   WHERE "userRoleId" = p_user_role_id;
  RETURN FOUND;
END $$;

-- 9) Remove by id
CREATE OR REPLACE FUNCTION remove_user_role(p_user_role_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM user_roles WHERE "userRoleId" = p_user_role_id;
  RETURN FOUND;
END $$;

-- 10) Remove by composite (userId + roleId)
CREATE OR REPLACE FUNCTION delete_user_role_by_composite(p_user_id int, p_role_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM user_roles WHERE "userId" = p_user_id AND "roleId" = p_role_id;
  RETURN FOUND;
END $$;

-- 11) Pending moderator requests

CREATE OR REPLACE FUNCTION get_pending_moderator_requests()
RETURNS TABLE (
  "userId"     int,
  "groupId"    int,
  "groupName"  text,
  "moderatorName"  text,
  "groupDescription"   text
) LANGUAGE sql AS $$
     SELECT u."userId", g."groupId",
             u."firstName" || ' ' || u."lastName" AS moderatorName,
             g."groupName",
             g."description" AS groupDescription
      FROM USERS u
      INNER JOIN USER_ROLES ur ON u."userId" = ur."userId"
      INNER JOIN ROLES r ON ur."roleId" = r."roleId"
      INNER JOIN GROUPS g ON g."moderator" = u."userId"
      WHERE r."roleCode" = 'MODERATOR'
        AND ur."status" = 'PENDING'
        AND g."status" = 'PENDING';
$$;


