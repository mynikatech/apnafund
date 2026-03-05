package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmailPayload(
    val to: String,
    val userName: String,
    val data: Map<String, String> = emptyMap()
)
