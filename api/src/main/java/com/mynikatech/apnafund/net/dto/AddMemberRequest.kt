package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddMemberRequest(
    val userId: Int,
    val joiningDate: String,
    val role: String,
    val requestorId: Int
)