CREATE OR REPLACE FUNCTION activate_group(
    p_group_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE groups
    SET
        "status" = 'ACTIVE'
    WHERE "groupId" = p_group_id;

    RETURN TRUE;

END;
$$;

CREATE OR REPLACE FUNCTION reject_group(
    p_group_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    UPDATE groups
    SET
        "status" = 'REJECTED'
    WHERE "groupId" = p_group_id;

    RETURN TRUE;

END;
$$;