CREATE OR REPLACE FUNCTION mark_notification_read(
    p_id INT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_count INT;
BEGIN
    UPDATE user_notifications
    SET "readFlag" = true,
        "readAt" = NOW()
    WHERE "userNotificationId" = p_id;

    GET DIAGNOSTICS v_count = ROW_COUNT;

    RETURN v_count > 0;
END;
$$;