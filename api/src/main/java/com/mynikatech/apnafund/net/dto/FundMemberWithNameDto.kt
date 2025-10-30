package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundMemberWithNameDto(
    val fundMemberId: Int,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String
) {
    val fullName: String get() = "$firstName $lastName"
}