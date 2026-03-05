ALTER TABLE user_notifications
ADD COLUMN IF NOT EXISTS "createdAt" TIMESTAMPTZ DEFAULT NOW(),
ADD COLUMN IF NOT EXISTS "readAt" TIMESTAMPTZ NULL,
ADD COLUMN IF NOT EXISTS "expiredAt" TIMESTAMPTZ NULL;

CREATE OR REPLACE FUNCTION mark_notification_read(
    p_id INT
)
RETURNS BOOLEAN
LANGUAGE sql
AS $$
    UPDATE user_notifications
    SET "readFlag" = true,
            "readAt" = NOW()
    WHERE "userNotificationId" = p_id;

    SELECT true;
$$;

CREATE INDEX IF NOT EXISTS idx_user_notifications_unread
ON user_notifications("userId", "readFlag")
WHERE "readFlag" = false;

CREATE OR REPLACE FUNCTION mark_notification_read(
    p_id INT
)
RETURNS BOOLEAN
LANGUAGE sql
AS $$
    UPDATE user_notifications
    SET "readFlag" = true,
        "readAt" = NOW()
    WHERE "userNotificationId" = p_id;

    SELECT true;
$$;