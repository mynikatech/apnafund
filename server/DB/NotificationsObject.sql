-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_user_notifications_user ON user_notifications("userId");
CREATE INDEX IF NOT EXISTS idx_user_notifications_status ON user_notifications("status");

-- READ
CREATE OR REPLACE FUNCTION get_user_notifications(p_user_id int)
RETURNS TABLE (
  "userNotificationId" int,
  "notificationType"   text,
  "userId"             int,
  "message"            text,
  "isExpiredFlag"      int,
  "publishedFlag"      int,
  "readFlag"           int,
  "status"             text
) LANGUAGE sql AS $$
  SELECT n."userNotificationId", n."notificationType", n."userId", n."message",
         n."isExpiredFlag", n."publishedFlag", n."readFlag", n."status"
    FROM user_notifications n
   WHERE n."userId" = p_user_id
   ORDER BY n."userNotificationId" DESC;
$$;

-- CREATE
CREATE OR REPLACE FUNCTION add_user_notification(
  p_notification_type text,
  p_user_id           int,
  p_message           text,
  p_is_expired        int,
  p_published         int,
  p_read              int,
  p_status            text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_notifications(
    "notificationType","userId","message",
    "isExpiredFlag","publishedFlag","readFlag","status"
  ) VALUES (
    p_notification_type, p_user_id, p_message,
    COALESCE(p_is_expired,false),
    COALESCE(p_published,false),
    COALESCE(p_read,false),
    COALESCE(p_status,'ACTIVE')
  )
  RETURNING "userNotificationId" INTO v_id;

  RETURN v_id;
END $$;

-- UPDATE
CREATE OR REPLACE FUNCTION update_user_notification(
  p_id               int,
  p_notification_type text,
  p_user_id           int,
  p_message           text,
  p_is_expired        int,
  p_published         int,
  p_read              int,
  p_status            text
) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  UPDATE user_notifications
     SET "notificationType" = COALESCE(p_notification_type, "notificationType"),
         "userId"           = COALESCE(p_user_id, "userId"),
         "message"          = COALESCE(p_message, "message"),
         "isExpiredFlag"    = COALESCE(p_is_expired, "isExpiredFlag"),
         "publishedFlag"    = COALESCE(p_published, "publishedFlag"),
         "readFlag"         = COALESCE(p_read, "readFlag"),
         "status"           = COALESCE(p_status, "status")
   WHERE "userNotificationId" = p_id;
  RETURN FOUND;
END $$;

-- DELETE
CREATE OR REPLACE FUNCTION delete_user_notification(p_id int)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
  DELETE FROM user_notifications WHERE "userNotificationId" = p_id;
  RETURN FOUND;
END $$;