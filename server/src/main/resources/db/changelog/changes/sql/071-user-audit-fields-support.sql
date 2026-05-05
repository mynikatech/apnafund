ALTER TABLE "users"
ADD COLUMN IF NOT EXISTS "createdByUserId" INTEGER;

ALTER TABLE "users"
ADD COLUMN IF NOT EXISTS "userSaveSource" TEXT;

ALTER TABLE "users"
ADD COLUMN IF NOT EXISTS "createdAt" BIGINT;

ALTER TABLE "users"
ADD COLUMN IF NOT EXISTS "updatedByUserId" INTEGER;

ALTER TABLE "users"
ADD COLUMN IF NOT EXISTS "updatedAt" BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_created_by' AND table_name = 'users'
    ) THEN
        ALTER TABLE "users"
        ADD CONSTRAINT "fk_users_created_by"
        FOREIGN KEY ("createdByUserId") REFERENCES "users"("userId");
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_updated_by' AND table_name = 'users'
    ) THEN
        ALTER TABLE "users"
        ADD CONSTRAINT "fk_users_updated_by"
        FOREIGN KEY ("updatedByUserId") REFERENCES "users"("userId");
    END IF;
END $$;

ALTER TABLE "users"
ALTER COLUMN "userSaveSource" SET DEFAULT 'SELF_REGISTER';

ALTER TABLE "users"
ALTER COLUMN "createdAt" SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

ALTER TABLE "users"
ALTER COLUMN "updatedAt" SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

UPDATE "users"
SET
    "createdAt" = COALESCE("createdAt", (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT),
    "userSaveSource" = COALESCE("userSaveSource", 'SELF_REGISTER');


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
    "isInvited" BOOLEAN,
    "createdAt" BIGINT,
    "createdByUserId" INT,
    "userSaveSource" TEXT,
    "updatedByUserId" INT,
    "updatedAt" BIGINT
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
    "isInvited",
    "createdAt",
    "createdByUserId",
    "userSaveSource",
    "updatedByUserId",
    "updatedAt"
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
    "isInvited" BOOLEAN,
    "createdAt" BIGINT,
    "createdByUserId" INT,
    "userSaveSource" TEXT,
    "updatedByUserId" INT,
    "updatedAt" BIGINT
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
    "isInvited",
    "createdAt",
    "createdByUserId",
    "userSaveSource",
    "updatedByUserId",
    "updatedAt"
FROM users
WHERE "phoneNumber" = p_phone
LIMIT 1;
$$;