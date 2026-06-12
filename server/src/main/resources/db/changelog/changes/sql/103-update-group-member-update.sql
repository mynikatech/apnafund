DROP FUNCTION IF EXISTS update_group_member(INT, INT,INT,TEXT, TEXT, TEXT, INT);

CREATE OR REPLACE FUNCTION update_group_member(
  p_group_member_id int,
  p_user_id int,
  p_group_id int,
  p_joining_date text,
  p_role text DEFAULT NULL,
  p_status text DEFAULT NULL,
  p_actor_user_id int DEFAULT NULL
)
RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
  v_rows_updated int;
  v_user_id INT;
  v_group_id INT;
BEGIN
        -- Resolve actual values safely
        IF p_group_member_id IS NOT NULL THEN

            SELECT
                COALESCE(p_user_id, gm."userId"),
                COALESCE(p_group_id, gm."groupId")
            INTO
                v_user_id,
                v_group_id
            FROM group_members gm
            WHERE gm."groupMemberId" = p_group_member_id;

        ELSE

            v_user_id := p_user_id;
            v_group_id := p_group_id;

        END IF;

        -- Validation
        IF p_status = 'INACTIVE' THEN

            IF EXISTS (

                SELECT 1
                FROM fund_members fm
                JOIN funds f
                    ON f."fundId" = fm."fundId"

                WHERE fm."userId" = v_user_id
                  AND f."groupId" = v_group_id
                  AND fm."status" = 'ACTIVE'::"memberStatus"
                  AND f."fundStatus" = 'ACTIVE'

            ) THEN

                RAISE EXCEPTION
                    USING
                        ERRCODE = 'AP001',
                        MESSAGE =
                            'GROUP_MEMBER_ACTIVE_IN_FUNDS';

            END IF;

        END IF;
  IF p_group_member_id IS NOT NULL THEN

    UPDATE "group_members"
       SET "userId"      = COALESCE(p_user_id, "userId"),
           "groupId"     = COALESCE(p_group_id, "groupId"),
           "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           -- Role update (protect PRIMARY_MODERATOR)
           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' THEN "role"
               ELSE COALESCE(p_role::"memberRole", "role")
           END,

           -- Status update (cannot deactivate PRIMARY_MODERATOR)
           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status::"memberStatus", "status")
           END,

           -- Audit
           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "groupMemberId" = p_group_member_id;

  ELSE

    UPDATE "group_members"
       SET "joiningDate" = COALESCE(p_joining_date, "joiningDate"),

           "role" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' THEN "role"
               ELSE COALESCE(p_role::"memberRole", "role")
           END,

           "status" = CASE
               WHEN "role" = 'PRIMARY_MODERATOR' AND p_status = 'INACTIVE'
                   THEN "status"
               ELSE COALESCE(p_status::"memberStatus", "status")
           END,

           "updatedAt" = EXTRACT(EPOCH FROM NOW()) * 1000,
           "updatedBy" = COALESCE(p_actor_user_id, "updatedBy")

     WHERE "userId" = p_user_id
       AND "groupId" = p_group_id;

  END IF;

  GET DIAGNOSTICS v_rows_updated = ROW_COUNT;

  RETURN v_rows_updated > 0;

END $$;