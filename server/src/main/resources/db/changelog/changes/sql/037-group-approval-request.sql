INSERT INTO "type" ("typeCode","typeDescription","status")
SELECT 'GROUP_REQUESTED', 'Group creation approval requested', 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM type WHERE "typeCode" = 'GROUP_REQUESTED'
);

DROP FUNCTION IF EXISTS add_group(text,text,int,text,text,text);

CREATE OR REPLACE FUNCTION add_group(
    p_group_name text,
    p_description text,
    p_moderator int,
    p_group_code text,
    p_created_date text,
    p_status text
)
RETURNS TABLE (
  "groupId" int,
  "groupName" text,
  "moderator" int,
  "createdDate" text,
  "description" text,
  "groupCode" text,
  "status" text
)
LANGUAGE sql
AS $$
    INSERT INTO "groups"(
        "groupName",
        "description",
        "moderator",
        "groupCode",
        "createdDate",
        "status"
    )
    VALUES (
        p_group_name,
        p_description,
        p_moderator,
        p_group_code,
        p_created_date,
        p_status
    )
    RETURNING
        "groupId",
        "groupName",
        "moderator",
        "createdDate",
        "description",
        "groupCode",
        "status";
$$;


CREATE OR REPLACE FUNCTION create_group_approval(
    p_group_id INT,
    p_requested_by INT,
    p_approver INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN

    INSERT INTO approval_requests(
        "entityType",
        "entityId",
        "requestedBy",
        "approverUserId"
    )
    VALUES (
        'GROUP',
        p_group_id,
        p_requested_by,
        p_approver
    );
    RETURN TRUE;

END;
$$;

CREATE OR REPLACE FUNCTION get_admin_user()
RETURNS TABLE (
    "userId" INT,
    "firstName" TEXT,
    "lastName" TEXT,
    "emailId" TEXT
)
LANGUAGE sql
AS $$
    SELECT
        u."userId",
        u."firstName",
        u."lastName",
        u."emailId"
    FROM users u
    JOIN user_roles ur ON ur."userId" = u."userId"
    JOIN roles r ON r."roleId" = ur."roleId"
    WHERE r."roleCode" = 'ADMIN'
      AND ur."status" = 'ACTIVE'
      LIMIT 1;
$$;