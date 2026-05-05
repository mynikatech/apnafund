DROP FUNCTION IF EXISTS get_user_by_email(TEXT);

DROP FUNCTION IF EXISTS get_user_by_phone(TEXT);

CREATE OR REPLACE FUNCTION get_user_by_email(p_email TEXT)
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
    "isInvited" BOOLEAN
)
LANGUAGE sql
AS $$
SELECT
    "userId",
    "firstName",
    "lastName",
    "emailId",
    "phoneNumber",
    status,
    "passwordHash",
    "createdDate",
    "isPinSet",
    "hashPIN",
    "firebaseUserId",
    email_verified AS "emailVerified",
    email_verified_at::TEXT AS "emailVerifiedAt",
    "userCode",
    "isInvited"
FROM users
WHERE "emailId" = p_email
LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION get_user_by_phone(p_phone TEXT)
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
    "isInvited" BOOLEAN
)
LANGUAGE sql
AS $$
SELECT
    "userId",
    "firstName",
    "lastName",
    "emailId",
    "phoneNumber",
    status,
    "passwordHash",
    "createdDate",
    "isPinSet",
    "hashPIN",
    "firebaseUserId",
    email_verified AS "emailVerified",
    email_verified_at::TEXT AS "emailVerifiedAt",
    "userCode",
    "isInvited"
FROM users
WHERE "phoneNumber" = p_phone
LIMIT 1;
$$;