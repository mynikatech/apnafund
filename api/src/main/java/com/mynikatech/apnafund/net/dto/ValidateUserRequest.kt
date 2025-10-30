package com.mynikatech.apnafund.net.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidateUserRequest(
    val emailId: String,
    val passwordHash: String
)