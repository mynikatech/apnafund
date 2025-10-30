package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsersDto(
    val userId: Int? = null,
    val firstName: String,
    val lastName: String? = null,
    val emailId: String,
    val phoneNumber: String? = null,
    val status: String = "ACTIVE",
    val passwordHash: String? = null,
    val createdDate: String,
    val isPinSet: Boolean = false,
    val hashPIN: String? = null,
    val firebaseUserId: String? = null,
    val userCode: String
)