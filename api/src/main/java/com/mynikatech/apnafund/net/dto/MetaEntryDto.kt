package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetaEntryDto(
    val changes: List<MetaChangeDto> = emptyList()
)
