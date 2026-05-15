package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailableFundMemberDto(
    val userId: Int,
    val firstName: String,
    val lastName: String?,
    val emailId: String?,
    val phoneNumber: String?
)
