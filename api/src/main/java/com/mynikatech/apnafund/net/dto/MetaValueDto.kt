package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetaValueDto(
    val statuses: List<MetaStatusDto> = emptyList()
)
