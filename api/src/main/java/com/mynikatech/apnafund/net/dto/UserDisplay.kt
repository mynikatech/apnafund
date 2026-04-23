package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDisplay(
    val userId: Int,
    val displayName: String
)