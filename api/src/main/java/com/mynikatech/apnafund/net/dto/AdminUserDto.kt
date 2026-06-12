package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDto(
    val userId: Int?,
    val firstName: String,
    val lastName: String?,
    val emailId: String?,
    val phoneNumber: String?,
    val isEmailVerified: Boolean?
) {

    val fullName: String
        get() = listOfNotNull(
            firstName.trim(),
            lastName?.trim()
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" ")
            .ifBlank { "Admin" }
}
