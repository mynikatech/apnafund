package com.mynikatech.apnafund.server.common.messaging

import com.mynikatech.apnafund.net.dto.Channel
import com.mynikatech.apnafund.net.dto.EmailPayload
import com.mynikatech.apnafund.net.dto.WhatsAppPayload

object NotificationHelper {

    fun buildChannels(
        email: String?,
        phone: String?
    ): Set<Channel> {
        val channels = mutableSetOf<Channel>()

        if (!email.isNullOrBlank()) {
            channels.add(Channel.EMAIL)
        }

        if (!phone.isNullOrBlank()) {
            channels.add(Channel.WHATSAPP)
        }

        return channels
    }

    fun buildEmailPayload(
        email: String?,
        userName: String,
        data: Map<String, String> = emptyMap()
    ): EmailPayload? =
        email?.let {
            EmailPayload(
                to = it,
                userName = userName,
                data = data
            )
        }

    fun buildWhatsAppPayload(
        phone: String?,
        template: String,
        templateParams: List<String> = emptyList()
    ): WhatsAppPayload? =
        phone?.let {
            WhatsAppPayload(
                phone = it,
                template = template,
                templateParams = templateParams
            )
        }
}