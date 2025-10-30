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