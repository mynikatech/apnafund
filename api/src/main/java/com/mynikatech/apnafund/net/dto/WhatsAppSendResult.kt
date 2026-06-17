package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppSendResult(
    val metaMessageId: String?,
    val waId: String?,
    val rawResponse: String?
)
