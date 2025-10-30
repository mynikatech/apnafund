
CREATE OR REPLACE FUNCTION get_roles()
RETURNS SETOF roles LANGUAGE sql AS $$
  SELECT * FROM roles ORDER BY "roleId"
$$;

CREATE OR REPLACE FUNCTION get_role(p_id int)
RETURNS SETOF roles LANGUAGE sql AS $$
  SELECT * FROM roles WHERE "roleId" = p_id LIMIT 1
$$;

CREATE OR REPLACE FUNCTION get_role_id_by_code(p_code text)
RETURNS int LANGUAGE sql AS $$
  SELECT r."roleId" FROM roles r WHERE r."roleCode" = p_code LIMIT 1
$$;

CREATE OR REPLACE FUNCTION create_role(p_code text, p_desc text, p_status text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO roles("roleCode","roleDescription","status")
  VALUES (p_code,p_desc,p_status)
  RETURNING "roleId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_role(p_id int, p_code text, p_desc text, p_status text)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE roles
     SET "roleCode" = p_code,
         "roleDescription" = p_desc,
         "status" = p_status
   WHERE "roleId" = p_id;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION delete_role(p_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM roles WHERE "roleId" = p_id;
  RETURN FOUND;
END $$;

-- --------- PRIVILEGES ----------
CREATE OR REPLACE FUNCTION get_privileges()
RETURNS SETOF privilege LANGUAGE sql AS $$
  SELECT * FROM privilege ORDER BY "privilegeId"
$$;

CREATE OR REPLACE FUNCTION get_privilege(p_id int)
RETURNS SETOF privilege LANGUAGE sql AS $$
  SELECT * FROM privilege WHERE "privilegeId" = p_id LIMIT 1
$$;

CREATE OR REPLACE FUNCTION create_privilege(p_code text, p_desc text, p_status text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO privileges("privilegeCode","privilegeDescription","status")
  VALUES (p_code,p_desc,p_status)
  RETURNING "privilegeId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION update_privilege(p_id int, p_code text, p_desc text, p_status text)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE privilege
     SET "privilegeCode" = p_code,
         "privilegeDescription" = p_desc,
         "status" = p_status
   WHERE "privilegeId" = p_id;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION delete_privilege(p_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM privilege WHERE "privilegeId" = p_id;
  RETURN FOUND;
END $$;

-- --------- ROLE ↔ PRIVILEGE ----------
CREATE OR REPLACE FUNCTION get_privileges_of_role(p_role_id int)
RETURNS TABLE(
  "privilegeId" int,
  "privilegeCode" text,
  "privilegeDescription" text,
  "status" text
) LANGUAGE sql AS $$
  SELECT p."privilegeId", p."privilegeCode", p."privilegeDescription", p."status"
    FROM role_privilege rp
    JOIN privilege p ON p."privilegeId" = rp."privilegeId"
   WHERE rp."roleId" = p_role_id
   ORDER BY p."privilegeCode"
$$;

CREATE OR REPLACE FUNCTION create_role_privilege(p_role_id int, p_privilege_id int, p_status text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO role_privilege("roleId","privilegeId","status")
  VALUES (p_role_id, p_privilege_id, p_status)
  RETURNING "rolePrivilegeId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION delete_role_privilege(p_role_privilege_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM role_privilege WHERE "rolePrivilegeId" = p_role_privilege_id;
  RETURN FOUND;
END $$;

-- --------- USER ↔ ROLES (assign) ----------
CREATE OR REPLACE FUNCTION add_user_role(p_user_id int, p_role_id int, p_status text)
RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_roles("userId","roleId","status")
  VALUES (p_user_id, p_role_id, p_status)
  RETURNING "userRoleId" INTO v_id;
  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION remove_user_role(p_user_role_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM user_roles WHERE "userRoleId" = p_user_role_id;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION get_all_roles_with_privilege()
RETURNS TABLE(
  "roleId"     int,
  "roleCode"   text,
  "privilegeCode"  text
)
LANGUAGE sql STABLE AS $$
      SELECT r."roleId", r."roleCode", p."privilegeCode"
      FROM roles r
      INNER JOIN role_privilege rp ON r."roleId" = rp."roleId"
      INNER JOIN privilege p ON rp."privilegeId" = p."privilegeId"
      WHERE r."status" = 'ACTIVE' AND rp."status" = 'ACTIVE' AND p."status" = 'ACTIVE'
$$;








