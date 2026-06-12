package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsersDto(
    val userId: Int? = null,
    val firstName: String,
    val lastName: String? = null,
    val emailId: String? = null,
    val phoneNumber: String? = null,
    val status: String = "ACTIVE",
    val passwordHash: String? = null,
    val createdDate: String,
    val isPinSet: Boolean = false,
    val hashPIN: String? = null,
    val firebaseUserId: String? = null,
    val emailVerified: Boolean = false,
    val emailVerifiedAt: String? = null,
    val phoneVerified: Boolean = false,
    val phoneVerifiedAt: String? = null,
    val userCode: String,
    val isInvited: Boolean = false,
    val createdByUserId: Int?,
    val userSaveSource: UserSaveSource?,
    val createdAt: Long,
    val updatedByUserId: Int?,
    val updatedAt: Long?,
    val createdByName: String? = null
) {

    val fullName: String
        get() = listOfNotNull(lastName, firstName)
            .joinToString(", ")
}