-- roles filter
CREATE INDEX IF NOT EXISTS idx_roles_status ON roles("status");

-- MOST IMPORTANT
CREATE INDEX IF NOT EXISTS idx_role_privilege_status_role
ON role_privilege("status", "roleId");

-- optional (if privilege table large)
CREATE INDEX IF NOT EXISTS idx_privilege_status
ON privilege("status");