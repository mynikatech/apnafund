package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AIChatMessage(
    val message: String,
    val isUser: Boolean,
    val type: AIResponseType = AIResponseType.TEXT,
    val data: JsonElement? = null,
    val actions: List<AIActionItem> = emptyList()
)