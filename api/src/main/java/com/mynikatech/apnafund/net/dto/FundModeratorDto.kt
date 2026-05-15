package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable

data class FundModeratorDto(

    val userId: Int,

    val firstName: String?,

    val lastName: String?,

    val fullName: String?,

    val emailId: String?,

    val phoneNumber: String?,

    val role: String,

    val status: String,

    val isEmailVerified: Boolean?
) {

    fun fullName(): String {

        return listOfNotNull(
            firstName?.trim(),
            lastName?.trim()
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" ")
            .ifBlank { "User" }
    }
}