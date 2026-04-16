package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AIIntentResult(
    val intent: AIIntent,
    val entity: AIEntity? = null,
    val action: AIAction,
    val filters: JsonObject = JsonObject(emptyMap())
)