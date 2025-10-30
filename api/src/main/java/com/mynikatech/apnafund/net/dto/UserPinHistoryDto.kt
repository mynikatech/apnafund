package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserPinHistoryDto(
    val id: Int? = null,
    val userId: Int,
    val pinHash: String,
    val changedAt: Long? = null
)