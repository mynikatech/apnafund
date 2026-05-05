CREATE OR REPLACE FUNCTION get_groups_with_moderator_info()
RETURNS TABLE (
  "groupId" int,
  "groupName" text,
  "description" text,
  "moderator" int,
  "moderatorName" text,
  "moderatorEmail" text,
  "status" text,
  "createdDate" text,
  "groupCode" text
)
LANGUAGE sql AS $$
  SELECT g."groupId",
         g."groupName",
         g."description",
         g."moderator",
        TRIM(
          COALESCE(u."firstName", '') || ' ' || COALESCE(u."lastName", '')
        ) AS "moderatorName",
         u."emailId" AS "moderatorEmail",
         g."status",
         g."createdDate",
         g."groupCode"
  FROM "groups" g
  LEFT JOIN users u ON u."userId" = g."moderator"
  ORDER BY g."createdDate" DESC;
$$;

CREATE OR REPLACE FUNCTION get_groups_for_moderator_with_moderator_info(p_user_id int)
RETURNS TABLE (
  "groupId" int,
  "groupName" text,
  "moderator" int,
  "moderatorName" text,
  "moderatorEmail" text,
  "createdDate" text,
  "description" text,
  "groupCode" text,
  "status" text
)
LANGUAGE sql AS $$
  SELECT g."groupId",
         g."groupName",
         g."moderator",
         TRIM(
          COALESCE(u."firstName", '') || ' ' || COALESCE(u."lastName", '')
         ) AS "moderatorName",
         u."emailId" AS "moderatorEmail",
         g."createdDate",
         g."description",
         g."groupCode",
         g."status"
  FROM group_members gm
  JOIN "groups" g ON g."groupId" = gm."groupId"
  LEFT JOIN users u ON u."userId" = g."moderator"
  WHERE gm."userId" = p_user_id
    AND g."moderator" = p_user_id
  ORDER BY gm."joiningDate" DESC;
$$;