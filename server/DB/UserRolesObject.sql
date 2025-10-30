-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles("userId");
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles("roleId");
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_roles_user_role ON user_roles("userId","roleId");

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
