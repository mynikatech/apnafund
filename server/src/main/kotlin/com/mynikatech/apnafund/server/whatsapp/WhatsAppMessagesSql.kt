package com.mynikatech.apnafund.server.whatsapp

import com.mynikatech.apnafund.net.dto.WhatsAppMessageDto
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery

@RegisterKotlinMapper(WhatsAppMessageDto::class)
interface WhatsAppMessagesSql {

    @SqlQuery(
        """
        SELECT add_whatsapp_message(
            :userId,
            :eventType,
            :templateName,
            :phoneNumber,
            :metaMessageId,
            :waId,
            :status,
            :rawResponse
        )
        """
    )
    fun addWhatsAppMessage(
        @Bind("userId")
        userId: Int?,

        @Bind("eventType")
        eventType: String?,

        @Bind("templateName")
        templateName: String?,

        @Bind("phoneNumber")
        phoneNumber: String?,

        @Bind("metaMessageId")
        metaMessageId: String?,

        @Bind("waId")
        waId: String?,

        @Bind("status")
        status: String?,

        @Bind("rawResponse")
        rawResponse: String?
    ): Int

    @SqlQuery(
        """
        SELECT *
        FROM get_whatsapp_message_by_meta_id(
            :metaMessageId
        )
        """
    )
    fun getWhatsAppMessageByMetaId(
        @Bind("metaMessageId")
        metaMessageId: String
    ): WhatsAppMessageDto?

    @SqlQuery(
        """
        SELECT update_whatsapp_message_status(
            :metaMessageId,
            :status,
            :lastWebhookResponse
        )
        """
    )
    fun updateWhatsAppMessageStatus(
        @Bind("metaMessageId")
        metaMessageId: String,

        @Bind("status")
        status: String,

        @Bind("lastWebhookResponse")
        lastWebhookResponse: String?
    ): Int

    @SqlQuery(
        """
    SELECT upsert_whatsapp_message(
        :userId,
        :eventType,
        :templateName,
        :phoneNumber,
        :metaMessageId,
        :waId,
        :status,
        :rawResponse
    )
    """
    )
    fun upsertWhatsAppMessage(
        @Bind("userId")
        userId: Int?,

        @Bind("eventType")
        eventType: String?,

        @Bind("templateName")
        templateName: String?,

        @Bind("phoneNumber")
        phoneNumber: String?,

        @Bind("metaMessageId")
        metaMessageId: String?,

        @Bind("waId")
        waId: String?,

        @Bind("status")
        status: String?,

        @Bind("rawResponse")
        rawResponse: String?
    ): Int
}