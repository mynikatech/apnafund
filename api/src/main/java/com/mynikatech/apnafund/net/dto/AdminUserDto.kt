package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminUserDto(
    val userId: Int?,
    val firstName: String,
    val lastName: String?,
    val emailId: String
){
    val fullName: String
        get() = listOfNotNull(lastName, firstName)
            .joinToString(", ")
}