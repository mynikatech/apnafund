package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundMemberWithNameDto(
    val fundMemberId: Int,
    val userId: Int,
    val fundId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String,
    val emailId: String,
    val role: String = "MEMBER",
    val status: String = "ACTIVE",
    val updatedAt: Long? = null,
    val updatedBy: Int? = null
) {
    val fullName: String get() = "$firstName $lastName"
}