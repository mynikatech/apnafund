package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberWithNameDto(
    val groupMemberId: Int,
    val userId: Int,
    val groupId: Int,
    val joiningDate: String,
    val firstName: String,
    val lastName: String,
    val emailId: String?,
    val phoneNumber: String?,
    val role: String,
    val status: String,
    val updatedAt: Long? = null,
    val updatedBy: Int? = null
) {
    val fullName: String get() = "$firstName $lastName"
}