package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserFundMembership(
    val fundId: Int,
    val role: String,
    val status: String
)
