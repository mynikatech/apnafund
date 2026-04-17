-- roles filter
CREATE INDEX idx_roles_status ON roles("status");

-- MOST IMPORTANT
CREATE INDEX idx_role_privilege_status_role
ON role_privilege("status", "roleId");

-- optional (if privilege table large)
CREATE INDEX idx_privilege_status
ON privilege("status");