package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppSendResponse(
    val messaging_product: String? = null,
    val contacts: List<WhatsAppContact> = emptyList(),
    val messages: List<WhatsAppMessage> = emptyList()
)
