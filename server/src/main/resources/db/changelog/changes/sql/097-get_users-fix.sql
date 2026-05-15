DROP FUNCTION IF EXISTS get_users();
DROP FUNCTION IF EXISTS get_user(INT);
DROP FUNCTION IF EXISTS get_users_for_moderator(p_user_id INT);

CREATE OR REPLACE FUNCTION get_users()
RETURNS TABLE (
  "userId" INT,
  "firstName" TEXT,
  "lastName" TEXT,
  "emailId" TEXT,
  "phoneNumber" TEXT,
  "status" TEXT,
  "passwordHash" TEXT,
  "createdDate" TEXT,
  "isPinSet" BOOLEAN,
  "hashPIN" TEXT,
  "firebaseUserId" TEXT,
  "userCode" TEXT,
  "isInvited" BOOLEAN,
  "createdAt" BIGINT,
  "createdByUserId" INT,
  "userSaveSource" TEXT,
  "updatedByUserId" INT,
  "updatedAt" BIGINT,
  "createdByName" TEXT,
  "emailVerified" BOOLEAN,
  "emailVerifiedAt" TEXT
)
LANGUAGE sql AS $$
SELECT
    u."userId",
    u."firstName",
    u."lastName",
    u."emailId",
    u."phoneNumber",
    u."status",
    u."passwordHash",
    u."createdDate",
    u."isPinSet",
    u."hashPIN",
    u."firebaseUserId",
    u."userCode",
    u."isInvited",
    u."createdAt",
    u."createdByUserId",
    u."userSaveSource",
    u."updatedByUserId",
    u."updatedAt",
    (creator."firstName" || ' ' || creator."lastName") AS "createdByName",
    u."email_verified" AS "emailVerified",
    u."email_verified_at" AS "emailVerifiedAt"
FROM users u
LEFT JOIN users creator
    ON u."createdByUserId" = creator."userId";
$$;

CREATE OR REPLACE FUNCTION get_user(p_user_id INT)
RETURNS TABLE (
  "userId" INT,
  "firstName" TEXT,
  "lastName" TEXT,
  "emailId" TEXT,
  "phoneNumber" TEXT,
  "status" TEXT,
  "passwordHash" TEXT,
  "createdDate" TEXT,
  "isPinSet" BOOLEAN,
  "hashPIN" TEXT,
  "firebaseUserId" TEXT,
  "userCode" TEXT,
  "isInvited" BOOLEAN,
  "createdAt" BIGINT,
  "createdByUserId" INT,
  "userSaveSource" TEXT,
  "updatedByUserId" INT,
  "updatedAt" BIGINT,
  "createdByName" TEXT,
  "emailVerified" BOOLEAN,
  "emailVerifiedAt" TEXT
)
LANGUAGE sql AS $$
SELECT
    u."userId",
    u."firstName",
    u."lastName",
    u."emailId",
    u."phoneNumber",
    u."status",
    u."passwordHash",
    u."createdDate",
    u."isPinSet",
    u."hashPIN",
    u."firebaseUserId",
    u."userCode",
    u."isInvited",
    u."createdAt",
    u."createdByUserId",
    u."userSaveSource",
    u."updatedByUserId",
    u."updatedAt",
    (creator."firstName" || ' ' || creator."lastName") AS "createdByName",
    u."email_verified" AS "emailVerified",
    u."email_verified_at" AS "emailVerifiedAt"
FROM users u
LEFT JOIN users creator
    ON u."createdByUserId" = creator."userId"
WHERE u."userId" = p_user_id;
$$;

CREATE OR REPLACE FUNCTION get_users_for_moderator(p_user_id INT)
RETURNS TABLE (
  "userId" INT,
  "firstName" TEXT,
  "lastName" TEXT,
  "emailId" TEXT,
  "phoneNumber" TEXT,
  "status" TEXT,
  "passwordHash" TEXT,
  "createdDate" TEXT,
  "isPinSet" BOOLEAN,
  "hashPIN" TEXT,
  "firebaseUserId" TEXT,
  "userCode" TEXT,
  "isInvited" BOOLEAN,
  "createdAt" BIGINT,
  "createdByUserId" INT,
  "userSaveSource" TEXT,
  "updatedByUserId" INT,
  "updatedAt" BIGINT,
  "createdByName" TEXT,
  "emailVerified" BOOLEAN,
  "emailVerifiedAt" TEXT
)
LANGUAGE sql AS $$

SELECT DISTINCT
    u."userId",
    u."firstName",
    u."lastName",
    u."emailId",
    u."phoneNumber",
    u."status",
    u."passwordHash",
    u."createdDate",
    u."isPinSet",
    u."hashPIN",
    u."firebaseUserId",
    u."userCode",
    u."isInvited",
    u."createdAt",
    u."createdByUserId",
    u."userSaveSource",
    u."updatedByUserId",
    u."updatedAt",
    (creator."firstName" || ' ' || creator."lastName") AS "createdByName",
    u."email_verified" AS "emailVerified",
    u."email_verified_at" AS "emailVerifiedAt"

FROM users u
JOIN group_members gm ON gm."userId" = u."userId"
LEFT JOIN users creator
    ON u."createdByUserId" = creator."userId"

WHERE gm."groupId" IN (
    SELECT g."groupId"
    FROM "groups" g
    WHERE g."moderator" = p_user_id
);

$$;