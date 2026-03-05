CREATE OR REPLACE FUNCTION mark_notification_read(
    p_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE user_notifications
    SET "readFlag" = true,
        "readAt" = NOW()
    WHERE "userNotificationId" = p_id;

    IF FOUND THEN
        RETURN TRUE;
    ELSE
        RETURN FALSE;
    END IF;
END;
$$;