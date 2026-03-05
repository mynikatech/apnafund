package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationEvent(
    val eventType: String,
    val userId: String,
    val channels: Set<Channel>,
    val email: EmailPayload? = null,
    val whatsapp: WhatsAppPayload? = null,
    val eventData: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)
