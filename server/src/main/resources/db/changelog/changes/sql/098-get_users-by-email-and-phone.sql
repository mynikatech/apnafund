DROP FUNCTION IF EXISTS get_user_by_email_and_phone(TEXT, TEXT);

CREATE OR REPLACE FUNCTION get_user_by_email_and_phone(
    p_email TEXT,
    p_phone TEXT
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
LEFT JOIN users creator
    ON u."createdByUserId" = creator."userId"
WHERE LOWER(u."emailId") = LOWER(p_email)
AND u."phoneNumber" = p_phone
AND u."isInvited" = true
LIMIT 1;
$$;