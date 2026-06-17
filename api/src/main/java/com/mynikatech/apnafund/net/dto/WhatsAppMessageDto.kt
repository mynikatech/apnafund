package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppMessageDto(
    val whatsAppMessageId: Int? = null,
    val userId: Int?,
    val eventType: String?,
    val templateName: String?,
    val phoneNumber: String?,
    val metaMessageId: String?,
    val waId: String?,
    val status: String?,
    val rawResponse: String?,
    val lastWebhookResponse: String?,
    val createdDate: String? = null,
    val updatedDate: String? = null
)
