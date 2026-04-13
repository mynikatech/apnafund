package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIRequest(
    val message: String,
    val context: AIContext
)
