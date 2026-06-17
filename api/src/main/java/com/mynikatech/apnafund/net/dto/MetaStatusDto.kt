package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetaStatusDto(
    val id: String,
    val status: String,
    val recipient_id: String? = null,
    val recipient_user_id: String? = null,
    val timestamp: String? = null
)
