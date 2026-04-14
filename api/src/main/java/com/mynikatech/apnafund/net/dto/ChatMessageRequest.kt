package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageRequest(
    val role: String,
    val content: String
)
