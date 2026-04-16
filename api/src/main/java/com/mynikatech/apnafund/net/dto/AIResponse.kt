package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AIResponse(
    val reply: String,
    val type: AIResponseType,
    val data: JsonElement? = null,
    val actions: List<AIActionItem> = emptyList()
)
