package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class WhatsAppContact(
    val input: String? = null,
    val wa_id: String? = null
)
