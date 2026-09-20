DROP FUNCTION IF EXISTS add_whatsapp_message(
    INTEGER,
    TEXT,
    TEXT,
    TEXT,
    TEXT,
    TEXT,
    TEXT,
    TEXT
);

DROP FUNCTION IF EXISTS get_whatsapp_message_by_meta_id(
    TEXT
);

DROP FUNCTION IF EXISTS update_whatsapp_message_status(
    TEXT,
    TEXT,
    TEXT
);

CREATE TABLE IF NOT EXISTS whats_app_messages
(
        "whatsAppMessageId" SERIAL PRIMARY KEY,

       "userId" INT,

       "eventType" TEXT,

       "templateName" TEXT,

       "phoneNumber" TEXT,

       "metaMessageId" TEXT,

       "waId" TEXT,

       "status" TEXT,

       "rawResponse" TEXT,

       "lastWebhookResponse" TEXT,

       "createdDate" TIMESTAMP NOT NULL DEFAULT NOW(),

       "updatedDate" TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_whatsapp_messages_meta_message_id
ON whats_app_messages("metaMessageId");

CREATE OR REPLACE FUNCTION add_whatsapp_message
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
        "rawResponse"
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
        p_raw_response
    )
    RETURNING "whatsAppMessageId"
    INTO v_id;

    RETURN v_id;

END;
$$;

CREATE OR REPLACE FUNCTION get_whatsapp_message_by_meta_id
(
    p_meta_message_id TEXT
)
RETURNS TABLE
(
    "whatsAppMessageId" INTEGER,
    "userId" INTEGER,
    "eventType" TEXT,
    "templateName" TEXT,
    "phoneNumber" TEXT,
    "metaMessageId" TEXT,
    "waId" TEXT,
    "status" TEXT,
    "rawResponse" TEXT,
    "lastWebhookResponse" TEXT,
    "createdDate" TIMESTAMP,
    "updatedDate" TIMESTAMP
)
LANGUAGE plpgsql
AS
$$
BEGIN

    RETURN QUERY

    SELECT *
    FROM whats_app_messages
    WHERE "metaMessageId" = p_meta_message_id;

END;
$$;

CREATE OR REPLACE FUNCTION update_whatsapp_message_status
(
    p_meta_message_id TEXT,
    p_status TEXT,
    p_last_response TEXT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS
$$
DECLARE
    v_count INTEGER;
BEGIN

    UPDATE whats_app_messages
    SET
        "status" = p_status,
        "updatedDate" = NOW(),
        "lastWebhookResponse" = p_last_response
    WHERE "metaMessageId" = p_meta_message_id;

    GET DIAGNOSTICS v_count = ROW_COUNT;

    RETURN v_count;

END;
$$;