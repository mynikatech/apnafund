DROP FUNCTION get_user_notifications(integer);

CREATE OR REPLACE FUNCTION add_user_notification(
  p_notification_type text,
  p_user_id           int,
  p_message           text,
  p_is_expired        boolean,
  p_published         boolean,
  p_read              boolean,
  p_status            text
) RETURNS int LANGUAGE plpgsql AS $$
DECLARE v_id int;
BEGIN
  INSERT INTO user_notifications(
    "notificationType","userId","message",
    "isExpiredFlag","publishedFlag","readFlag","status", "createdAt"
  ) VALUES (
    p_notification_type, p_user_id, p_message,
    COALESCE(p_is_expired,false),
    COALESCE(p_published,false),
    COALESCE(p_read,false),
    COALESCE(p_status,'ACTIVE'), NOW()
  )
  RETURNING "userNotificationId" INTO v_id;

  RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION get_unread_notification_count(
    p_user_id INT
)
RETURNS INT
LANGUAGE sql
AS $$
    SELECT COUNT(*)
    FROM user_notifications
    WHERE "userId" = p_user_id
      AND "readFlag" = false
      AND "status" = 'ACTIVE'
      AND "publishedFlag" = true;
$$;

CREATE OR REPLACE FUNCTION get_user_notifications(p_user_id int)
RETURNS TABLE (
  "userNotificationId" int,
  "notificationType"   text,
  "userId"             int,
  "message"            text,
  "isExpiredFlag"      boolean,
  "publishedFlag"      boolean,
  "readFlag"           boolean,
  "status"             text,
  "createdAt"          timestamptz,
  "readAt"             timestamptz,
  "expiredAt"          timestamptz
)
LANGUAGE sql
AS $$
  SELECT
      n."userNotificationId",
      n."notificationType",
      n."userId",
      n."message",
      n."isExpiredFlag",
      n."publishedFlag",
      n."readFlag",
      n."status",
      n."createdAt",
      n."readAt",
      n."expiredAt"
  FROM user_notifications n
  WHERE n."userId" = p_user_id
    AND n."status" = 'ACTIVE'
    AND n."publishedFlag" = true
  ORDER BY n."createdAt" DESC;
$$;