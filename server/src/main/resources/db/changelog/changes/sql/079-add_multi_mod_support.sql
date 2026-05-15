DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'memberRole'
    ) THEN
        CREATE TYPE "memberRole" AS ENUM (
            'PRIMARY_MODERATOR',
            'MODERATOR',
            'MEMBER'
        );
    END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'memberStatus'
    ) THEN
        CREATE TYPE "memberStatus" AS ENUM (
            'ACTIVE',
            'INACTIVE'
        );
    END IF;
END$$;

ALTER TABLE "group_members"
ADD COLUMN IF NOT EXISTS "role" "memberRole" DEFAULT 'MEMBER',
ADD COLUMN IF NOT EXISTS "status" "memberStatus" DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS "updatedAt" BIGINT,
ADD COLUMN IF NOT EXISTS "updatedBy" INT;

ALTER TABLE "fund_members"
ADD COLUMN IF NOT EXISTS "role" "memberRole" DEFAULT 'MEMBER',
ADD COLUMN IF NOT EXISTS "status" "memberStatus" DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS "updatedAt" BIGINT,
ADD COLUMN IF NOT EXISTS "updatedBy" INT;

UPDATE "fund_members" fm
SET "role" = 'PRIMARY_MODERATOR'
WHERE fm."role" IS NULL
AND fm."userId" IN (
    SELECT f."moderator" FROM "funds" f WHERE f."fundId" = fm."fundId"
);

UPDATE "group_members" gm
SET "role" = 'PRIMARY_MODERATOR'
WHERE gm."role" IS NULL
AND gm."userId" IN (
    SELECT g."moderator" FROM "groups" g WHERE g."groupId" = gm."groupId"
);