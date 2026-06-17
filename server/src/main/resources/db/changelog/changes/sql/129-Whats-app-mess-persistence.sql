DROP FUNCTION IF EXISTS get_whatsapp_message_by_meta_id(
    TEXT
);
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
    FROM whats_app_messages w
    WHERE w."metaMessageId" = p_meta_message_id;

END;
$$;