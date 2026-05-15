DROP FUNCTION IF EXISTS add_group_member(INT, INT, TEXT,TEXT, INT, TEXT);
DROP FUNCTION IF EXISTS activate_group_memberships(INT, INT);

CREATE OR REPLACE FUNCTION add_group_member(
    p_user_id INT,
    p_group_id INT,
    p_joining_date TEXT,
    p_role TEXT,
    p_actor_user_id INT,
    p_status TEXT DEFAULT 'ACTIVE'
)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id INT;
BEGIN

    INSERT INTO group_members(
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
        p_role::"memberRole",
        p_status::"memberStatus"
    )

    ON CONFLICT ("userId","groupId")

    DO UPDATE SET

        -- Reactivate/update status
        "status" = p_status::"memberStatus",

        -- Preserve primary moderator
        "role" = CASE
            WHEN group_members."role" = 'PRIMARY_MODERATOR'::"memberRole"
                THEN group_members."role"
            ELSE EXCLUDED."role"
        END,

        "joiningDate" = EXCLUDED."joiningDate",

        "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,

        "updatedBy" = p_actor_user_id

    RETURNING "groupMemberId"
    INTO v_id;

    RETURN v_id;

END;
$$;

CREATE OR REPLACE FUNCTION activate_group_memberships(
    p_group_id INT,
    p_actor_user_id INT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_updated_count INTEGER;
BEGIN

    UPDATE group_members
    SET
        "status" = 'ACTIVE'::"memberStatus",

        "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,

        "updatedBy" = p_actor_user_id

    WHERE
        "groupId" = p_group_id

        AND "role" IN (
            'PRIMARY_MODERATOR'::"memberRole",
            'MODERATOR'::"memberRole"
        )

        AND "status" <> 'ACTIVE'::"memberStatus";

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    RETURN v_updated_count;

END;
$$;


