CREATE INDEX IF NOT EXISTS idx_uph_user ON user_password_history("userId");
CREATE INDEX IF NOT EXISTS idx_uph_changedat ON user_password_history("changedAt");

-- Insert a history row and return its id
CREATE OR REPLACE FUNCTION add_user_password_history(
  p_user_id int,
  p_password_hash text
) RETURNS int
LANGUAGE plpgsql
AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_password_history("userId","passwordHash")
  VALUES (p_user_id, p_password_hash)
  RETURNING "id" INTO v_id;
  RETURN v_id;
END $$;

-- Return last 3 password hashes (newest first)
CREATE OR REPLACE FUNCTION get_last3_password_hashes(
  p_user_id int
) RETURNS SETOF text
LANGUAGE sql
AS $$
  SELECT h."passwordHash"
    FROM user_password_history h
   WHERE h."userId" = p_user_id
   ORDER BY h."changedAt" DESC
   LIMIT 3;
$$;

-- Return last change as epoch milliseconds
CREATE OR REPLACE FUNCTION get_last_password_change_ms(p_user_id int)
RETURNS bigint
LANGUAGE sql
AS $$
  SELECT h."changedAt"
  FROM user_password_history h
  WHERE h."userId" = p_user_id
  ORDER BY h."changedAt" DESC
  LIMIT 1;
$$;