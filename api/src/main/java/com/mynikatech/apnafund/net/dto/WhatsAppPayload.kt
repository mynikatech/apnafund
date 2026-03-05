package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppPayload(
    val phone: String,
    val template: String? = null
)