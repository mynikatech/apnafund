DROP FUNCTION IF EXISTS get_user_by_phone_and_group_code(TEXT, TEXT);

CREATE OR REPLACE FUNCTION get_user_by_phone_and_group_code(
    p_phone TEXT,
    p_group_code TEXT
)
RETURNS TABLE (
    "userId" INT,
    "firstName" TEXT,
    "lastName" TEXT,
    "emailId" TEXT,
    "phoneNumber" TEXT,
    status TEXT,
    "passwordHash" TEXT,
    "createdDate" TEXT,
    "isPinSet" BOOLEAN,
    "hashPIN" TEXT,
    "firebaseUserId" TEXT,
    "emailVerified" BOOLEAN,
    "emailVerifiedAt" TEXT,
    "userCode" TEXT,
    "isInvited" BOOLEAN,
    "createdAt" BIGINT,
    "createdByUserId" INT,
    "userSaveSource" TEXT,
    "updatedByUserId" INT,
    "updatedAt" BIGINT,
    "createdByName" TEXT
)
LANGUAGE sql
AS $$
SELECT
    u."userId",
    u."firstName",
    u."lastName",
    u."emailId",
    u."phoneNumber",
    u.status,
    u."passwordHash",
    u."createdDate",
    u."isPinSet",
    u."hashPIN",
    u."firebaseUserId",
    u.email_verified AS "emailVerified",
    u.email_verified_at::TEXT AS "emailVerifiedAt",
    u."userCode",
    u."isInvited",
    u."createdAt",
    u."createdByUserId",
    u."userSaveSource",
    u."updatedByUserId",
    u."updatedAt",
    COALESCE(
        creator."firstName" || ' ' || creator."lastName",
        'Self'
    ) AS "createdByName"
FROM users u
JOIN group_members gm
    ON gm."userId" = u."userId"
JOIN groups g
    ON g."groupId" = gm."groupId"
LEFT JOIN users creator
    ON u."createdByUserId" = creator."userId"
WHERE u."phoneNumber" = p_phone
  AND g."groupCode" = p_group_code
  AND u."isInvited" = true
LIMIT 1;
$$;

