DROP FUNCTION IF EXISTS get_group_members(INT);
DROP FUNCTION IF EXISTS add_group_member(INT, INT, DATE);
DROP FUNCTION IF EXISTS update_group_member(INT, INT,INT,TEXT);
DROP FUNCTION IF EXISTS get_group_members_for_fund(INT);

CREATE OR REPLACE FUNCTION get_group_members(p_group_id int)
RETURNS TABLE (
  "groupMemberId" int,
  "groupId" int,
  "userId" int,
  "joiningDate" text,
  "role" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT
    gm."groupMemberId",
    gm."groupId",
    gm."userId",
    gm."joiningDate",
    gm."role",
    gm."status"
  FROM "group_members" gm
  WHERE gm."groupId" = p_group_id
  ORDER BY
    CASE gm."status"
        WHEN 'ACTIVE' THEN 1
        ELSE 2
    END,
    gm."groupMemberId";
$$;

CREATE OR REPLACE FUNCTION add_group_member(
    p_user_id int,
    p_group_id int,
    p_joining_date date,
    p_role text,
    p_actor_user_id int
)
RETURNS int
LANGUAGE plpgsql AS $$
DECLARE
    v_id int;
BEGIN

  INSERT INTO "group_members"(
      "userId",
      "groupId",
      "joiningDate",
      "role",
      "status"
  )
  VALUES (
      p_user_id,
      p_group_id,
      p_joining_date,
      p_role,
      'ACTIVE'
  )
  ON CONFLICT ("userId","groupId")
  DO UPDATE SET
      -- Reactivate if inactive
      "status" = 'ACTIVE',

      -- Update role (but don't override PRIMARY_MODERATOR)
      "role" = CASE
          WHEN "group_members"."role" = 'PRIMARY_MODERATOR'
              THEN "group_members"."role"
          ELSE EXCLUDED."role"
      END,

      -- Update joining date
      "joiningDate" = EXCLUDED."joiningDate",

      -- Audit
      "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
      "updatedBy" = p_actor_user_id

  RETURNING "groupMemberId" INTO v_id;

  RETURN v_id;

END $$;


CREATE OR REPLACE FUNCTION update_group_member(
  p_group_member_id int,
  p_user_id int,
  p_group_id int,
  p_joining_date date,
  p_role text DEFAULT NULL,
  p_status text DEFAULT NULL,
  p_actor_user_id int DEFAULT NULL
)
RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
  v_rows_updated int;
BEGIN

  IF p_group_member_id IS NOT NULL THEN

    UPDATE "group_members"
       SET "userId"      = COALESCE(p_user_id, "userId"),
           "groupId"     = COALESCE(p_group_id, "groupId"),
           "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           -- Role update (protect PRIMARY_MODERATOR)
           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' THEN "role"
               ELSE COALESCE(p_role, "role")
           END,

           -- Status update (cannot deactivate PRIMARY_MODERATOR)
           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status, "status")
           END,

           -- Audit
           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "groupMemberId" = p_group_member_id;

  ELSE

    UPDATE "group_members"
       SET "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' THEN "role"
               ELSE COALESCE(p_role, "role")
           END,

           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status, "status")
           END,

           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "userId" = p_user_id
       AND "groupId" = p_group_id;

  END IF;

  GET DIAGNOSTICS v_rows_updated = ROW_COUNT;

  RETURN v_rows_updated > 0;

END $$;

CREATE OR REPLACE FUNCTION get_group_members_for_fund(p_fund_id int)
RETURNS TABLE (
  "groupMemberId" int,
  "groupId" int,
  "userId" int,
  "joiningDate" text,
  "role" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT
    gm."groupMemberId",
    gm."groupId",
    gm."userId",
    gm."joiningDate",
    gm."role",
    gm."status"
  FROM "funds" f
  JOIN "group_members" gm
    ON gm."groupId" = f."groupId"
  WHERE f."fundId" = p_fund_id
    AND gm."status" = 'ACTIVE'
  ORDER BY
    CASE gm."role"
      WHEN 'PRIMARY_MODERATOR' THEN 1
      WHEN 'MODERATOR' THEN 2
      ELSE 3
    END,
    gm."groupMemberId";
$$;