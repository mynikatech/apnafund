package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AIAction(
    val label: String,
    val action: String
)
