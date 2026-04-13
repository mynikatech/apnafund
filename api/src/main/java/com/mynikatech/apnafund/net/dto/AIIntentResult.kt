package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIIntentResult(
    val intent: AIIntent,
    val entity: AIEntity? = null,
    val filters: Map<String, String> = emptyMap()
)