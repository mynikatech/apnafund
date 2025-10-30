-- All groups (DTO shape)
CREATE OR REPLACE FUNCTION get_groups()
RETURNS TABLE (
  "groupId" int, "groupName" text, "description" text, "moderator" int, "status" text,
  "createdDate" text, "groupCode" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."description", g."moderator", g."status", g."createdDate"
  , g."groupCode"
    FROM "groups" g
   ORDER BY g."groupId";
$$;

-- One group (DTO)
CREATE OR REPLACE FUNCTION get_group(p_group_id int)
RETURNS TABLE (
  "groupId" int, "groupName" text, "description" text, "moderator" int, "status" text,
  "createdDate" text, "groupCode" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."description", g."moderator", g."status", g."createdDate" , g."groupCode"
    FROM "groups" g
   WHERE g."groupId" = p_group_id
   LIMIT 1;
$$;

-- Add group -> id
CREATE OR REPLACE FUNCTION add_group(
  p_group_name     text,
  p_description    text,
  p_moderator      int,
  p_groupcode      text,
  p_created_date   text,
  p_status         text
) RETURNS int
LANGUAGE plpgsql
AS $$
DECLARE
  v_id            int;
  v_joining_date  date;
BEGIN
  -- Insert the group
  INSERT INTO "groups" (
    "groupName","description","moderator","groupCode","createdDate","status"
  )
  VALUES (
    p_group_name, p_description, p_moderator, p_groupcode, p_created_date,
    COALESCE(p_status, 'ACTIVE')
  )
  RETURNING "groupId" INTO v_id;

  -- Derive joining date from createdDate text (supports DD/MM/YYYY or DD-MM-YYYY), else today
  IF p_created_date IS NULL OR btrim(p_created_date) = '' THEN
    v_joining_date := CURRENT_DATE;
  ELSIF p_created_date ~ '^\d{2}/\d{2}/\d{4}$' THEN
    v_joining_date := to_date(p_created_date, 'DD/MM/YYYY');
  ELSIF p_created_date ~ '^\d{2}-\d{2}-\d{4}$' THEN
    v_joining_date := to_date(p_created_date, 'DD-MM-YYYY');
  ELSE
    v_joining_date := CURRENT_DATE;
  END IF;

  -- Ensure moderator is a member (only if not already)
  IF p_moderator IS NOT NULL THEN
    INSERT INTO group_members("userId","groupId","joiningDate")
    VALUES (p_moderator, v_id, v_joining_date)
    ON CONFLICT ("userId","groupId") DO NOTHING;
  END IF;

  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION update_group(
  p_group_id     int,
  p_group_name   text,
  p_description  text,
  p_moderator    int,
  p_group_code   text,
  p_status       text
) RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE
  v_updated       boolean;
  v_created_text  text;
  v_joining_date  date;
  v_moderator     int;
BEGIN
  UPDATE "groups" g
     SET "groupName"   = COALESCE(p_group_name,  g."groupName"),
         "description" = COALESCE(p_description, g."description"),
         "moderator"   = COALESCE(p_moderator,   g."moderator"),
         "groupCode"   = COALESCE(p_group_code,  g."groupCode"),
         "status"      = COALESCE(p_status,      g."status")
   WHERE g."groupId" = p_group_id;

  v_updated := FOUND;
  IF NOT v_updated THEN
    RETURN false;
  END IF;

  -- Use the group's createdDate (text) as a joining date hint; fall back to today
  SELECT g."createdDate", g."moderator"
    INTO v_created_text, v_moderator
    FROM "groups" g
   WHERE g."groupId" = p_group_id;

  IF v_created_text IS NULL OR btrim(v_created_text) = '' THEN
    v_joining_date := CURRENT_DATE;
  ELSIF v_created_text ~ '^\d{2}/\d{2}/\d{4}$' THEN
    v_joining_date := to_date(v_created_text, 'DD/MM/YYYY');
  ELSIF v_created_text ~ '^\d{2}-\d{2}-\d{4}$' THEN
    v_joining_date := to_date(v_created_text, 'DD-MM-YYYY');
  ELSE
    v_joining_date := CURRENT_DATE;
  END IF;

  -- Ensure the (current) moderator is also a member
  IF v_moderator IS NOT NULL THEN
    INSERT INTO group_members("userId","groupId","joiningDate")
    VALUES (v_moderator, p_group_id, v_joining_date)
    ON CONFLICT ("userId","groupId") DO NOTHING;
  END IF;

  RETURN true;
END;
$$;

-- Delete group -> boolean
CREATE OR REPLACE FUNCTION delete_group(p_group_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM "groups" WHERE "groupId" = p_group_id;
  RETURN FOUND;
END $$;

-- Group by moderator (DTO)
CREATE OR REPLACE FUNCTION get_group_by_moderator(p_moderator int)
RETURNS TABLE (
  "groupId" int, "groupName" text, "description" text, "moderator" int, "status" text,
  "groupCode" text, "createdDate" text
) LANGUAGE sql AS $$
  SELECT g."groupId", g."groupName", g."description", g."moderator", g."status",
        g."groupCode", g."createdDate"
    FROM "groups" g
   WHERE g."moderator" = p_moderator
   ORDER BY g."groupId"
   LIMIT 1;
$$;

-- Moderator check
CREATE OR REPLACE FUNCTION does_group_have_moderator(p_group_id int)
RETURNS boolean LANGUAGE sql AS $$
  SELECT EXISTS (
    SELECT 1
      FROM user_roles ur
      JOIN roles r  ON ur."roleId" = r."roleId"
      JOIN group_members gm ON ur."userId" = gm."userId"
     WHERE gm."groupId" = p_group_id
       AND r."roleCode" = 'MODERATOR'
       AND ur."status"  = 'ACTIVE'
     LIMIT 1
  )
$$;
-- ============ MEMBERS (group) ============

-- Raw members (DTO)
CREATE OR REPLACE FUNCTION get_group_members(p_group_id int)
RETURNS TABLE (
  "groupMemberId" int, "groupId" int, "userId" int, "joiningDate" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId", gm."groupId", gm."userId", gm."joiningDate"
    FROM group_members gm
   WHERE gm."groupId" = p_group_id
   ORDER BY gm."groupMemberId";
$$;

-- Add member -> id
CREATE OR REPLACE FUNCTION add_group_member(p_user_id int, p_group_id int, p_joining_date date)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO group_members("userId","groupId","joiningDate")
  VALUES (p_user_id, p_group_id, p_joining_date)
  ON CONFLICT ("userId","groupId") DO UPDATE
     SET "joiningDate" = EXCLUDED."joiningDate"
  RETURNING "groupMemberId" INTO v_id;
  IF v_id IS NULL THEN
    SELECT "groupMemberId" INTO v_id
      FROM group_members
     WHERE "userId" = p_user_id AND "groupId" = p_group_id;
  END IF;
  RETURN v_id;
END $$;

-- Update member (by id primarily; else composite)
CREATE OR REPLACE FUNCTION update_group_member(
  p_group_member_id int,
  p_user_id int,
  p_group_id int,
  p_joining_date text
) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF p_group_member_id IS NOT NULL THEN
    UPDATE group_members
       SET "userId"      = COALESCE(p_user_id, "userId"),
           "groupId"     = COALESCE(p_group_id, "groupId"),
           "joiningDate" = COALESCE(p_joining_date, "joiningDate")
     WHERE "groupMemberId" = p_group_member_id;
  ELSE
    UPDATE group_members
       SET "joiningDate" = COALESCE(p_joining_date, "joiningDate")
     WHERE "userId" = p_user_id AND "groupId" = p_group_id;
  END IF;
END $$;

-- Delete member -> boolean
CREATE OR REPLACE FUNCTION delete_group_member(
  p_group_member_id int,
  p_user_id int,
  p_group_id int
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  IF p_group_member_id IS NOT NULL THEN
    DELETE FROM group_members WHERE "groupMemberId" = p_group_member_id;
  ELSE
    DELETE FROM group_members WHERE "userId" = p_user_id AND "groupId" = p_group_id;
  END IF;
  RETURN FOUND;
END $$;

-- Members with names (DTO)
CREATE OR REPLACE FUNCTION get_group_members_with_names(p_group_id int)
RETURNS TABLE (
  "groupMemberId" int, "groupId" int, "userId" int,"firstName" text, "lastName" text,"joiningDate" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId",
         gm."groupId",
         gm."userId",
         u."firstName",
         u."lastName",
         gm."joiningDate"
    FROM group_members gm
    JOIN users u ON u."userId" = gm."userId"
   WHERE gm."groupId" = p_group_id
   ORDER BY gm."groupMemberId";
$$;

-- Exists?
CREATE OR REPLACE FUNCTION group_member_exists(p_user_id int, p_group_id int)
RETURNS boolean LANGUAGE sql AS $$
  SELECT EXISTS(
    SELECT 1 FROM group_members
     WHERE "userId" = p_user_id AND "groupId" = p_group_id
  );
$$;

-- Count
CREATE OR REPLACE FUNCTION count_group_members(p_group_id int)
RETURNS int LANGUAGE sql AS $$
  SELECT COUNT(*)::int FROM group_members WHERE "groupId" = p_group_id;
$$;

-- ============ MEMBERS (fund-based) ============

-- Members with names for fund
CREATE OR REPLACE FUNCTION get_members_with_names_for_fund(p_fund_id int)
RETURNS TABLE (
  "groupMemberId" int, "groupId" int, "userId" int, "userName" text, "joiningDate" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId",
         g."groupId",
         gm."userId",
         CASE
           WHEN u."lastName" IS NULL OR u."lastName" = ''
           THEN u."firstName"
           ELSE u."firstName" || ' ' || u."lastName"
         END AS "userName",
         gm."joiningDate"
    FROM "funds" f
    JOIN "groups" g      ON g."groupId" = f."groupId"
    JOIN group_members gm ON gm."groupId" = g."groupId"
    JOIN users u          ON u."userId" = gm."userId"
   WHERE f."fundId" = p_fund_id
   ORDER BY gm."groupMemberId";
$$;

-- Raw members for fund
CREATE OR REPLACE FUNCTION get_group_members_for_fund(p_fund_id int)
RETURNS TABLE (
  "groupMemberId" int, "groupId" int, "userId" int, "joiningDate" text
) LANGUAGE sql AS $$
  SELECT gm."groupMemberId", gm."groupId", gm."userId", gm."joiningDate"
    FROM "funds" f
    JOIN group_members gm ON gm."groupId" = f."groupId"
   WHERE f."fundId" = p_fund_id
   ORDER BY gm."groupMemberId";
$$;