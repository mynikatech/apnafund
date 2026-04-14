package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<ChatMessageRequest>,
    val temperature: Double = 0.0
)
