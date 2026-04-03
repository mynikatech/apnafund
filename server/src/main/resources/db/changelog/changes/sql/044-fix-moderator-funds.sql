UPDATE roles
SET "roleCode" = 'GROUP_MODERATOR'
WHERE "roleCode" = 'MODERATOR'
  AND NOT EXISTS (
      SELECT 1 FROM roles WHERE "roleCode" = 'GROUP_MODERATOR'
  );

INSERT INTO roles ("roleCode", "roleDescription", "status")
SELECT 'GROUP_MODERATOR', 'Group Moderator', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE "roleCode" = 'GROUP_MODERATOR'
);

INSERT INTO user_roles ("userId", "roleId", "status")
SELECT DISTINCT f."moderator",
       r."roleId",
       'ACTIVE'
FROM funds f
JOIN roles r ON r."roleCode" = 'FUND_MODERATOR'
WHERE f."moderator" IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur."userId" = f."moderator"
      AND ur."roleId" = r."roleId"
);

INSERT INTO user_roles ("userId", "roleId", "status")
SELECT DISTINCT g."moderator",
       r."roleId",
       'ACTIVE'
FROM groups g
JOIN roles r ON r."roleCode" = 'GROUP_MODERATOR'
WHERE g."moderator" IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur."userId" = g."moderator"
      AND ur."roleId" = r."roleId"
);

CREATE OR REPLACE FUNCTION upsert_user_role_by_code(
    p_user_id INT,
    p_role_code TEXT,
    p_status TEXT DEFAULT 'ACTIVE'
)
RETURNS BOOLEAN AS $$
DECLARE
    v_role_id INT;
    v_user_role_id INT;
BEGIN
    -- Get role_id
    SELECT "roleId" INTO v_role_id
    FROM roles
    WHERE "roleCode" = p_role_code;

    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Role not found: %', p_role_code;
    END IF;

    -- Check if role already exists
    SELECT "userRoleId" INTO v_user_role_id
    FROM user_roles
    WHERE "userId" = p_user_id
      AND "roleId"= v_role_id;

    IF v_user_role_id IS NOT NULL THEN
        -- Use your existing function
        PERFORM update_user_role(v_user_role_id, p_status);
    ELSE
        -- Insert new role
        INSERT INTO user_roles ("userId", "roleId", "status")
        VALUES (p_user_id, v_role_id, p_status);
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION get_roles_of_user(p_user_id int)
RETURNS TABLE (
  "roleId" int,
  "roleCode" text,
  "roleDescription" text,
  "status" text
) LANGUAGE sql AS $$
  SELECT r."roleId", r."roleCode", r."roleDescription", ur."status"
    FROM user_roles ur
    JOIN roles r ON r."roleId" = ur."roleId"
   WHERE ur."userId" = p_user_id
     AND ur."status" = 'ACTIVE'   -- 🔥 FILTER ADDED
   ORDER BY r."roleId";
$$;