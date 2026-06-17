DROP FUNCTION IF EXISTS upsert_whatsapp_message(
    INT,TEXT,TEXT,TEXT,TEXT,TEXT,TEXT,TEXT
);
CREATE OR REPLACE FUNCTION upsert_whatsapp_message
(
    p_user_id INTEGER,
    p_event_type TEXT,
    p_template_name TEXT,
    p_phone_number TEXT,
    p_meta_message_id TEXT,
    p_wa_id TEXT,
    p_status TEXT,
    p_raw_response TEXT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS
$$
DECLARE
    v_id INTEGER;
BEGIN

    INSERT INTO whats_app_messages
    (
        "userId",
        "eventType",
        "templateName",
        "phoneNumber",
        "metaMessageId",
        "waId",
        "status",
        "rawResponse",
        "lastWebhookResponse"
    )
    VALUES
    (
        p_user_id,
        p_event_type,
        p_template_name,
        p_phone_number,
        p_meta_message_id,
        p_wa_id,
        p_status,
        p_raw_response,
        p_raw_response
    )
    ON CONFLICT ("metaMessageId")
    DO UPDATE
    SET
        "status" = EXCLUDED."status",
        "lastWebhookResponse" = EXCLUDED."lastWebhookResponse",
        "updatedDate" = NOW()

    RETURNING "whatsAppMessageId"
    INTO v_id;
    RETURN v_id;

END;
$$;