
-- Insert a history row and return its id
CREATE OR REPLACE FUNCTION add_user_pin_history(
  p_user_id  int,
  p_pin_hash text
) RETURNS int
LANGUAGE plpgsql
AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_pin_history("userId","pinHash")
  VALUES (p_user_id, p_pin_hash)
  RETURNING "id" INTO v_id;
  RETURN v_id;
END $$;

-- Return last 3 pin hashes (newest first)
CREATE OR REPLACE FUNCTION get_last3_pin_hashes(
  p_user_id int
) RETURNS SETOF text
LANGUAGE sql
AS $$
  SELECT h."pinHash"
    FROM user_pin_history h
   WHERE h."userId" = p_user_id
   ORDER BY h."changedAt" DESC
   LIMIT 3;
$$;

-- Return last change as epoch milliseconds (robust to bigint/timestamp variants)
-- If your "changedAt" is timestamptz (as defined above), this path is used.
CREATE OR REPLACE FUNCTION get_last_pin_change_ms(
  p_user_id int
) RETURNS bigint
  LANGUAGE sql
  AS $$
    SELECT h."changedAt"
    FROM user_pin_history h
    WHERE h."userId" = p_user_id
    ORDER BY h."changedAt" DESC
    LIMIT 1;
  $$;

