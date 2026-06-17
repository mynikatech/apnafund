package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetaWebhookDto(
    val `object`: String? = null,
    val entry: List<MetaEntryDto> = emptyList()
)
