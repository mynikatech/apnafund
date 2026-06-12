package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserBasicDto(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val emailId: String?,
    val phoneNumber: String?
) {
    val fullName: String get() = "$firstName $lastName"
}
