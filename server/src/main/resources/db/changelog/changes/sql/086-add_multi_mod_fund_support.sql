
DROP FUNCTION IF EXISTS get_fund_members(INT);
DROP FUNCTION IF EXISTS add_fund_member(INT, INT, TEXT);
DROP FUNCTION IF EXISTS add_fund_members_batch(JSONB);
DROP FUNCTION IF EXISTS get_fund_members_with_names_for_fund(INT);
DROP FUNCTION IF EXISTS update_fund_member(INT, INT, INT, TEXT, TEXT,TEXT, INT );
CREATE OR REPLACE FUNCTION get_fund_members(p_fund_id int)
RETURNS TABLE (
  "fundMemberId" int,
  "userId" int,
  "fundId" int,
  "joiningDate" text,
  "role" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT fm."fundMemberId",
         fm."userId",
         fm."fundId",
         fm."joiningDate",
         fm."role"::text,
         fm."status"::text
    FROM fund_members fm
   WHERE fm."fundId" = p_fund_id
   ORDER BY fm."fundMemberId";
$$;

CREATE OR REPLACE FUNCTION add_fund_member(
    p_user_id int,
    p_fund_id int,
    p_joining_date text,
    p_role text DEFAULT 'MEMBER',
    p_actor_user_id int DEFAULT NULL
)
RETURNS int
LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO fund_members(
      "userId",
      "fundId",
      "joiningDate",
      "role",
      "status"
  )
  VALUES (
      p_user_id,
      p_fund_id,
      p_joining_date,
      p_role::"memberRole",
      'ACTIVE'::"memberStatus"
  )
  ON CONFLICT ("userId","fundId")
  DO UPDATE SET
      -- Reactivate if inactive
      "status" = 'ACTIVE'::"memberStatus",

      -- Don't override PRIMARY_MODERATOR
      "role" = CASE
          WHEN fund_members."role" = 'PRIMARY_MODERATOR'::"memberRole"
              THEN fund_members."role"
          ELSE EXCLUDED."role"
      END,

      -- Update joining date
      "joiningDate" = COALESCE(EXCLUDED."joiningDate", fund_members."joiningDate"),

      -- Audit
      "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
      "updatedBy" = COALESCE(p_actor_user_id, fund_members."updatedBy")

  RETURNING "fundMemberId" INTO v_id;

  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION add_fund_members_batch(p_members jsonb)
RETURNS SETOF int
LANGUAGE plpgsql AS $$
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
      NULLIF(rec->>'joiningDate',''),
      COALESCE(rec->>'role','MEMBER'),
      (rec->>'actorUserId')::int
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
  "lastName"     text,
  "role"         text,
  "status"       text
)
LANGUAGE sql STABLE AS $$
  SELECT
    fm."fundMemberId",
    fm."userId",
    fm."fundId",
    fm."joiningDate",
    u."firstName",
    COALESCE(u."lastName", '') AS "lastName",
    fm."role"::text,
    fm."status"::text
  FROM fund_members fm
  JOIN users u ON u."userId" = fm."userId"
  WHERE fm."fundId" = p_fund_id
    AND fm."status" = 'ACTIVE'::"memberStatus"
  ORDER BY u."firstName", u."lastName";
$$;

CREATE OR REPLACE FUNCTION update_fund_member(
  p_fund_member_id int,
  p_user_id int,
  p_fund_id int,
  p_joining_date text,
  p_role text DEFAULT NULL,
  p_status text DEFAULT NULL,
  p_actor_user_id int DEFAULT NULL
)
RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
  v_rows_updated int;
BEGIN

  IF p_fund_member_id IS NOT NULL THEN

    UPDATE "fund_members"
       SET "userId"      = COALESCE(p_user_id, "userId"),
           "fundId"      = COALESCE(p_fund_id, "fundId"),
           "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           -- 🔥 Role update (protect PRIMARY_MODERATOR)
           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR'::"memberRole"
                   THEN "role"
               ELSE COALESCE(p_role::"memberRole", "role")
           END,

           -- 🔥 Status update (cannot deactivate PRIMARY_MODERATOR)
           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR'::"memberRole"
                    AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status::"memberStatus", "status")
           END,

           -- 🔥 Audit
           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "fundMemberId" = p_fund_member_id;

  ELSE

    UPDATE "fund_members"
       SET "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR'::"memberRole"
                   THEN "role"
               ELSE COALESCE(p_role::"memberRole", "role")
           END,

           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR'::"memberRole"
                    AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status::"memberStatus", "status")
           END,

           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "userId" = p_user_id
       AND "fundId" = p_fund_id;

  END IF;

  GET DIAGNOSTICS v_rows_updated = ROW_COUNT;

  RETURN v_rows_updated > 0;

END $$;