package com.mynikatech.apnafund.lambda.whatsapp

import com.mynikatech.apnafund.net.dto.NotificationEvent

object WhatsAppRenderer {

    fun render(event: NotificationEvent): String {
        val content = UserWhatsAppContentRegistry.get(event)
        return content.message(event)
    }
}