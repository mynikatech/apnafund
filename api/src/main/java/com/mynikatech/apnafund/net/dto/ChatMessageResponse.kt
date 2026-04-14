package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageResponse(
    val role: String,
    val content: String
)
