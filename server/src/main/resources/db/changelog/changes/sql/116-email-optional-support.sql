DROP FUNCTION IF EXISTS upsert_user_by_email( TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, BOOLEAN, TEXT, TEXT, TEXT, TEXT, BOOLEAN, INT);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'emailId'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE users
        ALTER COLUMN "emailId" DROP NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'phoneNumber'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE users
        ALTER COLUMN "phoneNumber" SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_namespace n
          ON n.oid = c.connamespace
        WHERE c.conname = 'users_phone_number_unique'
          AND n.nspname = current_schema()
    ) THEN
        ALTER TABLE users
        ADD CONSTRAINT users_phone_number_unique
        UNIQUE ("phoneNumber");
    END IF;
END $$;


CREATE OR REPLACE FUNCTION upsert_user_by_email(
  p_firstName text,
  p_lastName text,
  p_emailId text,
  p_phoneNumber text,
  p_status text,
  p_createdDate text,
  p_isPinSet boolean,
  p_hashPIN text,
  p_passwordHash text,
  p_firebaseUserId text,
  p_userCode text,
  p_isInvited boolean,
  p_actorUserId int,
  p_userSaveSource text
)
RETURNS int
LANGUAGE plpgsql AS $$
DECLARE
  v_id int;
  v_now BIGINT := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
BEGIN

  INSERT INTO users(
    "firstName","lastName","emailId","phoneNumber","status","createdDate",
    "isPinSet","hashPIN","passwordHash","firebaseUserId","userCode",
    "isInvited","createdByUserId","updatedByUserId","updatedAt", "createdAt", "userSaveSource"
  )
  VALUES(
    p_firstName,p_lastName,NULLIF(TRIM(p_emailId), ''),p_phoneNumber,p_status,p_createdDate,
    p_isPinSet,p_hashPIN,p_passwordHash,p_firebaseUserId,p_userCode,
    p_isInvited,p_actorUserId,p_actorUserId, v_now, v_now, p_userSaveSource
  )

  ON CONFLICT ("phoneNumber") DO UPDATE
    SET "firstName"      = COALESCE(EXCLUDED."firstName",      users."firstName"),
        "lastName"       = COALESCE(EXCLUDED."lastName",       users."lastName"),
        "emailId"        = COALESCE(EXCLUDED."emailId",        users."emailId"),
        "status"         = COALESCE(EXCLUDED."status",         users."status"),
        "isPinSet"       = COALESCE(EXCLUDED."isPinSet",       users."isPinSet"),
        "hashPIN"        = COALESCE(EXCLUDED."hashPIN",        users."hashPIN"),
        "passwordHash"   = COALESCE(EXCLUDED."passwordHash",   users."passwordHash"),
        "firebaseUserId" = COALESCE(EXCLUDED."firebaseUserId", users."firebaseUserId"),
        "userCode"       = COALESCE(EXCLUDED."userCode",       users."userCode"),
        "isInvited"      = COALESCE(EXCLUDED."isInvited",      users."isInvited"),
        "updatedByUserId" = p_actorUserId,
        "updatedAt"     = v_now,
        "userSaveSource" = users."userSaveSource";

  SELECT u."userId" INTO v_id
  FROM users u
  WHERE u."phoneNumber" = p_phoneNumber
  LIMIT 1;

  RETURN v_id;
END $$;