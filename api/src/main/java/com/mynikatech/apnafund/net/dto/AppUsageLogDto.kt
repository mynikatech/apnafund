package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageLogDto(

    val userId: Int,

    val eventType: String,

    val screenName: String? = null,

    val details: String? = null,

    val deviceInfo: String? = null
)
