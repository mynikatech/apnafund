DROP FUNCTION IF EXISTS upsert_user_by_email( TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, BOOLEAN, TEXT, TEXT, TEXT, TEXT, BOOLEAN, INT);

CREATE OR REPLACE FUNCTION upsert_user_by_email(
  p_userId int,
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
LANGUAGE plpgsql
AS $$
DECLARE
  v_id int;
  v_now BIGINT := (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;
BEGIN

  -- UPDATE EXISTING USER
  IF COALESCE(p_userId, 0) > 0 THEN

    UPDATE users
    SET
      "firstName"       = p_firstName,
      "lastName"        = p_lastName,
      "emailId"         = NULLIF(TRIM(p_emailId), ''),
      "phoneNumber"     = NULLIF(TRIM(p_phoneNumber), ''),
      "status"          = p_status,
      "isPinSet"        = p_isPinSet,
      "hashPIN"         = p_hashPIN,
      "passwordHash"    = p_passwordHash,
      "firebaseUserId"  = p_firebaseUserId,
      "userCode"        = p_userCode,
      "isInvited"       = p_isInvited,
      "updatedByUserId" = p_actorUserId,
      "updatedAt"       = v_now
    WHERE "userId" = p_userId;

    RETURN p_userId;
  END IF;

  -- CREATE NEW USER
  INSERT INTO users(
    "firstName",
    "lastName",
    "emailId",
    "phoneNumber",
    "status",
    "createdDate",
    "isPinSet",
    "hashPIN",
    "passwordHash",
    "firebaseUserId",
    "userCode",
    "isInvited",
    "createdByUserId",
    "updatedByUserId",
    "updatedAt",
    "createdAt",
    "userSaveSource"
  )
  VALUES(
    p_firstName,
    p_lastName,
    NULLIF(TRIM(p_emailId), ''),
    NULLIF(TRIM(p_phoneNumber), ''),
    p_status,
    p_createdDate,
    p_isPinSet,
    p_hashPIN,
    p_passwordHash,
    p_firebaseUserId,
    p_userCode,
    p_isInvited,
    p_actorUserId,
    p_actorUserId,
    v_now,
    v_now,
    p_userSaveSource
  )
  RETURNING "userId"
  INTO v_id;

  RETURN v_id;

END;
$$;