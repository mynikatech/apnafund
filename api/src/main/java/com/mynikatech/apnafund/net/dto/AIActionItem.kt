package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AIActionItem(
    val label: String,
    val type: UIActionType,   // e.g. OPEN_SCREEN, APPLY_LOAN
    val payload: JsonObject? = null
)
